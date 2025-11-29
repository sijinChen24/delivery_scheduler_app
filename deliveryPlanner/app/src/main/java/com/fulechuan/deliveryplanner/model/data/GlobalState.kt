package com.fulechuan.deliveryplanner.model.data


import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fulechuan.deliveryplanner.enums.OverlayMode
import android.app.PendingIntent

/**
 * 提供悬浮窗ui所需数据
 */
object GlobalState {


    // 控制悬浮窗显示什么模式
    var currentMode by mutableStateOf(OverlayMode.HIDDEN)

    // 扫描到的建议列表 (决策模式用)
    var acceptedList = mutableStateListOf<Order>()

    var refusedList = mutableStateListOf<Order>()


    // 规划好的路线节点 (导航模式用) -存 RouteNode 对象
    var plannedRoute = mutableStateListOf<OrderNode>()

    //扫描到的评分列表(冷启动模式用) -用户根据评分等级自己去接单
    var scoredCandidates = mutableStateListOf<ScoredOrder>()

    // 新增：最新收到的外卖通知 intent (用于跳转)
    var pendingNotificationIntent by mutableStateOf<PendingIntent?>(null)

    // 或者简单点，只存个标记和包名
    var latestNotificationPackage by mutableStateOf<String?>(null)


    // 辅助方法：重置所有状态 (比如切回自己 App 时)
    fun reset() {
        currentMode = OverlayMode.HIDDEN
        acceptedList.clear()
        refusedList.clear()
        // plannedRoute 通常不清除，除非用户手动结束
    }
}