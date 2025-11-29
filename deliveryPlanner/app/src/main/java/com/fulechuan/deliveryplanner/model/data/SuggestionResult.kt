package com.fulechuan.deliveryplanner.model.data

/**
 * 数据类：用于封装一条接单建议
 *
 * @param candidate 相关的订单
 * @param isFeasible 是否可行
 * @param reason 提出该建议的原因 (例如 "距离最近", "顺路单")
 */
data class SuggestionResult(
    val acceptedList: List<Order>,
    val refusedList: List<Order>,
)


/**
 * 带有评分的订单包装类
 * 用于冷启动时的列表排序展示
 */
data class ScoredOrder(
    val order: Order,

    // 三大核心指标 (显示给用户参考)
    val distanceVal: Double,   // 距我多远 (米)
    val priceVal: Double,      // 订单金额 (元)
    val hourlyRateVal: Double, // 预估时薪 (元/小时)

    // 综合打分 (用于内部排序, 0-100分)
    val totalScore: Int,

    // 推荐标签 (例如: "💰 高价单", "📍 极速取", "💎 性价比")
    val tags: List<String>,

    // UI 显示颜色 (Green=推荐, Yellow=一般)
    val recommendColor: Long = 0xFF4CAF50
)