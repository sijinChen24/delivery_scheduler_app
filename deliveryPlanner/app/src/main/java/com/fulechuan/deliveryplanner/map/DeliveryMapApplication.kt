package com.fulechuan.deliveryplanner.map

import android.app.Application
import android.content.Context
import com.amap.api.location.AMapLocationClient
import com.amap.api.maps.MapsInitializer

/**
 * 隐私政策
 */
class DeliveryMapApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // 【关键】高德隐私合规接口，必须在初始化之前调用
        val context: Context = this
        MapsInitializer.updatePrivacyShow(context, true, true)
        MapsInitializer.updatePrivacyAgree(context, true)

    }
}