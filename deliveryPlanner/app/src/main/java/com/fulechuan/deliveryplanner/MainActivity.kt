package com.fulechuan.deliveryplanner

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fulechuan.deliveryplanner.enums.OrderStatus
import com.fulechuan.deliveryplanner.enums.PlatFormStatus
import com.fulechuan.deliveryplanner.enums.TaskType
import com.fulechuan.deliveryplanner.map.AmapView
import com.fulechuan.deliveryplanner.model.data.Order
import com.fulechuan.deliveryplanner.model.data.Point
import com.fulechuan.deliveryplanner.ui.theme.BgGray
import com.fulechuan.deliveryplanner.ui.theme.BlueLight
import com.fulechuan.deliveryplanner.ui.theme.BluePrimary
import com.fulechuan.deliveryplanner.ui.theme.DeliveryPlannerTheme
import com.fulechuan.deliveryplanner.ui.theme.DropoffColor
import com.fulechuan.deliveryplanner.ui.theme.PickupColor
import com.fulechuan.deliveryplanner.ui.theme.UrgentBg
import com.fulechuan.deliveryplanner.ui.theme.UrgentColor
import com.fulechuan.deliveryplanner.viewModel.DeliveryViewModel
import com.fulechuan.deliveryplanner.viewModel.factory.DeliveryViewModelFactory
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fulechuan.deliveryplanner.model.data.GlobalState
import com.fulechuan.deliveryplanner.model.data.OrderNode
import com.fulechuan.deliveryplanner.model.data.RouteManager
import kotlinx.coroutines.flow.toList

// ================= UI 界面 (Compose) =================

class MainActivity : ComponentActivity() {

    //使用ViewModelFactory通过委托的方式创建viewModel
    private val viewModel: DeliveryViewModel by viewModels {
        DeliveryViewModelFactory(application)
    }

    // [NEW] 运行时权限请求
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            ) {
                // 权限被授予，可以继续
            } else {
                Toast.makeText(this, "定位权限被拒绝，无法显示地图", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // [NEW] 请求权限
        requestLocationPermissions()

        // ⚡️ 启动时恢复数据
        RouteManager.getInstance(applicationContext).restoreState()

        setContent {
            DeliveryPlannerTheme {
                DeliveryPlannerScreen(viewModel)
            }
        }
    }


    private fun requestLocationPermissions() {
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }


}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryPlannerScreen(viewModel: DeliveryViewModel) {


    val routeList = viewModel.currentRoute.toList()

    var activeOrders = viewModel.activeOrders.collectAsStateWithLifecycle(
        initialValue = emptyList()
    )

//    var currentLoc by viewModel.currentLoc
    var log by viewModel.logInfo

    // 获取当前的 Context
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Navigation,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            context.getString(R.string.title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BluePrimary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    // AI Button
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFA855F7), Color(0xFF6366F1))
                                ),
                                shape = CircleShape
                            )
                            .size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = "AI",
                            tint = Color(0xFFFEF08A),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    // Add Button
                    IconButton(
                        onClick = { },
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            )
        }, bottomBar = {
            Surface(shadowElevation = 8.dp, color = Color.White) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("待处理任务: ${routeList.size}", color = Color.Gray, fontSize = 14.sp)
                    Text(
                        "预计耗时: ${routeList.size * 15} 分钟",
                        color = Color.Gray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .background(BgGray)
        ) {
            // 1. Map Simulation Area
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .background(Color(0xFFE2E8F0))
            ) {
                //MapSimulationView(currentLoc,orderList , routeList)
                AmapView(
                    modifier = Modifier.fillMaxSize(),
                    onLocationChanged = { location -> viewModel.updateCurrentLocation(location) }
                )
            }

            // 2. Task List
            Column(
                Modifier
                    .weight(1f)
                    .background(BlueLight.copy(alpha = 0.3f))
            ) {
                Text(
                    "建议执行顺序",
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF374151)
                )

                if (routeList.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                null,
                                tint = Color.Gray,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                log,
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                        itemsIndexed(routeList) { index, task ->
                            TaskItem(
                                task = task,
                                isFirst = index == 0,
                                isLast = index == routeList.lastIndex,
                                onComplete = {
                                    viewModel.completeTask(task)
                                }
                            )
                        }
                    }
                }
            }

            // 3. 新订单雷达提醒 (顶层覆盖)
            // 监听 GlobalState 的变化
            val intent = GlobalState.pendingNotificationIntent
            val pkg = GlobalState.latestNotificationPackage

            if (intent != null && pkg != null) {
                NewOrderAlertCard(
                    packageName = pkg,
                    onJump = {
                        try {
                            // 执行通知自带的跳转 (最稳，直接跳到订单详情或列表)
                            intent.send()
                        } catch (e: Exception) {
                            // 兜底：直接拉起 App
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                            context.startActivity(launchIntent)
                        }
                        // 清空状态，隐藏弹窗
                        GlobalState.pendingNotificationIntent = null
                    },
                    onIgnore = {
                        GlobalState.pendingNotificationIntent = null
                    }
                )
            }
        }

    }
}

