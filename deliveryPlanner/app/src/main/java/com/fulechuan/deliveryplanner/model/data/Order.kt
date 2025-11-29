package com.fulechuan.deliveryplanner.model.data


import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.fulechuan.deliveryplanner.enums.OrderStatus
import com.fulechuan.deliveryplanner.enums.PlatFormStatus

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
        return Point(pieces[0].toDouble(), pieces[1].toDouble())
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

    // PlatFormStatus -> String
    @TypeConverter
    fun fromPlatFormStatus(status: PlatFormStatus): String {
        return status.name
    }

    @TypeConverter
    fun toPlatFormStatus(value: String): PlatFormStatus {
        return PlatFormStatus.valueOf(value)
    }
}

// 订单模型
@Entity(tableName = "orders")
data class Order(

    @PrimaryKey
    val id: String,
    val shopName: String, //商家名称
    val customName : String, //客户名称
    val pickupAddress: String?, //取货地址
    val pickupLoc: Point,  //取货坐标
    val deliveryAddress: String?, //送货地址
    val deliveryLoc: Point,  //送货坐标
    val price: Double,
    val pickupDeadlineText: String, // 取货最晚时间文本
    val deliveryDeadlineText: String, // 送货最晚时间文本
    val pickupDeadline: Long=0L, // 取货最晚时间  毫秒值
    val deliveryDeadline: Long=0L, // 送货最晚时间 。毫秒值
    val pickupToDeliveryDistance: Double, //取货点到送货点的距离(从列表扫描到)
    var status: OrderStatus = OrderStatus.NEW_OFFER,
    val createTime: String,
    val finishTime: String?,
    val platForm: PlatFormStatus,
    val orderType : String
)
