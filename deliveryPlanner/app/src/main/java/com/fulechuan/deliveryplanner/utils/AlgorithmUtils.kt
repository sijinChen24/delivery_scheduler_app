package com.example.deliveryplanner.utils

import com.amap.api.maps.AMapUtils
import com.amap.api.maps.model.LatLng
import com.fulechuan.deliveryplanner.enums.TaskType
import com.fulechuan.deliveryplanner.model.data.Order
import com.fulechuan.deliveryplanner.model.data.OrderNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AlgorithmUtils {

    // todo 骑行平均速度 (30km/h ≈ 500米/分) 目前是写死的速度,后期考虑从高德api获取速度
    private const val SPEED_M_PER_MIN = 500.0

    //todo 每次取货的预估停留时间 (分钟)  取货点要考虑的因素:1.商家出餐时间;2.寻找时间(如遇到商场里的店家)
    private const val PICK_STOP_DURATION_MIN = 5

    //todo 每次送货的预估停留时间 (分钟)  送货点要考虑的因素:1.是否步行进小区;2.单元楼寻找;3.是否有电梯
    private const val DELIVERY_STOP_DURATION_MIN = 10
    // todo 后期提供一个反馈系统,根据骑手的实际反馈,动态更新预估停留时间

    /**
     * 用于在接单前判断：如果加入这个新订单，是否会导致超时？
     * @param currentNodeTask 当前已接的任务列表
     * @param newOrderList 扫描到的新订单列表
     * @param point 骑手当前经纬度
     * @return Pair<Boolean, String>: (是否可行, 原因/提示)
     */
     fun simulateIsFeasible(
        currentNodeTask: List<OrderNode>,
        newOrderList: List<Order>,
        currentLocation: Pair<Double, Double>
    ): Pair<List<OrderNode>, List<Order>> {

        // 1. 预处理：按截止时间排序 (越急的越先尝试插入)
        // 也可以按距离排序，看你的策略，通常时间约束更紧
        val sortedCandidates = newOrderList.sortedBy { it.pickupDeadline }

        // 2. 初始化“滚雪球”变量
        var runningRoute = currentNodeTask
        val acceptedOrders = mutableListOf<Order>()

        //3. 判断是否需要锁定当前任务节点
        val firstNode = currentNodeTask[0]
        val isNeedLock = isNeedLock(firstNode,currentLocation)

        for (order in sortedCandidates){

            // 调用之前写好的“单单插入”逻辑
            // 注意：这里的入参 runningRoute 是上一轮更新过的
            val newRouteCandidate = insertIntoExistingRoute(
                currentRoute = runningRoute,
                newOrder = order,
                currentLocation=currentLocation,
                isNeedLock =isNeedLock
            )

            // 4. 检查是否导致超时 (Valid Check)
            // 之前的 insertIntoExistingRoute 如果超时会标记 isOvertime=true
            // 我们只需要检查新生成的路线里有没有红节点
            if (isRouteSafe(newRouteCandidate)) {
                // ✅ 成功：更新路线，接纳该订单
                runningRoute = newRouteCandidate
                acceptedOrders.add(order)
            } else {
                // ❌ 失败：这单插进去会导致超时，放弃这单，保持 runningRoute 不变
                // 继续尝试下一个 candidate
                // (Log: 订单 ${order.shopName} 插入失败，超时)
            }
        }
        return runningRoute to acceptedOrders
    }

    /**
     * 辅助检查：路线是否安全（无超时节点）
     */
    private fun isRouteSafe(route: List<OrderNode>): Boolean {
        // 只要有一个节点超时，整个方案就不安全
        return route.none { it.isOvertime }
    }

    /**
     * 🗺️ 规划方法 (PlanRoute)
     * 用于接单后，正式生成 UI 显示的路径列表
     * @return List<OrderNode>: 排好序的节点列表
     */

    suspend fun planRoute(
        currentNodeTask: List<OrderNode>,
        newOrder: Order,
        currentLocation: Pair<Double, Double>
    ): List<OrderNode> = withContext(Dispatchers.Default) {
        // 判断是否需要锁定当前任务节点
        val firstNode = currentNodeTask[0]
        val isNeedLock = isNeedLock(firstNode,currentLocation)
        return@withContext insertIntoExistingRoute(currentNodeTask, newOrder,currentLocation, isNeedLock)
    }

    /**
     * 🆕 冷启动/恢复现场专用：从零开始规划
     *
     * @param orders 从数据库读出来的所有订单
     * @param riderLat 骑手位置
     * @param riderLng 骑手位置
     */
    fun planRouteFromScratch(
        orders: List<Order>,
        currentLocation: Pair<Double, Double>,
    ): List<OrderNode> {

        if (orders.isEmpty()) return emptyList()

        // 1. 预处理：按截止时间排序 (越急的越先规划)
        // 这样可以保证最重要的单子先占据最佳位置
        val sortedOrders = orders.sortedBy { it.pickupDeadline }

        // 2. 初始为空路线
        var currentRoute = emptyList<OrderNode>()

        // 3. 循环插入 (复用增量逻辑)
        for (order in sortedOrders) {
            currentRoute = insertIntoExistingRoute(
                currentRoute = currentRoute,
                newOrder = order,
                currentLocation,
                isNeedLock = false       // ⚡️ 关键：恢复模式下，允许插队到最前面！
            )
        }

        return currentRoute
    }


    /**
     * 计算路径的成本和合法性
     * @return Pair(Boolean, Double): (是否所有节点都不超时, 总耗时分钟)
     */
    private fun calculateRouteCost(
        route: List<OrderNode>,
        currentLocation: Pair<Double, Double>
    ): Pair<Boolean, Double> {
        var currentTime = System.currentTimeMillis()
        var currentLat = currentLocation.first
        var currentLng = currentLocation.second
        var totalCost = 0.0

        for (node in route) {
            // 1. 计算路程耗时
            val dist = getDistance(currentLat, currentLng, node.location.x, node.location.y)

            // 耗时 = 路程 + 停车
            val stopTime =
                if (node.type == TaskType.PICKUP) PICK_STOP_DURATION_MIN else DELIVERY_STOP_DURATION_MIN
            val timeConsumed = (dist / SPEED_M_PER_MIN) + stopTime // 分钟

            //更新到达时间
            currentTime += (timeConsumed * 60 * 1000).toLong()
            totalCost += timeConsumed

            // 更新节点显示的预计到达时间 (重要：因为这直接更新了内存中的 Node 对象)
            node.estimatedArrival = currentTime

            // 检查是否超时
            if (node.deadline > 0 && node.estimatedArrival > node.deadline) {
                return false to Double.MAX_VALUE // 只要有一个节点超时，这方案就废了
            } else {
                node.isOvertime = false
            }

            // 更新当前位置为该节点位置，用于计算下一段
            currentLat = node.location.x
            currentLng = node.location.y
        }

        return Pair(true, totalCost)
    }

    // ================= 辅助工具 =================

    private fun createNode(order: Order, type: TaskType): OrderNode {
        return OrderNode(
            orderId = order.id,
            name = if (type == TaskType.PICKUP) order.shopName else order.customName,
            address = if (type == TaskType.PICKUP) order.pickupAddress else order.deliveryAddress,
            location = if (type == TaskType.PICKUP) order.pickupLoc else order.deliveryLoc,
            type = type,
            deadline = if (type == TaskType.PICKUP) order.pickupDeadline else order.deliveryDeadline
        )
    }

    /**
     * 计算两点间直线距离 (高德SDK)
     * 返回单位: 米
     */
    private fun getDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {

        return AMapUtils.calculateLineDistance(LatLng(lat1, lon1), LatLng(lat2, lon2))
    }


    /**
     * 判断是否需要锁定当前任务(防止路线抖动带来的干扰)
     * 逻辑:如果当前位置到当前任务的距离<500m就锁定
     */

    fun isNeedLock(firstNode: OrderNode, location: Pair<Double, Double>): Boolean {
        var shouldLock = false
        // 计算离当前目标的距离

        val distToTarget =
            getDistance(firstNode.location.x, firstNode.location.y, location.first, location.second)
        // 策略：如果离目标小于 500米，就锁定
        shouldLock = if (distToTarget < 500) true else false
        return shouldLock
    }


    /**
     * 🚀 增量贪心算法：直接基于现有的节点列表进行插入
     *
     * @param currentRoute 当前内存中的路线 (List<OrderNode>)
     * @param newOrder     新抢到的订单 (Order)
     * @param currentLocation     骑手当前位置
     * @param isNeedLock     是否需要锁定
     */
    fun insertIntoExistingRoute(
        currentRoute: List<OrderNode>,
        newOrder: Order,
        currentLocation: Pair<Double, Double>,
        isNeedLock: Boolean
    ): List<OrderNode> {

        // 1. 如果当前没任务，直接返回空
        if (currentRoute.isEmpty()) {
            return emptyList()
        }

        // 2. 准备变量
        var bestRoute: List<OrderNode>? = null
        var minCost = Double.MAX_VALUE

        // 3. 开始双层循环插入
        // i 是 Pickup 插入的位置
        // 🔒 锁定策略：i 从 1 开始，意味着绝不插队到当前正在进行的第一个任务前面
        // 如果你想允许插队到第一个，改成 i in 0..currentRoute.size
        val index = if (isNeedLock) 1 else 0
        for (i in index..currentRoute.size) {

            // j 是 Delivery 插入的位置 (必须在 i 之后)
            for (j in i + 1..currentRoute.size + 1) {

                // 3.1 复制现有路线 (基于 Node 列表复制，速度很快)
                val tempRoute = ArrayList(currentRoute)

                // 3.2 创建新订单的两个节点
                val pNode = createNode(newOrder, TaskType.PICKUP)
                val dNode = createNode(newOrder, TaskType.DELIVERY)

                // 3.3 插入 (先插后面的 D，再插前面的 P，防止索引偏移)
                if (j >= tempRoute.size) tempRoute.add(dNode) else tempRoute.add(j, dNode)
                if (i >= tempRoute.size) tempRoute.add(pNode) else tempRoute.add(i, pNode)

                // 3.4 算账 (计算耗时 + 校验超时)
                // 起点：如果锁定了第一个节点，起点应该是 currentRoute[0] 的坐标？
                // 不，为了计算准确，起点依然建议用【骑手当前实时坐标】。
                // 因为虽然锁定了第一个任务，但骑手可能离第一个任务还有段距离，这段时间也要算进去。
                val (isFeasible, cost) = calculateRouteCost(tempRoute, currentLocation)

                if (isFeasible && cost < minCost) {
                    minCost = cost
                    bestRoute = tempRoute
                }
            }
        }

        // 4. 返回结果
        return if (bestRoute != null) {
            bestRoute
        } else {
            // 兜底：插不进去（超时），放到最后
            val fallbackRoute = currentRoute.toMutableList()
            fallbackRoute.add(createNode(newOrder, TaskType.PICKUP).apply { isOvertime = true })
            fallbackRoute.add(createNode(newOrder, TaskType.DELIVERY).apply { isOvertime = true })
            calculateRouteCost(fallbackRoute, currentLocation) // 重算一下时间供显示
            fallbackRoute
        }
    }

    /**
     * ⏱️ 刷新时间轴 (Refresh Timeline)
     * 用于在节点增删后，重新计算剩余节点的预计到达时间
     *
     * @param nodes 过滤后的节点列表
     * @param riderLat 当前位置
     * @param riderLng 当前位置
     * @return 更新了时间信息的节点列表
     */
    fun refreshTimeline(
        nodes: List<OrderNode>,
        location: Pair<Double, Double>
    ): List<OrderNode> {
        // 1. 复制一份列表（防止直接修改入参导致的状态问题，Compose 推荐做法）
        val updatedNodes = nodes.map { it.copy() } // data class copy 是浅拷贝，够用了

        // 2. 复用之前的 calculateCost 逻辑来填充时间
        // calculateCost 会修改 list 中对象的 estimatedTime 和 isOvertime 属性
        calculateRouteCost(updatedNodes, location)

        return updatedNodes
    }

}