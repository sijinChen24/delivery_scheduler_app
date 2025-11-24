package com.fulechuan.deliveryplanner.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fulechuan.deliveryplanner.model.Order
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Query("SELECT * FROM orders WHERE status != 'DELIVERED' OR status != 'NEW_OFFER'")
    fun getActiveOrders(): Flow<List<Order>> // 使用 Flow 实时监控数据库变化

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: Order)

    @Delete
    suspend fun deleteOrder(order: Order)

    @Query("DELETE FROM orders")
    suspend fun clearAll()
}