package com.fulechuan.deliveryplanner.strategies

import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.graphics.GraphicsLayerScope
import com.fulechuan.deliveryplanner.R
import com.fulechuan.deliveryplanner.enums.OrderStatus
import com.fulechuan.deliveryplanner.enums.OverlayMode
import com.fulechuan.deliveryplanner.enums.PlatFormStatus
import com.fulechuan.deliveryplanner.model.data.GlobalState
import com.fulechuan.deliveryplanner.model.data.Order
import com.fulechuan.deliveryplanner.model.data.Point
import com.fulechuan.deliveryplanner.model.data.RouteManager
import com.fulechuan.deliveryplanner.model.data.SuggestionResult
import com.fulechuan.deliveryplanner.utils.dateStringToLong
import com.fulechuan.deliveryplanner.utils.parseDeadline
import com.fulechuan.deliveryplanner.utils.toNowTimeString
import com.fulechuan.deliveryplanner.utils.toNowTimeString
import com.fulechuan.deliveryplanner.utils.toTimeString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.String

/**
 * 解析数据,封装订单,通过SuggestionEngine 发送给viewModel去处理
 */
class UUStrategy(private val context: Context) : PlatformStrategy {

    // 扫描到的订单列表交给routeManager
    private val routeManager = RouteManager.getInstance(context)


    override val targetPackage: String by lazy {
        context.getString(R.string.uu_package_name)
    }

    private val id_prefix: String = targetPackage + ":id/"




    // 防线1:上次扫描时间 (用于简单的节流，防止1秒扫100次)
    private var lastScanTime = 0L
    private val SCAN_INTERVAL = 5000L // 至少间隔5秒扫一次

    // 防线2：上次扫描结果的指纹 (Hash)
    private var lastDataHash = 0


    /**
     * 当检测到用户进入新任务列表才执行扫描
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent, rootNode: AccessibilityNodeInfo?) {

        // 1. 【核心过滤器】判断是否在“新任务”界面
        // 如果不在，直接 return，停止后续所有操作
        if (!isNewTaskTab(rootNode)) {
            Log.d("UU_Accessibility", "当前不在新任务Tab，停止扫描")
            // 也可以选择在这里隐藏悬浮窗的建议列表
            GlobalState.acceptedList.clear()
            GlobalState.refusedList.clear()
            return
        }

        Log.d("UU_Accessibility", ">>> 过滤器通过，开始扫描列表 <<<")

        // 3. 【防线1】时间节流
        val now = System.currentTimeMillis()
        if (now - lastScanTime < SCAN_INTERVAL) {
            return // 还没到时间，忽略这次事件
        }
        lastScanTime = now
        //4. 扫描列表
        val parseOrders = scanList(rootNode)

        if (parseOrders.isEmpty()) {
            Log.d("UU_Accessibility", ">>> 扫描列表为空 <<<")
            return
        }

        //5. 调用routeManager 获取接单建议
        routeManager.analyzeCandidate(parseOrders)
    }


    /**
     *用户点击抢单
     */
    override fun onUserClick(node: AccessibilityNodeInfo) {
        val text = node.text?.toString() ?: ""
        Log.d("UUStrategy", "Click detected: $text")

//        // todo 场景1: 点击列表里的 "抢单"
//        if (text.contains("抢单") && !text.contains("确认")) {
//            // 模拟：找到该按钮对应的订单信息
//            // 真实开发中需要 node.parent.parent... findById
//            val order = Order(
//                id = "uu_${System.currentTimeMillis()}",
//                shopName = "",
//                price = 10.0,
//                pickupAddress = "",
//                deliveryAddress = "",
//                pickupLoc = Point(0F, 0F),
//                deliveryLoc = Point(0F, 0F),
//                pickupDeadline = "",
//                deliveryDeadline = "",
//                status = OrderStatus.NEW_OFFER,
//                createTime = toTimeString(),
//                finishTime = null,
//                platForm = PlatFormStatus.UU,
//                orderType = ""
//            )
//
//            tempCandidate = order
//            Log.d("UUStrategy", "进入暂存态: ${order.id}")
//        }
//        // todo 场景2: 点击弹窗里的 "确认抢单"
//        else if (text.contains("确认") || text.contains("立即接单")) {
//            tempCandidate?.let { order ->
//                // 1.转正！
//                val acceptedOrder = order.copy(status = OrderStatus.ACCEPTED)
//                //2.通过routeManager 将订单入库并重新规划路线
//                //todo 如果抢单失败,要回滚
//                routeManager.confirmOrder(acceptedOrder)
//
//            }
//        }
    }


