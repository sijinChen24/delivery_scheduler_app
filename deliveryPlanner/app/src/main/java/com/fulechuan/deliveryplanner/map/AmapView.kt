package com.fulechuan.deliveryplanner.map

import android.location.Location
import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.MapView
import com.amap.api.maps.model.MyLocationStyle



@Composable
fun AmapView(
    modifier: Modifier = Modifier,
    onLocationChanged:(Location) -> Unit
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    // 创建 MapView
    val mapView = remember {
        MapView(context)
    }
    val amap:AMap = mapView.map
    // 【新增】设置地图的默认缩放级别
    amap.moveCamera(CameraUpdateFactory.zoomTo(16f))
    //设置定位蓝点的Style
    val myLocationStyle = MyLocationStyle()
    myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_FOLLOW)
    amap.myLocationStyle = myLocationStyle
    amap.isMyLocationEnabled = true // 设置为true表示启动显示定位蓝点，false表示隐藏定位蓝点并不进行定位，默认是false。

    //获取定位
    amap.setOnMyLocationChangeListener { location ->
        // from AMapLocationListener
        if (location != null) {
            Log.d(
                "AmapView",
                "onMyLocationChange: " + location.latitude + ", " + location.longitude
            )
            // 通过回调函数将位置信息传递出去
            onLocationChanged(location)
        }
    }

    // 管理 MapView 的生命周期 (必须，否则黑屏)
    DisposableEffect(lifecycle, mapView) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                Lifecycle.Event.ON_START -> {}
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> {}
                Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycle.addObserver(lifecycleObserver)

        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
            mapView.onDestroy()
        }
    }

    // 显示到界面
    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}