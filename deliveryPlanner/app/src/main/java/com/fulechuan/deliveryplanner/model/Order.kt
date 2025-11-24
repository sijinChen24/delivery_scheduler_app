package com.fulechuan.deliveryplanner.model


import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.fulechuan.deliveryplanner.enums.OrderStatus
import com.fulechuan.deliveryplanner.enums.PlatFormStatus
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// 1. 类型转换器 (处理 Point 和 Enum)
class Converters {
    // Point -> String ("50.0,20.0")
    @TypeConverter
    fun fromPoint(point: Point): String {
        return "${point.x},${point.y}"
    }

    // String -> Point
    @TypeConverter
    fun toPoint(data: String): Point {
        val pieces = data.split(",")
        return Point(pieces[0].toFloat(), pieces[1].toFloat())
    }

    // OrderStatus -> String
    @TypeConverter
    fun fromStatus(status: OrderStatus): String {
        return status.name
    }

    @TypeConverter
    fun toStatus(value: String): OrderStatus {
        return OrderStatus.valueOf(value)
    }
}

// 订单模型
@Entity(tableName = "orders")
data class Order(

    @PrimaryKey
    val id: String,
    val shopName: String, //商家名称
    val customerName: String, //客户名称
    val price: Double,
    val pickupAddress: String?,
    val deliveryAddress: String?,
    val pickupLoc: Point,
    val deliveryLoc: Point,
    val pickupDeadline: Long, // 时间戳
    val deliveryDeadline: Long, // 时间戳
    var status: OrderStatus = OrderStatus.NEW_OFFER,
    val createTime : String ,
    val finishTime : String,
    val platForm: PlatFormStatus
)
