package com.fulechuan.deliveryplanner.utils

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 从文本中提取时间并格式化为 HH:mm:00
 * 输入: "65分钟内(18:35前)"
 * 输出: "18:35:00"
 */
fun parseDeadline(text: String): String {
    // 1. 定义正则：匹配冒号分隔的时间格式
    val regex = Regex("""(\d{1,2}:\d{2})""")

    // 2. 查找匹配项
    val matchResult = regex.find(text)

    // 3. 处理结果
    return if (matchResult != null) {
        val timeStr = matchResult.value // 拿到 "18:35"
        "$timeStr:00" // 拼接秒数，返回 "18:35:00"
    } else {
        "" // 或者返回当前时间/默认时间
    }
}



