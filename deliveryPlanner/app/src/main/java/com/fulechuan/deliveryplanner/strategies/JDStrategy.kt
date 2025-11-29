package com.fulechuan.deliveryplanner.strategies

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.fulechuan.deliveryplanner.R

class JDStrategy (private val context: Context) : PlatformStrategy{

    override val targetPackage: String by lazy {
        context.getString(R.string.jd_package_name)
    }

    override fun onAccessibilityEvent(
        event: AccessibilityEvent,
        rootNode: AccessibilityNodeInfo?
    ) {
        TODO("Not yet implemented")
    }

    override fun onUserClick(node: AccessibilityNodeInfo) {
        TODO("Not yet implemented")
    }

}