package com.modukey.app.ui

import android.view.WindowInsets
import android.view.WindowManager
import android.view.Display
import android.hardware.display.DisplayManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.modukey.app.core.Insets
import com.modukey.app.core.PreviewGeometry
import com.modukey.app.platform.ScreenSpace
import kotlin.math.max

@Composable internal fun rememberPreviewSpace(landscape: Boolean): ScreenSpace {
    val context = LocalContext.current; val configuration = LocalConfiguration.current; val density = LocalDensity.current.density
    val displays = remember(context) { context.getSystemService(DisplayManager::class.java) }
    var rotation by remember(context) { mutableIntStateOf(displays.getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: 0) }
    DisposableEffect(displays) {
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(id: Int) = Unit
            override fun onDisplayRemoved(id: Int) = Unit
            override fun onDisplayChanged(id: Int) { if (id == Display.DEFAULT_DISPLAY) rotation = displays.getDisplay(id)?.rotation ?: 0 }
        }
        displays.registerDisplayListener(listener, null)
        onDispose { displays.unregisterDisplayListener(listener) }
    }
    val metrics = remember(configuration, rotation) { context.getSystemService(WindowManager::class.java).currentWindowMetrics }
    val safe = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
    val nav = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.mandatorySystemGestures())
    val actual = Insets(safe.left.toFloat(), safe.top.toFloat(), safe.right.toFloat(), max(safe.bottom, nav.bottom).toFloat())
    val preview = PreviewGeometry.calculate(metrics.bounds.width().toFloat(), metrics.bounds.height().toFloat(), actual, rotation, landscape)
    return ScreenSpace(preview.width, preview.height, density, preview.insets, landscape)
}
