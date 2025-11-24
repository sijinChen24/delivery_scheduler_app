package com.fulechuan.deliveryplanner.model

import com.fulechuan.deliveryplanner.enums.TaskType


// 路径节点 (规划出的每一个步骤)
data class RouteNode(
    val type: TaskType,
    val order: Order,
    val location: Point,
    val deadline: Long=0L, // 时间戳
    val estimatedArrival: Long=0L,
    val info: String=""

)