    /**
     * 🕵️‍♀️ 过滤器逻辑：判断当前页面是否是“新任务/接单大厅”
     */
    private fun isNewTaskTab(root: AccessibilityNodeInfo?): Boolean {
        // 方案 A：检查底部 Tab 按钮的选中状态 (最标准，但有的App不支持)
        // 1. 找到所有包含“新任务”文字的节点
        val tabNodes = root?.findAccessibilityNodeInfosByText("新任务")
        if (tabNodes != null) {
            for (node in tabNodes) {
                // 检查这个节点，或者它的父节点（有时候文字被包裹在按钮里）是否被选中
                // 只有当它是 Visible 且 Selected 时，才说明当前确实停在这个 Tab
                if (node.isSelected || node.parent?.isSelected == true) {
                    Log.d("UU_FILTER", "检测到顶部Tab被选中: 新任务")
                    return true
                }
            }
        }
        return false
    }


    /**
     * 🚀 步骤 1: 扫描列表入口
     */
    private fun scanList(root: AccessibilityNodeInfo?): List<Order> {
        val foundOrders = mutableListOf<Order>()
        // 1. 直接通过 ID 找列表，这是最准的
        // 日志显示列表ID是 "android:id/list"
        val listNodes = root?.findAccessibilityNodeInfosByViewId("android:id/list")
        val listNode = if (listNodes?.isNotEmpty() == true) {
            listNodes[0]
        } else {
            Log.d("UU_SCAN", "==============没有扫描到任务列表viewId================")
            return emptyList()
        }

        // 2. 遍历列表子项
        for (i in 0 until listNode.childCount) {
            val cardNode = listNode.getChild(i)
            if (cardNode != null && cardNode.isVisibleToUser) {
                //根据viewId精准解析
                val order = parseOrderCardById(cardNode)
                if (order != null) {
                    foundOrders.add(order)
                }
            }
        }
        if (foundOrders.isNotEmpty()) {
            // 防线2】数据去重 (核心)
            // 算出当前这批订单的“指纹”
            // 逻辑：把所有订单ID拼起来算个 Hash，如果ID没变，说明列表没变
            val currentHash = foundOrders.joinToString { it.id }.hashCode()
            if (currentHash == lastDataHash) {
                // 数据完全没变，静默退出！不要打印日志！
                return foundOrders
            }
            // 数据变了 (有新订单，或者旧订单消失了)
            lastDataHash = currentHash
            Log.d("UU_SCAN", "✅ 扫描完成，更新了 ${foundOrders.size} 个订单")
            foundOrders.forEach {
                Log.d("UU_SCAN", "   -> ${it.shopName} ¥${it.price}")
            }
        }
        return emptyList()
    }

