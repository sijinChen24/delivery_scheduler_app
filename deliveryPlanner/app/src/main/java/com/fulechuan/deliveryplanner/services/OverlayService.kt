package com.fulechuan.deliveryplanner.services

import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.fulechuan.deliveryplanner.enums.OverlayMode
import com.fulechuan.deliveryplanner.model.data.GlobalState


/**
 * 悬浮窗 UI
 */
class OverlayService : LifecycleService(), SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private lateinit var composeView: ComposeView
    private lateinit var savedStateRegistryController: SavedStateRegistryController

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry


    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // 创建 ComposeView
        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@OverlayService)
            setViewTreeSavedStateRegistryOwner(this@OverlayService)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP

        composeView.setContent {
            OverlayContent()
        }

        windowManager.addView(composeView, params)
    }

    @Composable
    fun OverlayContent() {
        val mode = GlobalState.currentMode
        if (mode == OverlayMode.HIDDEN) return

        Card(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF212121).copy(alpha = 0.9f))
        ) {
            when (mode) {
                OverlayMode.DECISION -> DecisionView()
                OverlayMode.NAVIGATION -> NavigationView()
                OverlayMode.COLD_START -> ColdStartView()
                else -> {}
            }
        }
    }

    @Composable
    fun Card(
        modifier: Modifier,
        shape: RoundedCornerShape,
        colors: CardColors,
        content: @Composable () -> Unit
    ) {
        //TODO("Not yet implemented")
    }

    @Composable
    fun DecisionView() {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("💡 接单建议 (UU)", color = Color.White, fontSize = 12.sp)
            GlobalState.acceptedList.forEach { order ->

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {

                    //Text("${order.shopName} + ¥${order.price}", color = Color.White)
                    ///Text(
                        //if(suggestionResult.isFeasible) "可接" else "不能接",  color = Color.Green)
                    //Text("原因:${suggestionResult.reason}",  color = Color.Green)
                }
                HorizontalDivider(color = Color.Gray, thickness = 0.5.dp)
            }
            if (GlobalState.acceptedList.isEmpty() && GlobalState.refusedList.isEmpty()) {
                Text("正在扫描...", color = Color.Gray)
            }
        }
    }

    @Composable
    fun NavigationView() {
        //获取节点列表的第一个元素
        //todo 当点击完成后,剔除第一个元素,再重新调用这个方法,直到节点全部剔除
        val next = GlobalState.plannedRoute.firstOrNull()
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                if(next != null){
                    Text("🚀 当前任务(剩余${if(GlobalState.plannedRoute.size-1<0) 0 else GlobalState.plannedRoute.size}个节点)", color = Color.Gray, fontSize = 10.sp)
                    Text(next.name, color = Color.White, fontSize = 16.sp)
                }else{
                    Text("当前无任务 - 等待接单", color = Color.Green)
                }
                //todo 三个按钮,一个导航,一个查看,一个完成
                Button(onClick = { /* 打开调高德地图,当前位置为起点,任务地址为终点 */ }, modifier = Modifier.height(36.dp)) {
                    Text("导航", fontSize = 12.sp)
                }
                Button(onClick = { /* 打开主APP */ }, modifier = Modifier.height(36.dp)) {
                    Text("查看", fontSize = 12.sp)
                }
                Button(onClick = { /* 剔除当前节点 */ }, modifier = Modifier.height(36.dp)) {
                    Text("完成", fontSize = 12.sp)
                }
            }

        }
    }


    @Composable
    fun ColdStartView() {
        Column(modifier = Modifier.padding(10.dp)) {
            Text("🚀 当前空闲，为你推荐首单", fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))

            // 遍历评分后的列表
            GlobalState.scoredCandidates.take(3).forEach { item ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C2C)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 左侧：分数
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(40.dp).background(Color(item.recommendColor), CircleShape)
                        ) {
                            Text("${item.totalScore}", color = Color.White, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // 中间：详情
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.order.shopName, color = Color.White, maxLines = 1)

                            // 显示标签
                            Row {
                                item.tags.forEach { tag ->
                                    Text(
                                        text = tag,
                                        fontSize = 10.sp,
                                        color = Color(0xFF81D4FA),
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                            }

                            // 显示核心指标
                            Text(
                                text = "距我${item.distanceVal.toInt()}米 | 时薪≈¥${item.hourlyRateVal.toInt()}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        // 右侧：价格
                        Text("¥${item.priceVal}", color = Color(0xFFFFC107), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::composeView.isInitialized) windowManager.removeView(composeView)
    }
}