@Composable
fun NewOrderAlertCard(packageName: String, onJump: () -> Unit, onIgnore: () -> Unit) {
    val appName = if (packageName.contains("paotui")) "UU跑腿" else "京东外卖"
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {

        Card(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 50.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE65100)), // 醒目的橙色
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text("🔥 $appName 来新单了!", fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "点击立即跳转抢单",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                Button(
                    onClick = onJump,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("去看看", color = Color(0xFFE65100))
                }

                IconButton(onClick = onIgnore) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = Color.White)
                }
            }
        }
    }
}

@Composable
fun IncomingOrderDialog(
    order: Order,
    isFeasible: Boolean,
    onAccept: () -> Unit,
    onReject: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "🔥 新订单提醒",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("商家", color = Color.Gray)
                        Text(order.shopName, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("收入", color = Color.Gray)
                        Text(
                            "¥${order.price}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider()
                Spacer(modifier = Modifier.height(10.dp))

                Text("送至: ${order.pickupLoc}")

                Spacer(modifier = Modifier.height(20.dp))

                // 智能分析结果
                if (isFeasible) {
                    Container(color = Color(0xFFE8F5E9), padding = 8) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "顺路单！加入后不超时",
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Container(color = Color(0xFFFFEBEE), padding = 8) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFC62828)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "不建议接单：会导致原单超时",
                                color = Color(0xFFC62828),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    OutlinedButton(onClick = onReject, modifier = Modifier.weight(1f)) {
                        Text("残忍拒绝")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = onAccept,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isFeasible) MaterialTheme.colorScheme.primary else Color.Gray
                        ),
                        enabled = isFeasible // 如果不顺路，禁止接单（或者可以强制接单，看需求）
                    ) {
                        Text("立即抢单")
                    }
                }
            }
        }
    }
}

@Composable
fun Container(color: Color, padding: Int, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color)
            .padding(padding.dp)
    ) {
        content()
    }
}

@Composable
fun TaskItem(task: OrderNode, isFirst: Boolean, isLast: Boolean, onComplete: () -> Unit) {
    val timeLeft = (task.deadline - System.currentTimeMillis()) / 60000 //转化为分钟
    val isUrgent = timeLeft < 10
    val typeColor = if (task.type == TaskType.PICKUP) PickupColor else DropoffColor

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // Timeline line
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(typeColor, CircleShape)
                    .border(1.dp, Color.White, CircleShape)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(80.dp) // approximate height
                        .background(Color.LightGray)
                )
            }
        }

        // Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, bottom = 12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = typeColor,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                if (task.type == TaskType.PICKUP) "取餐" else "送达",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text("#${task.orderId}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (isUrgent) {
                            Spacer(Modifier.width(8.dp))
                            Surface(color = UrgentBg, shape = RoundedCornerShape(4.dp)) {
                                Row(
                                    Modifier.padding(horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        null,
                                        tint = UrgentColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text("加急", fontSize = 10.sp, color = UrgentColor)
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(task.name, fontSize = 14.sp, color = Color.DarkGray)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Schedule,
                            null,
                            tint = Color.Gray,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "剩余: ${if (timeLeft < 0) "已超时" else "${timeLeft}分钟"}",
                            fontSize = 12.sp,
                            color = if (isUrgent) DropoffColor else Color.Gray,
                            fontWeight = if (isUrgent) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                IconButton(
                    onClick = onComplete,
                    colors = IconButtonDefaults.iconButtonColors(containerColor = BgGray),
                    enabled = isFirst
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        null,
                        tint = if (isFirst) Color.Green else Color.Gray
                    )
                }
            }
        }
    }
}