    /**
     * 🚀 步骤 2: 基于 ID 精准解析 (新版)
     */
    private fun parseOrderCardById(card: AccessibilityNodeInfo): Order? {
        // 使用辅助函数 findTextById 快速提取
        // 1. 提取价格 (必选)
        val priceText = findTextById(card, "${id_prefix}feight_money") // "5.85元"
        if (priceText.isEmpty()) return null // 没价格肯定不是订单

        val price = priceText.replace("元", "").toDoubleOrNull() ?: 0.0

        //2.提取送达时间order_start_time 如65分钟内(18:35前)送达

        var deliveryDeadlineText = findTextById(card, "${id_prefix}order_start_time")
        if (deliveryDeadlineText.isEmpty()) return null
        deliveryDeadlineText = parseDeadline(deliveryDeadlineText)

        //3. 提取类型
        val type = findTextById(card, "${id_prefix}tv_order_type") // "帮送"

        // 4. 提取取货信息
        // 因为 start_addr 和 end_addr 里的子 View ID 是一样的 (都叫 tv_first_address)
        // 所以我们需要先找到容器，再从容器里找子 View
        val startContainer = findChildById(card, "${id_prefix}start_addr")
        val endContainer = findChildById(card, "${id_prefix}end_addr")

        // 提取取货地址1 (tv_first_address商家店名)
        val shopName = findTextById(startContainer, "${id_prefix}tv_first_address")

        // 提取取货地址2 (tv_second_address商家地址)
        val pickupAddress = findTextById(startContainer, "${id_prefix}tv_second_address")

        // 提取取货距离 ("1.4\nkm" -> "1.4km")
        val pickupDist = findTextById(startContainer, "${id_prefix}tv_distance").replace("\n", "")
            .replace(" ", "").toDouble()

        // 提取客户名称
        val customName = findTextById(endContainer, "${id_prefix}tv_first_address")

        //提取送货地址 ("和谐苑小区...")
        var deliveryAddr = findTextById(endContainer, "${id_prefix}tv_second_address")
        if (deliveryAddr.isEmpty()) {
            deliveryAddr = customName
        }

        // 提取送货距离
        val deliveryDist =
            findTextById(endContainer, "${id_prefix}tv_distance").replace("\n", "").replace(" ", "")

        // 5. 组装数据
        // todo 逻辑：假设需要 15分钟基础缓冲(下楼/停车/等餐) + 每公里骑行3分钟(后续注意对比真实时间差异)
        val now = System.currentTimeMillis()
        val estimatedMinutes = 15 + (pickupDist * 3)
        val estimatedPickupTime = now + (estimatedMinutes * 60 * 1000).toLong()

        return Order(
            id = (System.currentTimeMillis()).hashCode().toString(),
            shopName = shopName,
            customName = customName,
            pickupAddress = pickupAddress,
            pickupLoc = Point(0.0, 0.0),
            deliveryAddress = deliveryAddr,
            deliveryLoc = Point(0.0, 0.0),
            price = price,
            pickupDeadlineText = toTimeString(estimatedPickupTime),
            deliveryDeadlineText = deliveryDeadlineText,
            pickupDeadline = estimatedPickupTime, //这个字段只有点击抢单后才能看到
            deliveryDeadline = dateStringToLong(deliveryDeadlineText),
            pickupToDeliveryDistance = deliveryDist.toDouble(),
            status = OrderStatus.NEW_OFFER,
            createTime = toNowTimeString(),
            finishTime = "", //todo 订单完成时记得更新此值
            platForm = PlatFormStatus.UU,
            orderType = type
        )
    }


// ================== 辅助工具 ==================

    // 在节点下根据 ID 查找文字
    private fun findTextById(root: AccessibilityNodeInfo?, resId: String): String {
        if (root == null) return ""
        val nodes = root.findAccessibilityNodeInfosByViewId(resId)
        if (nodes.isNotEmpty()) {
            return nodes[0].text?.toString() ?: ""
        }
        return ""
    }

    // 在节点下根据 ID 查找子节点 (用于定位容器)
    private fun findChildById(root: AccessibilityNodeInfo?, resId: String): AccessibilityNodeInfo? {
        if (root == null) return null
        val nodes = root.findAccessibilityNodeInfosByViewId(resId)
        if (nodes.isNotEmpty()) {
            return nodes[0]
        }
        return null
    }




}

// 放在 UUStrategy 类里面或者外面都可以
private fun printNodeTree(node: AccessibilityNodeInfo?, depth: Int = 0) {
    if (node == null) return
    val indent = "  ".repeat(depth)

    // 打印最关键的三个信息：类名、文字、ID
    // 重点关注：Text (用来定位内容) 和 ViewId (用来定位控件)
    Log.d(
        "UU_DEBUG",
        "$indent Class=${node.className} Text='${node.text}' ID='${node.viewIdResourceName}' Clickable=${node.isClickable}" + "Visible=${node.isVisibleToUser} Selected=${node.isSelected}\""
    )


    for (i in 0 until node.childCount) {
        printNodeTree(node.getChild(i), depth + 1)
    }
}
