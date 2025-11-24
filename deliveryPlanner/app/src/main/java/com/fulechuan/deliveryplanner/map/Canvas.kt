package com.fulechuan.deliveryplanner.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.unit.dp
import com.fulechuan.deliveryplanner.enums.OrderStatus
import com.fulechuan.deliveryplanner.model.Order
import com.fulechuan.deliveryplanner.model.Point
import com.fulechuan.deliveryplanner.model.RouteNode
import com.fulechuan.deliveryplanner.ui.theme.BluePrimary
import com.fulechuan.deliveryplanner.ui.theme.DropoffColor
import com.fulechuan.deliveryplanner.ui.theme.MapGridColor
import com.fulechuan.deliveryplanner.ui.theme.PickupColor

// --- Map Component (Canvas) ---

@Composable
fun MapSimulationView(current: Point, orders: List<Order>, route: List<RouteNode>) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // Draw Grid
        val step = 20.dp.toPx()
        for (i in 0 until (w / step).toInt()) {
            drawLine(MapGridColor.copy(alpha = 0.5f), start = Offset(i * step, 0f), end = Offset(i * step, h), strokeWidth = 1f)
        }
        for (i in 0 until (h / step).toInt()) {
            drawLine(MapGridColor.copy(alpha = 0.5f), start = Offset(0f, i * step), end = Offset(w, i * step), strokeWidth = 1f)
        }

        // Draw Route Line
        if (route.isNotEmpty()) {
            val pathPoints = mutableListOf(Offset(current.x / 100 * w, current.y / 100 * h))
            route.forEach { task ->
                pathPoints.add(Offset(task.location.x / 100 * w, task.location.y / 100 * h))
            }
            for (i in 0 until pathPoints.size - 1) {
                drawLine(
                    color = BluePrimary,
                    start = pathPoints[i],
                    end = pathPoints[i + 1],
                    strokeWidth = 4f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f),
                    alpha = 0.6f
                )
            }
        }

        // Draw Points
        // Current Location
        drawCircle(BluePrimary, radius = 18f, center = Offset(current.x / 100 * w, current.y / 100 * h))
        drawCircle(Color.White, radius = 12f, center = Offset(current.x / 100 * w, current.y / 100 * h), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))

        orders.forEach { order ->
            if (order.status != OrderStatus.DELIVERED) {
                val pX: Float = order.pickupLoc.x / 100 * w
                val pY: Float = order.pickupLoc.y / 100 * h
                val dX: Float = order.deliveryLoc.x / 100 * w
                val dY: Float = order.deliveryLoc.y / 100 * h

                if (order.status == OrderStatus.NEW_OFFER) {
                    drawCircle(Color.White, radius = 20f, center = Offset(pX, pY))
                    drawCircle(PickupColor, radius = 20f, center = Offset(pX, pY), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
                }

                val dropColor = if (order.status == OrderStatus.PICKED_UP) DropoffColor else Color.Gray
                drawCircle(Color.White, radius = 20f, center = Offset(dX, dY))
                drawCircle(dropColor, radius = 20f, center = Offset(dX, dY), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
            }
        }
    }
}