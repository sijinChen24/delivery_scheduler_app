package com.fulechuan.deliveryplanner.view

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import com.fulechuan.deliveryplanner.R
import com.fulechuan.deliveryplanner.enums.OrderStatus
import com.fulechuan.deliveryplanner.enums.TaskType
import com.fulechuan.deliveryplanner.model.Order
import com.fulechuan.deliveryplanner.model.Point
import com.fulechuan.deliveryplanner.model.RouteNode
import kotlin.collections.forEach
import kotlin.math.pow
import kotlin.math.sqrt

// ================= 核心逻辑 & 算法 View Model =================
class DeliveryViewModel {
    // 骑手当前位置
    var currentLoc = mutableStateOf(Point(50F, 50F)) // 假设地图是一个 100x100 的网格

    // 已接订单列表
    var activeOrders = mutableStateListOf<Order>()

    // 规划好的路径队列
    var plannedRoute = mutableStateListOf<RouteNode>()

    // 新进来的外卖推送 (待处理)
    var incomingOffer = mutableStateOf<Order?>(null)

    // 模拟日志
    var logInfo = mutableStateOf("系统就绪，等待订单...")

    // 算法常量
    private val SPEED = 10.0 // 模拟速度: 10单位/分钟
    private val PICKUP_TIME = R.string.picked_time // 取餐耗时(分钟)
    private val DELIVERY_TIME = R.string.delivery_time // 送餐耗时(分钟)

    /**
     * 核心算法：智能判断订单是否可接
     * 逻辑：尝试将新订单的 P(取) 和 D(送) 插入到当前路径中
     * 如果找到一种插入方式，使得总路程增加最少，且不导致任何旧订单超时，则返回 true
     */
    fun evaluateOrder(newOrder: Order): Pair<Boolean, List<RouteNode>?> {
        // 如果当前没有任务，直接接单
        if (plannedRoute.isEmpty()) {
            val simpleRoute = listOf(
                RouteNode(TaskType.PICKUP, newOrder, newOrder.pickupLoc),
                RouteNode(TaskType.DELIVERY, newOrder, newOrder.deliveryLoc)
            )
            return if (checkTimeConstraints(simpleRoute)) true to simpleRoute else false to null
        }

        // 深度复制当前路径用于模拟
        val baseRoute = plannedRoute.toList()
        var bestRoute: List<RouteNode>? = null
        var minAddedDist = Double.MAX_VALUE

        // 暴力插入法 (简化版 TSP Insertion)
        // 必须先 Pick 后 Deliver
        // 尝试在位置 i 插入 Pickup, 在位置 j 插入 Delivery (j > i)
        for (i in 0..baseRoute.size) {
            for (j in i + 1..baseRoute.size + 1) {
                val tempRoute = baseRoute.toMutableList()

                // 插入 Pickup
                val pickupNode = RouteNode(TaskType.PICKUP, newOrder, newOrder.pickupLoc)
                tempRoute.add(i, pickupNode)

                // 插入 Delivery
                val deliveryNode = RouteNode(TaskType.DELIVERY, newOrder, newOrder.deliveryLoc)
                tempRoute.add(j, deliveryNode)

                // 1. 检查是否超时
                if (checkTimeConstraints(tempRoute)) {
                    // 2. 计算总距离增加量
                    val dist = calculateTotalDistance(tempRoute)
                    if (dist < minAddedDist) {
                        minAddedDist = dist
                        bestRoute = tempRoute
                    }
                }
            }
        }

        return if (bestRoute != null) true to bestRoute else false to null
    }

    // 检查路径中的所有节点是否都能在 Deadline 前完成
    private fun checkTimeConstraints(route: List<RouteNode>): Boolean {
        var currentTime = System.currentTimeMillis()
        var currentPos = currentLoc.value

        route.forEach { node ->
            val dist = distance(currentPos, node.location)
            val travelTimeMillis = (dist / SPEED * 60 * 1000).toLong()
            val operationTimeMillis = if(node.type == TaskType.PICKUP) PICKUP_TIME * 60000L else DELIVERY_TIME * 60000L

            currentTime += travelTimeMillis + operationTimeMillis

            // 如果是送餐节点，必须在 Deadline 前完成
            if (node.type == TaskType.DELIVERY) {
                if (currentTime > node.deadline) {
                    return false // 超时！
                }
            }
        }
        return true
    }

    private fun calculateTotalDistance(route: List<RouteNode>): Double {
        var total = 0.0
        var prev = currentLoc.value
        route.forEach {
            total += distance(prev, it.location)
            prev = it.location
        }
        return total
    }

    private fun distance(p1: Point, p2: Point): Float {
        return sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
    }

    // 用户点击“接单”
    fun acceptOffer(newOrder: Order, optimizedRoute: List<RouteNode>) {
        newOrder.status = OrderStatus.ACCEPTED
        activeOrders.add(newOrder)
        plannedRoute.clear()
        plannedRoute.addAll(optimizedRoute)
        incomingOffer.value = null
        logInfo.value = "已接单：${newOrder.shopName}，路线已重新规划"
    }

    fun rejectOffer() {
        incomingOffer.value = null
    }

}