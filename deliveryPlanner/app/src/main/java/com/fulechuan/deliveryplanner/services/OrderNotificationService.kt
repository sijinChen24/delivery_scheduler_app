package com.fulechuan.deliveryplanner.services

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.fulechuan.deliveryplanner.R
import com.fulechuan.deliveryplanner.model.data.GlobalState

/**
 * 通知跳转
 */

class OrderNotificationService : NotificationListenerService() {

    private val uuPackageName by lazy {
        getString(R.string.uu_package_name)
    }

    private val jdPackageName by lazy {
        getString(R.string.jd_package_name)
    }
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val pkg = sbn?.packageName ?: return
        val extras = sbn.notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""


        if ((pkg.equals(uuPackageName) || pkg.equals(jdPackageName)) && title.contains("新订单")) {
            // 💡 关键：不要直接跳转，而是把这个动作存到 GlobalState
            // 这样如果用户正在看 MainActivity，UI 就能感知到
            GlobalState.pendingNotificationIntent = sbn.notification.contentIntent
            GlobalState.latestNotificationPackage = pkg

            Log.d("Notif", "捕获到新订单通知，已通知前台")
        }
    }
}