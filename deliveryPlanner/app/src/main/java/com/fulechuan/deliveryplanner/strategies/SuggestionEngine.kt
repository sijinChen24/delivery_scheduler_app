package com.fulechuan.deliveryplanner.strategies

import com.fulechuan.deliveryplanner.model.data.Order
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * 一个单例的建议引擎，作为 Service 和 ViewModel 之间的通信桥梁。
 * Service 是数据生产者，ViewModel 是数据消费者。
 */
object SuggestionEngine {

    // 私有的、可变的 SharedFlow，用于内部接收数据
    private val _scannedOrdersFlow = MutableSharedFlow<List<Order>>(replay = 1)

    // 公开的、只读的 SharedFlow，供外部订阅
    val scannedOrdersFlow = _scannedOrdersFlow.asSharedFlow()

    /**
     * 由 Strategy 调用，用于发布刚刚从外部App扫描到的订单列表。
     */
    suspend fun postScannedOrders(orders: List<Order>) {
        _scannedOrdersFlow.emit(orders)
    }


}