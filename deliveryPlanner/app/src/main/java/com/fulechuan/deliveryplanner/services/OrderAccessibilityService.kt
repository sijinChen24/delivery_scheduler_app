package com.fulechuan.deliveryplanner.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.fulechuan.deliveryplanner.R
import com.fulechuan.deliveryplanner.model.data.GlobalState
import com.fulechuan.deliveryplanner.enums.OverlayMode
import com.fulechuan.deliveryplanner.strategies.JDStrategy
import com.fulechuan.deliveryplanner.strategies.PlatformStrategy
import com.fulechuan.deliveryplanner.strategies.UUStrategy

/**
 * 无障碍服务总控
 */
class OrderAccessibilityService : AccessibilityService() {

    private val strategies = HashMap<String, PlatformStrategy>()
    private var currentStrategy: PlatformStrategy? = null

    private val uuPackageName by lazy {
        getString(R.string.uu_package_name)
    }
    private val jdPackageName by lazy {
        getString(R.string.jd_package_name)
    }


    override fun onServiceConnected() {
        super.onServiceConnected()
        // 1. 启动信号
        Log.e("orderAccessibility", ">>> 无障碍服务已连接！系统已激活！ <<<")
        // 注册策略
        strategies[uuPackageName] = UUStrategy(this)
        strategies[jdPackageName] = JDStrategy(this)

        // ⚠️【新增保险丝】检查悬浮窗权限
        if (android.provider.Settings.canDrawOverlays(this)) {
            // 只有权限开启了，才启动悬浮窗
            try {
                startService(Intent(this, OverlayService::class.java))
            } catch (e: Exception) {
                Log.e("orderAccessibility", "启动悬浮窗失败: ${e.message}")
            }
        } else {
            Log.e("orderAccessibility", "没有悬浮窗权限，跳过启动 OverlayService")
            // 弹个 Toast 提示用户去开权限
            Toast.makeText(this, "请开启悬浮窗权限", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        Log.d("orderAccessibility", "检测到事件event")
        event ?: return
        val pkg = event.packageName?.toString() ?: return

        //只过滤TYPE_WINDOW_CONTENT_CHANGED 和 TYPE_VIEW_SCROLLED 事件(只关注内容变化和滚动)
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
        ) {
            //1.只过滤uu的包
            if (pkg == uuPackageName){
                Log.e("orderAccessibility", ">>>>> 当前切换到了 App: $packageName <<<<<")
                //2.更新悬浮窗样式
                //updateOverlayMode(pkg)

                //3.策略分发
                currentStrategy = strategies[pkg]
                currentStrategy?.onAccessibilityEvent(event, rootInActiveWindow)

            }else if(pkg==jdPackageName){
                //todo
            }
        }
        // 4. 点击捕获
        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            event.source?.let { currentStrategy?.onUserClick(it) }
        }

    }

    private fun updateOverlayMode(pkg: String) {
        val myPkg = packageName
        val mapApps = listOf("com.autonavi.minimap", "com.baidu.BaiduMap")

        when {
            pkg == myPkg -> GlobalState.currentMode = OverlayMode.HIDDEN
            strategies.containsKey(pkg) -> GlobalState.currentMode = OverlayMode.DECISION
            mapApps.contains(pkg) -> GlobalState.currentMode = OverlayMode.NAVIGATION
            else -> GlobalState.currentMode = OverlayMode.NAVIGATION // 默认显示导航(或待命)
        }
    }

    override fun onInterrupt() {}
}