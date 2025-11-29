package com.fulechuan.deliveryplanner.strategies

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

interface PlatformStrategy {

    // 目标包名
    val targetPackage: String

    // 处理页面滚动/内容变化 (用于扫描列表)
    fun onAccessibilityEvent(event: AccessibilityEvent, rootNode: AccessibilityNodeInfo?)

    // 处理用户点击 (用于捕获接单动作)
    fun onUserClick(node: AccessibilityNodeInfo)
}