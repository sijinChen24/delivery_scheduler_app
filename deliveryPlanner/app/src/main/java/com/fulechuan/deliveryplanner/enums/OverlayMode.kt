package com.fulechuan.deliveryplanner.enums

/**
 * 定义悬浮窗显示模式
 */
enum class OverlayMode {

    HIDDEN,  //平台处于前台时,隐藏
    COLD_START, //冷启动模式,显示待评分的订单推荐
    DECISION,  //决策模式,显示接单建议
    NAVIGATION  //导航模式,显示任务节点
}