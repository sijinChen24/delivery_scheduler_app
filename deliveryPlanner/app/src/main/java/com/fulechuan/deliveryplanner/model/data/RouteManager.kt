package com.fulechuan.deliveryplanner.model.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.deliveryplanner.utils.AlgorithmUtils
import com.example.deliveryplanner.utils.AlgorithmUtils.isNeedLock
import com.fulechuan.deliveryplanner.db.AppDatabase
import com.fulechuan.deliveryplanner.enums.OverlayMode
import com.fulechuan.deliveryplanner.utils.MapUtils
import com.fulechuan.deliveryplanner.utils.OrderScoringUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * 路由管理器,用于策略和viewModel的中间桥梁
 * 策略不直接引用viewModel,而是通过RouteManager间接协调数据和算法
 * 无障碍服务和viewmodel生命周期不同,若应用程序进程被杀死,无障碍服务会引用空指针,从而内存泄漏
 * 路由管理器的职责:负责接应
 *
 */

class RouteManager private constructor(private val context: Context) {

    private val orderDao = AppDatabase.getDatabase(context).orderDao()
    private val scope = CoroutineScope(Dispatchers.Default)

    // 暂存区：用户点了列表抢单，但还没点确认
    private var tempCandidate: Order? = null

    //全局单例
    companion object {
        @Volatile private var INSTANCE: RouteManager? = null
        fun getInstance(context: Context): RouteManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: RouteManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * 核心逻辑: 分析候选订单 (供 Strategy 调用)
     */
    fun analyzeCandidate(candidateOrders: List<Order>){
        if(candidateOrders.isEmpty()){
            return
        }
        scope.launch {

            // 1. 获取定位 (一行代码，自动挂起，直到拿到结果)
            val location = MapUtils.getCurrentLocation(context)

            if (location == null) {
                Log.e("RouteManager", "定位失败，无法计算顺路程度")
                // 兜底策略：可以使用上一次缓存的位置，或者提示用户"GPS信号弱"
                return@launch
            }

            // 2. 从内存获取当前任务列表
            val activeOrderNodes = GlobalState.plannedRoute
            if(activeOrderNodes.isEmpty()){
                //=== 场景 1: 冷启动模式 ===
                // 调用评分工具
                val rankedList = OrderScoringUtils.rankOrders(candidateOrders, location)

                // 更新 GlobalState，通知悬浮窗显示“评分列表”
                GlobalState.scoredCandidates.clear()
                GlobalState.scoredCandidates.addAll(rankedList)
                GlobalState.currentMode = OverlayMode.COLD_START
            }else{
                //=== 场景 2: 决策模式 ===
                // 调用算法引擎 (AlgorithmUtils)
                val (finalRoute, acceptedList) = AlgorithmUtils.simulateIsFeasible(
                    currentNodeTask = activeOrderNodes,
                    newOrderList = candidateOrders,
                    currentLocation = location
                )
                // 更新 UI 状态
                if (acceptedList.isNotEmpty()) {
                    // 告诉用户哪些能接
                    // 比如显示：建议接单 [麦当劳, 肯德基]，忽略 [星巴克]
                    GlobalState.acceptedList.clear()
                    GlobalState.refusedList.clear()
                    GlobalState.acceptedList = acceptedList as SnapshotStateList<Order>
                    val refusedList = candidateOrders-acceptedList
                    GlobalState.refusedList = refusedList as SnapshotStateList<Order>
                    return@launch

                } else {
                    // 全部都不顺路
                }
            }
        }

    }


    /**
     * 🟢 乐观抢单 (点击瞬间调用)
     * 只更新内存 GlobalState，不写库
     */
    fun preJoinOrder(order: Order) {
        scope.launch {
            tempCandidate = order

            // 1. 获取当前内存中的路线 (旧路线)
            val currentNodes = GlobalState.plannedRoute.toList()

            //2.获取当前位置
            val location = MapUtils.getCurrentLocation(context) ?: Pair(0.0, 0.0)

            // 2. 跑增量算法：把新订单插进去
            //3. 判断是否需要锁定当前任务节点
            var isNeedLock: Boolean
            if(currentNodes.isEmpty()){
                isNeedLock = true
            }else{
                val firstNode = currentNodes[0]
                isNeedLock = isNeedLock(firstNode, location)
            }

            val newRoute = AlgorithmUtils.insertIntoExistingRoute(currentNodes, order, location, isNeedLock)

            // 3. 更新 UI (用户立刻看到路线变了)
            GlobalState.plannedRoute.clear()
            GlobalState.plannedRoute.addAll(newRoute)
            GlobalState.currentMode = OverlayMode.NAVIGATION

            Log.d("RouteManager", "乐观更新：已将订单加入临时路线")
        }
    }

    /**
     * 核心逻辑: 抢单成功，入库 (供 Strategy 或 ViewModel 调用)
     */
    fun confirmOrder(order: Order) {
        val order = tempCandidate ?: return
        scope.launch {
            Log.d("RouteManager", "抢单成功，持久化数据")
            // 1. 写入数据库
            orderDao.insertOrder(order)
            // 2. 清空暂存 (路线不用变了，因为 preJoin 已经变过了)
            tempCandidate = null
        }
    }

    /**
     * 🔴 抢单失败回滚 (检测到失败 Toast 调用)
     */
    fun rollbackOrder(order: Order) {
        // 如果没有正在抢的单子，直接返回
        val failedOrder = tempCandidate ?: return
        scope.launch {
            Log.w("RouteManager", "抢单失败，执行内存回滚: ${failedOrder.id}")

            // 1. 获取当前 GlobalState 中的路线快照
            val currentNodes = GlobalState.plannedRoute.toList()

            // 2. 【核心步骤】内存过滤
            // 剔除所有 orderId 等于失败订单 ID 的节点
            // (因为一个订单有 Pickup 和 Delivery 两个节点，filter 会把两个都删掉)
            val filteredNodes = currentNodes.filter { it.orderId != failedOrder.id }

            // 3. 获取当前位置 (用于重算时间)
            // 如果拿不到定位，就用 0.0 或者上次的位置，影响不大，反正回滚的是“未发生”的事
            val location = MapUtils.getCurrentLocation(context) ?: Pair(0.0, 0.0)

            // 4. 【关键】重新计算剩余节点的时间轴
            // 否则后面的节点时间会显示得比实际晚（因为中间少跑了一段路）
            val correctedRoute = AlgorithmUtils.refreshTimeline(filteredNodes, location)

            // 5. 更新 UI
            GlobalState.plannedRoute.clear()
            GlobalState.plannedRoute.addAll(correctedRoute)

            // 6. 恢复模式 (如果回滚后没有节点了，可能要隐藏导航，或者保持空导航)
            if (correctedRoute.isEmpty()) {
                // 如果本来手里也没单，抢第一单失败了，那就切回 DECISION 模式或 HIDDEN
                 GlobalState.currentMode = OverlayMode.DECISION
            } else {
                GlobalState.currentMode = OverlayMode.NAVIGATION
            }

            // 7. 清空暂存对象
            tempCandidate = null
        }
    }

    /**
     * 🚀 核心方法：恢复现场
     * 在 APP 启动 (MainActivity onCreate) 时调用一次
     */
    fun restoreState() {
        scope.launch {
            // 1. 从数据库读取所有 "进行中" 的订单
            // (状态是 ACCEPTED 或 PICKED_UP)
            val activeOrders = orderDao.getActiveOrders()

            activeOrders.collect { currentOrderList ->
                if (currentOrderList.isEmpty()) {
                    // 如果没单子，清空 UI
                    GlobalState.plannedRoute.clear()
                    GlobalState.currentMode = OverlayMode.HIDDEN
                }else{
                    // 2. 获取当前位置
                    // (刚启动可能还没定位成功，可以尝试拿一次，拿不到就用 0,0 或者上次缓存的位置)
                    val location = MapUtils.getCurrentLocation(context)?: Pair(0.0, 0.0)

                    // 3. 【关键】重新跑一遍算法
                    // 即使 App 挂了，只要数据库还在，路线就能算出来
                    val restoredRoute = AlgorithmUtils.planRouteFromScratch(currentOrderList, location)

                    // 4. 恢复 GlobalState (内存)
                    GlobalState.plannedRoute.clear()
                    GlobalState.plannedRoute.addAll(restoredRoute)

                    // 5. 恢复悬浮窗状态
                    GlobalState.currentMode = OverlayMode.NAVIGATION

                    Log.d("RouteManager", "现场已恢复: 重新加载了 ${currentOrderList.size} 个订单")
                }
            }


        }
    }


    /**
     * 当用户在前台完成任务/取消订单后调用
     * 重新读取数据库，刷新内存中的路线
     */
    fun refreshRouteAfterAction() {
        // 其实这就等同于“恢复现场”，因为数据库状态已经变了
        // 直接复用 restoreState 即可，或者写一个类似的轻量级方法
        restoreState()
    }

}






