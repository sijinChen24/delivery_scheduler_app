package com.fulechuan.deliveryplanner.model.data

import com.fulechuan.deliveryplanner.enums.TaskType


// 路径节点 (规划出的每一个步骤)
data class OrderNode(
    val orderId: String,
    val type: TaskType,
    val name: String, // 如果是送货，这里可以显示"送：xx小区"
    val address: String?,
    val location: Point,
    val deadline: Long =0L, // 时间戳
    var estimatedArrival: Long =0L,
    var isOvertime: Boolean = false // 是否超时

){
    // 辅助方法：获取动作名称
    fun getActionName(): String = if (type == TaskType.PICKUP) "取" else "送"
}