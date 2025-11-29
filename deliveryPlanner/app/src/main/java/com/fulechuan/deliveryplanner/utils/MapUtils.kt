package com.fulechuan.deliveryplanner.utils


import android.content.Context
import android.util.Log
import com.amap.api.location.AMapLocationClient
import com.amap.api.location.AMapLocationClientOption
import com.amap.api.location.AMapLocationListener
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.fulechuan.deliveryplanner.R
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object MapUtils {


    /**
     * 地址转坐标 (Geocoding)
     * @param address 详细地址 (如 "深圳市南山区肯德基(科技园店)")
     * @param city 城市 (可选，填了更准，如 "深圳")
     * @return Pair<Double, Double>?  (Latitude纬度, Longitude经度)
     */
    suspend fun getCoordinate(mapKey:String,address: String, city: String = ""): Pair<Double, Double>? {
        //简单的内存缓存,防止同一个地址重复请求高德API，节省配额且速度快
        val locationCache = HashMap<String, Pair<Double, Double>>()
        val client = OkHttpClient()
        // 1. 先查缓存
        if (locationCache.containsKey(address)) {
            Log.d("MapUtils", "⚡️ 命中缓存: $address")
            return locationCache[address]
        }

        // 2. 拼接 URL
        // 高德文档: https://restapi.amap.com/v3/geocode/geo?parameters
        val url = "https://restapi.amap.com/v3/geocode/geo?address=$address&city=$city&key=$mapKey"

        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val jsonStr = response.body?.string()

                if (jsonStr != null) {
                    val jsonObj = JSONObject(jsonStr)
                    val status = jsonObj.optString("status") // "1" 代表成功

                    if (status == "1") {
                        val geocodes = jsonObj.optJSONArray("geocodes")
                        if (geocodes != null && geocodes.length() > 0) {
                            val firstResult = geocodes.getJSONObject(0)
                            // 高德返回格式是 "经度,纬度" (lng,lat)
                            val locationStr = firstResult.optString("location")
                            val parts = locationStr.split(",")

                            if (parts.size == 2) {
                                val lng = parts[0].toDouble() // 经度
                                val lat = parts[1].toDouble() // 纬度

                                val result = Pair(lat, lng)
                                // 3. 存入缓存
                                locationCache[address] = result
                                Log.d("MapUtils", "🌍 解析成功: $address -> $lat, $lng")
                                return@withContext result
                            }
                        }
                    } else {
                        Log.e("MapUtils", "解析失败: $jsonStr")
                    }
                }
            } catch (e: Exception) {
                Log.e("MapUtils", "网络请求异常: ${e.message}")
            }
            return@withContext null
        }
    }


    /**
     * 获取单次高精度定位 (挂起函数)
     * @return Pair(纬度, 经度) 或 null
     */
    suspend fun getCurrentLocation(context: Context): Pair<Double, Double>? {

        // 使用 suspendCancellableCoroutine 将回调转为协程
        return suspendCancellableCoroutine { continuation ->

            // 1. 初始化 Client
            val locationClient = AMapLocationClient(context.applicationContext)

            // 2. 配置参数
            val option = AMapLocationClientOption().apply {
                locationMode = AMapLocationClientOption.AMapLocationMode.Hight_Accuracy // 高精度模式
                isOnceLocation = true // 关键：只定位一次，拿到结果就停止
                isOnceLocationLatest = true // 获取最近3秒内精度最高的一次
                httpTimeOut = 5000 // 超时时间 5秒
            }
            locationClient.setLocationOption(option)

            // 3. 设置监听器
            val listener = AMapLocationListener { location ->
                if (location != null && location.errorCode == 0) {
                    // 定位成功
                    Log.d("AMap", "定位成功: ${location.latitude}, ${location.longitude}")
                    // 恢复协程，返回结果
                    if (continuation.isActive) {
                        continuation.resume(Pair(location.latitude, location.longitude))
                    }
                } else {
                    // 定位失败
                    Log.e(
                        "AMap",
                        "定位失败: ErrCode=${location?.errorCode} Info=${location?.errorInfo}"
                    )
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }

                // 拿到结果后，销毁 Client
                locationClient.stopLocation()
                locationClient.onDestroy()
            }

            locationClient.setLocationListener(listener)

            // 4. 启动定位
            locationClient.startLocation()

            // 5. 处理协程取消 (如果外部取消了任务，这里也要停止定位)
            continuation.invokeOnCancellation {
                locationClient.stopLocation()
                locationClient.onDestroy()
            }
        }
    }
}