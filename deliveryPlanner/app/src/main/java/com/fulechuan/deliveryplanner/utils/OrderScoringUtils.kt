package com.fulechuan.deliveryplanner.utils

import com.amap.api.maps.AMapUtils
import com.amap.api.maps.model.LatLng
import com.fulechuan.deliveryplanner.model.data.Order
import com.fulechuan.deliveryplanner.model.data.ScoredOrder
import kotlin.math.log2


object OrderScoringUtils {

    // 基础参数配置
    private const val RIDER_SPEED_KPH = 20.0 // 骑手均速 20km/h
    private const val BASE_WAIT_MIN = 10.0   // 基础等餐/停车耗时 (分钟)

    /**
     * 对候选订单列表进行打分和排序
     */
    fun rankOrders(
        candidates: List<Order>,
        location: Pair<Double,Double>
    ): List<ScoredOrder> {

        return candidates.map { order ->
            evaluate(order, location.first, location.second)
        }.sortedByDescending { it.totalScore } // 按总分从高到低排
    }

    private fun evaluate(order: Order, rLat: Double, rLng: Double): ScoredOrder {

        // --- 1. 基础数据准备 ---

        // A. 计算【距离分】
        // 我到商家的距离 (米)
        val distanceToPickup = getDistance(rLat, rLng, order.pickupLoc.x, order.pickupLoc.y) // order.pickupLoc 是商家坐标
        // 订单的总路程 (取货+送货) -> 往往需要从文本里解析出距离数值
        val deliveryDist = order.pickupToDeliveryDistance * 1000 // 转米
        val totalDistMeters = distanceToPickup + deliveryDist

        // B. 计算【时薪分】
        // 总耗时 (小时) = (总路程km / 速度) + (基础等待时间 / 60)
        val totalTimeHours = (totalDistMeters / 1000.0 / RIDER_SPEED_KPH) + (BASE_WAIT_MIN / 60.0)
        // 预估时薪 = 价格 / 小时
        val hourlyRate = if (totalTimeHours > 0) order.price / totalTimeHours else 0.0

        // --- 2. 归一化打分 (0-100分制) ---

        // 距离分：距离越近分越高。假设 500米以内满分，5公里0分
        val scoreDist = calculateLinearScore(deliveryDist, 500.0, 5000.0, reverse = true)

        // 价格分：价格越高分越高。假设 30元满分，3元0分
        val scorePrice = calculateLinearScore(order.price, 3.0, 30.0, reverse = false)

        // 时薪分：时薪越高分越高。假设 时薪30元满分，时薪15元0分
        val scoreRate = calculateLinearScore(hourlyRate, 15.0, 30.0, reverse = false)

        // --- 3. 综合加权总分 ---
        // 权重策略：时薪最重要(50%)，距离次之(30%)，绝对价格最后(20%)
        val totalScore = (scoreRate * 0.5 + scoreDist * 0.3 + scorePrice * 0.2).toInt()

        // --- 4. 生成标签 (Tags) ---
        val tags = mutableListOf<String>()

        if (distanceToPickup < 300) tags.add("📍 就在楼下")
        else if (distanceToPickup < 1000) tags.add("⚡️ 取货近")

        if (hourlyRate > 40) tags.add("💎 高时薪")
        else if (order.price / (totalDistMeters/1000) > 4.0) tags.add("💰 单价高") // 每公里单价

        if (totalScore > 80) tags.add("🔥 系统力荐")

        return ScoredOrder(
            order = order,
            distanceVal = deliveryDist,
            priceVal = order.price,
            hourlyRateVal = hourlyRate,
            totalScore = totalScore,
            tags = tags,
            recommendColor = if(totalScore > 80) 0xFF4CAF50 else 0xFFFFC107 // 绿 or 黄
        )
    }

    /**
     * 线性打分工具
     * @param value 当前值
     * @param min 门槛下限
     * @param max 门槛上限
     * @param reverse 是否反向 (true代表越小越好，如距离)
     */
    private fun calculateLinearScore(value: Double, min: Double, max: Double, reverse: Boolean): Int {
        var v = value
        if (v < min) v = min
        if (v > max) v = max

        // 归一化 0.0 - 1.0
        var ratio = (v - min) / (max - min)

        if (reverse) {
            ratio = 1.0 - ratio
        }

        return (ratio * 100).toInt()
    }

    // 距离计算(高德)
    private fun getDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        return AMapUtils.calculateLineDistance(LatLng(lat1,lon1), LatLng(lat2,lon2))
    }


}