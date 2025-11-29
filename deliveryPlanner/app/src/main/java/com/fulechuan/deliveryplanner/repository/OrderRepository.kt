package com.fulechuan.deliveryplanner.repository

import android.content.Context
import com.fulechuan.deliveryplanner.model.dao.OrderDao
import com.fulechuan.deliveryplanner.db.AppDatabase
import com.fulechuan.deliveryplanner.model.data.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext


/**
 * 订单数据的唯一真实来源 (Single Source of Truth)
 * 连接 ViewModel 和 数据源 (DAO)
 */
class OrderRepository(private val orderDao: OrderDao) {

    /**
     * 从数据库获取所有进行中的订单。
     * 返回一个 Flow，当数据变化时会自动发出新数据。
     */
    fun getActiveOrders(): Flow<List<Order>> {
        return orderDao.getActiveOrders()
    }

    /**
     * 插入一个新订单。
     * 这是一个 suspend 函数，因为它执行数据库 IO 操作。
     * 使用 withContext(Dispatchers.IO) 确保它总是在后台线程执行。
     */
    suspend fun insertOrder(order: Order) {
        withContext(Dispatchers.IO) {
            orderDao.insertOrder(order)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: OrderRepository? = null

        fun getInstance(context: Context): OrderRepository {
            return INSTANCE ?: synchronized(this) {
                val database = AppDatabase.Companion.getDatabase(context)
                val instance = OrderRepository(database.orderDao())
                INSTANCE = instance
                instance
            }
        }
    }
}