package com.fulechuan.deliveryplanner.utils

import androidx.core.net.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale


/**
 * 获取当前时间字符串
 */
fun toNowTimeString(): String{
    val  currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    return currentTime
}

/**
 * 将字符串转为时间戳
 * @param timeStr 时间字符串，如 "2025-11-26 15:30:00"
 * @param pattern 格式模版，如 "yyyy-MM-dd HH:mm:ss"
 */
fun dateStringToLong(timeStr: String): Long {
    if(timeStr.isEmpty()){
        return 0L
    }

    var pattern = "yyyy-MM-dd HH:mm:ss"
    // 验证timeStr是否标准的yyyy-MM-dd HH:mm:ss
    if(!isStandardDateTimeFormatLegacy(timeStr)){
        return timeFormatHandler(timeStr)
    }else{
        return try {
            val sdf = SimpleDateFormat(pattern, Locale.getDefault())
            val date: Date? = sdf.parse(timeStr)
            date?.time ?: 0L // 如果解析失败返回 0
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }

    }
    return 0L
}


/**
 * (传统方式) 判断字符串是否为 "yyyy-MM-dd HH:mm:ss" 格式并且是一个有效的日期时间。
 *
 * @param dateString 要验证的字符串。
 * @return 如果格式正确且日期有效，返回 true；否则返回 false。
 */
fun isStandardDateTimeFormatLegacy(dateString: String?): Boolean {
    // 1. 检查输入是否为 null 或空白
    if (dateString.isNullOrBlank()) {
        return false
    }

    // 2. 创建 SimpleDateFormat 实例
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // 3. 设置为严格模式！这是最重要的一步。
    // 如果不设置，它会尝试"宽容地"解析，比如把 "2023-01-32" 解析成 "2023-02-01"，这不是我们想要的。
    sdf.isLenient = false

    // 4. 尝试解析
    return try {
        // parse 方法如果成功，会返回一个 Date 对象。我们只需要知道它没抛异常就行。
        sdf.parse(dateString)
        true
    } catch (e: ParseException) {
        // 如果抛出 ParseException 异常，说明格式不匹配或日期无效
        false
    }
}

/**
 * 将 "HH:mm" (如 "18:35") 转为 今天该时间的时间戳
 * 自动处理跨天逻辑（如果当前是23点，订单是01点，通常指明天）
 */
private fun timeFormatHandler(timeStr: String): Long {
    try {

        // 1. 获取当前日期
        val calendar = Calendar.getInstance()
        val currentYear = calendar.get(Calendar.YEAR)
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // 月份从0开始
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        // 2. 拼凑成完整字符串: "2025-11-26 18:35"
        val fullDateStr = "$currentYear-$currentMonth-$currentDay $timeStr"

        // 3. 解析
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        var timestamp = sdf.parse(fullDateStr)?.time ?: 0L

        // 4. (可选) 简单的跨天处理逻辑
        // 如果解析出来的时间比当前时间早很多(比如早12小时以上)，说明可能是明天的凌晨单
        // 比如现在 23:00，订单显示 00:30，拼接今天日期就是过去的 00:30，显然不对，应该是明天 00:30
        val now = System.currentTimeMillis()
        if (timestamp < now - 12 * 60 * 60 * 1000) {
            timestamp += 24 * 60 * 60 * 1000 // 加一天
        }

        return timestamp

    } catch (e: Exception) {
        return 0L
    }
}

/**
 * 将毫秒时间戳转换为 "yyyy-MM-dd HH:mm:ss" 格式的字符串
 * @param millis 毫秒时间戳 (Long)
 * @return 格式化后的字符串
 */
 fun toTimeString(millis: Long): String {
    // 1. 创建 Date 对象
    val date = Date(millis)

    // 2. 创建格式化工具 (指定格式 和 语言环境)
    // Locale.getDefault() 会根据手机系统语言自动适配
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    // 3. 执行格式化
    return sdf.format(date)
}
