package com.fulechuan.deliveryplanner.viewModel

import android.app.Application
import android.location.Location
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.util.copy
import com.fulechuan.deliveryplanner.db.AppDatabase
import com.fulechuan.deliveryplanner.enums.OrderStatus
import com.fulechuan.deliveryplanner.enums.TaskType
import com.fulechuan.deliveryplanner.model.data.GlobalState
import com.fulechuan.deliveryplanner.model.data.Order
import com.fulechuan.deliveryplanner.model.data.OrderNode
import com.fulechuan.deliveryplanner.model.data.Point
import com.fulechuan.deliveryplanner.model.data.RouteManager
import kotlinx.coroutines.launch


/**
 * 主要负责观察数据库 (Flow) 和转发用户操作给 RouteManager
 */


class DeliveryViewModel(application: Application) : AndroidViewModel(application) {


    // Repository 的单例
    private val orderDao = AppDatabase.getDatabase(application).orderDao()

    // 2. 获取全局路由管理器 (单例)
    private val routeManager = RouteManager.getInstance(application)

    /**
     * 📋 已接订单列表 (实时流)
     * UI (MainActivity) 里的列表应该观察这个 Flow。
     * 只要数据库发生变动 (Service 插入了新单 / 用户完成了订单)，这里会自动更新。
     */
    val activeOrders = orderDao.getActiveOrders()

    // 注意：路线规划数据 (plannedRoute) 建议 UI 直接读取 GlobalState
    // 因为它是针对 Compose 优化的 SnapshotStateList
    val currentRoute = GlobalState.plannedRoute


    // 骑手当前位置
    var currentLoc = mutableStateOf(Point(0.0, 0.0)) // 初始化


    // 模拟日志
    var logInfo = mutableStateOf("系统就绪，等待订单...")


    /**
     * 【新增】公开方法，用于从 UI 更新骑手位置
     */
    fun updateCurrentLocation(location: Location) {
        //我们直接用经纬度的值
        currentLoc.value = Point(location.longitude, location.latitude)
        Log.d(
            "viewModel",
            currentLoc.toString()
        )
    }


    /**
     * ✅ 用户点击“完成/到达”按钮
     * @param node 当前完成的任务节点
     */
    fun completeTask(node: OrderNode) {
        viewModelScope.launch {
            // 1. 找到对应的订单
            // 注意：这里需要根据 ID 查出订单，修改状态
            // 简单起见，我们假设可以通过 DAO 更新状态

            // 逻辑分支：
            // 如果是 PICKUP 节点 -> 更新状态为 PICKED_UP
            // 如果是 DELIVERY 节点 -> 更新状态为 COMPLETED (归档)

            val orderId = node.orderId
            val isDelivery = node.type == TaskType.DELIVERY

            // 这里需要在 Dao 加一个更新状态的方法，或者先查再改
            // 伪代码示例：
            val order = orderDao.getOrderById(orderId)

            val newStatus = if (isDelivery) OrderStatus.DELIVERED else OrderStatus.PICKED_UP
            val updatedOrder = order.copy(status = newStatus)

            // 更新数据库
            orderDao.insertOrder(updatedOrder)

            // ⚡️ 关键：告诉 RouteManager 重新规划路线
            // 因为少了一个节点，剩下的路线需要刷新 (去除已完成的，并可能重新排序)
            routeManager.refreshRouteAfterAction()
        }
    }

    /**
     * 🛑 用户手动取消订单
     */
    fun cancelOrder(order: Order) {
        viewModelScope.launch {
            // 从数据库删除
            orderDao.deleteOrder(order)
            // 触发重排
            routeManager.refreshRouteAfterAction()
        }
    }


    /**
     * 🔄 用户手动强行刷新 (调试用)
     */
    fun forceRefresh() {
        routeManager.restoreState() // 复用恢复现场的逻辑进行全量重算
    }
}