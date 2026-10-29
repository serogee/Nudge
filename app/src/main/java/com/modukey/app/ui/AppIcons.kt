package com.modukey.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import com.modukey.app.core.BuiltInIcon

/** Small static vectors share one style across navigation, editing, and the icon library. */
object AppIcons {
    private fun icon(name: String, autoMirror: Boolean = false, draw: PathBuilder.() -> Unit) = ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f, autoMirror = autoMirror).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round, pathBuilder = draw)
    }.build()
    val Wheel = icon("Wheel") {
        moveTo(2f,21f); curveTo(2f,7f,22f,7f,22f,21f); close()
        moveTo(12f,21f); lineTo(5f,13f); moveTo(12f,21f); lineTo(12f,10.5f); moveTo(12f,21f); lineTo(19f,13f)
    }
    val Back = icon("Back") { moveTo(19f,12f); lineTo(5f,12f); moveTo(11f,5f); lineTo(4f,12f); lineTo(11f,19f) }
    val Edit = icon("Edit") { moveTo(4f,16f); lineTo(15f,5f); lineTo(19f,9f); lineTo(8f,20f); lineTo(3f,21f); close(); moveTo(14f,6f); lineTo(18f,10f) }
    val Delete = icon("Delete") { moveTo(4f,6f); lineTo(20f,6f); moveTo(9f,6f); lineTo(9f,3f); lineTo(15f,3f); lineTo(15f,6f); moveTo(6f,6f); lineTo(7f,21f); lineTo(17f,21f); lineTo(18f,6f); moveTo(10f,10f); lineTo(10f,17f); moveTo(14f,10f); lineTo(14f,17f) }
    val Duplicate = icon("Duplicate") { moveTo(9f,8f); lineTo(21f,8f); lineTo(21f,21f); lineTo(9f,21f); close(); moveTo(15f,4f); lineTo(3f,4f); lineTo(3f,17f); moveTo(12f,14f); lineTo(18f,14f); moveTo(15f,11f); lineTo(15f,17f) }
    val Reset = icon("Reset") { moveTo(4f,10f); curveTo(5f,1f,20f,1f,20f,12f); curveTo(20f,21f,7f,23f,4f,15f); moveTo(4f,4f); lineTo(4f,10f); lineTo(10f,10f) }
    val Handle = icon("Handle") { moveTo(6f,3f); lineTo(18f,3f); curveTo(20f,3f,21f,4f,21f,6f); lineTo(21f,18f); curveTo(21f,20f,20f,21f,18f,21f); lineTo(6f,21f); curveTo(4f,21f,3f,20f,3f,18f); lineTo(3f,6f); curveTo(3f,4f,4f,3f,6f,3f); moveTo(18f,9f); lineTo(18f,15f) }
    val HandleVisibility = icon("Handle visibility") {
        moveTo(2f,12f); curveTo(7f,3f,17f,3f,22f,12f); curveTo(17f,21f,7f,21f,2f,12f); close()
        moveTo(15f,12f); curveTo(15f,16f,9f,16f,9f,12f); curveTo(9f,8f,15f,8f,15f,12f)
    }
    val HandleToggleAndHide = icon("Toggle and hide handles") {
        moveTo(8f,3f); lineTo(18f,3f); curveTo(20f,3f,21f,4f,21f,6f); lineTo(21f,16f)
        moveTo(16f,21f); lineTo(6f,21f); curveTo(4f,21f,3f,20f,3f,18f); lineTo(3f,8f)
        moveTo(18f,9f); lineTo(18f,14f)
        moveTo(3f,3f); lineTo(21f,21f)
    }
    val Drag = icon("Drag") { for(x in listOf(9f,15f)) for(y in listOf(5f,12f,19f)) { moveTo(x,y); lineTo(x+.1f,y) } }
    val ChevronUp = icon("Collapse") { moveTo(6f,15f); lineTo(12f,9f); lineTo(18f,15f) }
    val Down = icon("Expand") { moveTo(6f,9f); lineTo(12f,15f); lineTo(18f,9f) }
    val Add = icon("Add") { moveTo(4f,12f); lineTo(20f,12f); moveTo(12f,4f); lineTo(12f,20f) }
    val Minus = icon("Minus") { moveTo(5f,12f); lineTo(19f,12f) }
    val Notifications = icon("Notifications") {
        moveTo(5f,17f); lineTo(7f,14f); lineTo(7f,9f); curveTo(7f,2f,17f,2f,17f,9f)
        lineTo(17f,14f); lineTo(19f,17f); close(); moveTo(10f,21f); lineTo(14f,21f)
    }
    val ControlPanel = icon("Control panel") {
        moveTo(3f,6f); lineTo(8f,6f); moveTo(13f,6f); lineTo(21f,6f); moveTo(8f,3f); lineTo(8f,9f); lineTo(13f,9f); lineTo(13f,3f); close()
        moveTo(3f,18f); lineTo(12f,18f); moveTo(17f,18f); lineTo(21f,18f); moveTo(12f,15f); lineTo(12f,21f); lineTo(17f,21f); lineTo(17f,15f); close()
    }
    val BrightnessAutomatic = icon("Automatic brightness") {
        moveTo(3f,9f); lineTo(3f,3f); lineTo(9f,3f); moveTo(15f,3f); lineTo(21f,3f); lineTo(21f,9f)
        moveTo(21f,15f); lineTo(21f,21f); lineTo(15f,21f); moveTo(9f,21f); lineTo(3f,21f); lineTo(3f,15f)
        moveTo(8f,17f); lineTo(12f,7f); lineTo(16f,17f); moveTo(10f,13f); lineTo(14f,13f)
    }
    val BrightnessMaximum = icon("Maximum brightness") {
        moveTo(17f,12f); curveTo(17f,19f,7f,19f,7f,12f); curveTo(7f,5f,17f,5f,17f,12f)
        moveTo(12f,2f); lineTo(12f,4f); moveTo(12f,20f); lineTo(12f,22f)
        moveTo(2f,12f); lineTo(4f,12f); moveTo(20f,12f); lineTo(22f,12f)
        moveTo(5f,5f); lineTo(6.5f,6.5f); moveTo(17.5f,17.5f); lineTo(19f,19f)
        moveTo(5f,19f); lineTo(6.5f,17.5f); moveTo(17.5f,6.5f); lineTo(19f,5f)
    }
    val BrightnessMinimum = icon("Minimum brightness") {
        moveTo(16f,12f); curveTo(16f,17.5f,8f,17.5f,8f,12f); curveTo(8f,6.5f,16f,6.5f,16f,12f)
        moveTo(12f,3f); lineTo(12f,4f); moveTo(12f,20f); lineTo(12f,21f)
        moveTo(3f,12f); lineTo(4f,12f); moveTo(20f,12f); lineTo(21f,12f)
    }
    val BrightnessUp = icon("Increase brightness") {
        moveTo(13f,11f); curveTo(13f,17f,5f,17f,5f,11f); curveTo(5f,5f,13f,5f,13f,11f)
        moveTo(9f,2f); lineTo(9f,4f); moveTo(9f,18f); lineTo(9f,20f); moveTo(1f,11f); lineTo(3f,11f)
        moveTo(16f,15f); lineTo(22f,15f); moveTo(19f,12f); lineTo(19f,18f)
    }
    val BrightnessDown = icon("Decrease brightness") {
        moveTo(13f,11f); curveTo(13f,17f,5f,17f,5f,11f); curveTo(5f,5f,13f,5f,13f,11f)
        moveTo(9f,2f); lineTo(9f,4f); moveTo(9f,18f); lineTo(9f,20f); moveTo(1f,11f); lineTo(3f,11f)
        moveTo(16f,15f); lineTo(22f,15f)
    }
    val ClosePanels = icon("Close panels") {
        moveTo(6f,3f); lineTo(21f,3f); lineTo(21f,18f)
        moveTo(3f,6f); lineTo(18f,6f); lineTo(18f,21f); lineTo(3f,21f); close()
        moveTo(7f,10f); lineTo(14f,17f); moveTo(14f,10f); lineTo(7f,17f)
    }
    val Screenshot = icon("Screenshot") {
        moveTo(3f,8f); lineTo(3f,3f); lineTo(8f,3f); moveTo(16f,3f); lineTo(21f,3f); lineTo(21f,8f)
        moveTo(21f,16f); lineTo(21f,21f); lineTo(16f,21f); moveTo(8f,21f); lineTo(3f,21f); lineTo(3f,16f)
        moveTo(7f,8f); lineTo(17f,8f); lineTo(17f,16f); lineTo(7f,16f); close()
    }
    val Check = icon("Check") { moveTo(4f,12f); lineTo(9f,17f); lineTo(20f,6f) }
    val Open = icon("Open") { moveTo(10f,4f); lineTo(20f,4f); lineTo(20f,14f); moveTo(20f,4f); lineTo(9f,15f); moveTo(5f,7f); lineTo(3f,7f); lineTo(3f,21f); lineTo(17f,21f); lineTo(17f,19f) }
    val Camera = icon("Camera") { moveTo(3f,7f); lineTo(7f,7f); lineTo(9f,4f); lineTo(15f,4f); lineTo(17f,7f); lineTo(21f,7f); lineTo(21f,20f); lineTo(3f,20f); close(); moveTo(16f,13f); curveTo(16f,18f,8f,18f,8f,13f); curveTo(8f,8f,16f,8f,16f,13f) }
    val Wallet = icon("Wallet") { moveTo(20f,7f); lineTo(3f,7f); lineTo(3f,20f); lineTo(21f,20f); lineTo(21f,7f); moveTo(3f,7f); lineTo(3f,4f); lineTo(18f,4f); lineTo(18f,7f); moveTo(21f,11f); lineTo(15f,11f); lineTo(15f,16f); lineTo(21f,16f); moveTo(18f,13.5f); lineTo(18.1f,13.5f) }
    val Phone = icon("Phone") { moveTo(5f,3f); lineTo(9f,3f); lineTo(10f,8f); lineTo(7f,10f); curveTo(9f,14f,11f,16f,15f,17f); lineTo(17f,14f); lineTo(22f,16f); lineTo(21f,20f); curveTo(8f,24f,0f,8f,5f,3f); close() }
    val Flashlight = icon("Flashlight") { moveTo(7f,3f); lineTo(17f,3f); lineTo(17f,8f); lineTo(14f,12f); lineTo(14f,21f); lineTo(10f,21f); lineTo(10f,12f); lineTo(7f,8f); close(); moveTo(7f,7f); lineTo(17f,7f); moveTo(12f,13f); lineTo(12f,15f) }
    val Music = icon("Music") { moveTo(9f,18f); lineTo(9f,5f); lineTo(20f,3f); lineTo(20f,16f); moveTo(9f,8f); lineTo(20f,6f); moveTo(9f,18f); curveTo(9f,23f,2f,23f,2f,19f); curveTo(2f,16f,9f,15f,9f,18f); moveTo(20f,16f); curveTo(20f,21f,13f,21f,13f,17f); curveTo(13f,14f,20f,13f,20f,16f) }
    val Headphones = icon("Headphones") { moveTo(4f,16f); lineTo(4f,11f); curveTo(4f,0f,20f,0f,20f,11f); lineTo(20f,16f); moveTo(4f,12f); lineTo(8f,12f); lineTo(8f,21f); lineTo(4f,21f); close(); moveTo(20f,12f); lineTo(16f,12f); lineTo(16f,21f); lineTo(20f,21f); close() }
    val Calendar = icon("Calendar") { moveTo(3f,6f); lineTo(21f,6f); lineTo(21f,21f); lineTo(3f,21f); close(); moveTo(7f,3f); lineTo(7f,8f); moveTo(17f,3f); lineTo(17f,8f); moveTo(3f,11f); lineTo(21f,11f); moveTo(7f,15f); lineTo(10f,15f); moveTo(14f,15f); lineTo(17f,15f) }
    val Clock = icon("Clock") { moveTo(21f,12f); curveTo(21f,24f,3f,24f,3f,12f); curveTo(3f,0f,21f,0f,21f,12f); moveTo(12f,6f); lineTo(12f,12f); lineTo(16f,15f) }
    val Map = icon("Map") { moveTo(3f,6f); lineTo(9f,3f); lineTo(15f,6f); lineTo(21f,3f); lineTo(21f,18f); lineTo(15f,21f); lineTo(9f,18f); lineTo(3f,21f); close(); moveTo(9f,3f); lineTo(9f,18f); moveTo(15f,6f); lineTo(15f,21f) }
    val Home = icon("Home") { moveTo(2f,11f); lineTo(12f,3f); lineTo(22f,11f); moveTo(5f,9f); lineTo(5f,21f); lineTo(10f,21f); lineTo(10f,14f); lineTo(14f,14f); lineTo(14f,21f); lineTo(19f,21f); lineTo(19f,9f) }
    val Favorite = icon("Favorite") { moveTo(12f,3f); lineTo(15f,9f); lineTo(22f,10f); lineTo(17f,15f); lineTo(18f,22f); lineTo(12f,18f); lineTo(6f,22f); lineTo(7f,15f); lineTo(2f,10f); lineTo(9f,9f); close() }
    val Apps = icon("Apps") { for(x in listOf(3f,14f)) for(y in listOf(3f,14f)) { moveTo(x,y); lineTo(x+7,y); lineTo(x+7,y+7); lineTo(x,y+7); close() } }
    // Sample 1, translated into a tight viewport. The library mark has no background and accepts the panel tint.
    val Nudge = ImageVector.Builder("Nudge", 24.dp, 24.dp, 64f, 64f).apply {
        path(fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 14f, strokeLineCap = StrokeCap.Round) {
            moveTo(12f, 33.5f); lineTo(12f, 53f)
            moveTo(15f, 9f); lineTo(52f, 56f)
            moveTo(52f, 7.7f); lineTo(52f, 27.7f)
        }
    }.build()
    val Empty = icon("Empty button") { moveTo(21f,12f); curveTo(21f,24f,3f,24f,3f,12f); curveTo(3f,0f,21f,0f,21f,12f); moveTo(6f,6f); lineTo(18f,18f) }
    val Keyboard = icon("Keyboard") {
        moveTo(2f,5f); lineTo(22f,5f); lineTo(22f,19f); lineTo(2f,19f); close()
        for (y in listOf(9f,12f)) for (x in listOf(6f,10f,14f,18f)) { moveTo(x,y); lineTo(x+.1f,y) }
        moveTo(7f,16f); lineTo(17f,16f)
    }
    fun library(icon: BuiltInIcon) = when(icon) {
        BuiltInIcon.CAMERA -> Camera; BuiltInIcon.WALLET -> Wallet; BuiltInIcon.PHONE -> Phone; BuiltInIcon.FLASHLIGHT -> Flashlight
        BuiltInIcon.MUSIC -> Music; BuiltInIcon.HEADPHONES -> Headphones; BuiltInIcon.CALENDAR -> Calendar; BuiltInIcon.CLOCK -> Clock
        BuiltInIcon.MAP -> Map; BuiltInIcon.HOME -> Home; BuiltInIcon.FAVORITE -> Favorite; BuiltInIcon.APPS -> Apps
        BuiltInIcon.NUDGE -> Nudge
        BuiltInIcon.HANDLE -> Handle; BuiltInIcon.HANDLE_VISIBILITY -> HandleVisibility
        BuiltInIcon.TOGGLE_AND_HIDE_HANDLES -> HandleToggleAndHide
    }
}
