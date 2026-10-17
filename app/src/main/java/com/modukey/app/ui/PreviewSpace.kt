package com.modukey.app.ui

import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.modukey.app.core.Insets
import com.modukey.app.platform.ScreenSpace
import kotlin.math.max
import kotlin.math.min

@Composable internal fun rememberPreviewSpace(landscape: Boolean): ScreenSpace {
    val context = LocalContext.current; val configuration = LocalConfiguration.current; val density = LocalDensity.current.density
    val metrics = remember(configuration) { context.getSystemService(WindowManager::class.java).currentWindowMetrics }
    val short = min(metrics.bounds.width(), metrics.bounds.height()).toFloat(); val long = max(metrics.bounds.width(), metrics.bounds.height()).toFloat()
    val safe = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
    val nav = metrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.mandatorySystemGestures())
    val insets = if ((metrics.bounds.width() > metrics.bounds.height()) == landscape) Insets(safe.left.toFloat(), safe.top.toFloat(), safe.right.toFloat(), max(safe.bottom, nav.bottom).toFloat())
        else Insets(top = 28*density, bottom = 24*density)
    return ScreenSpace(if (landscape) long else short, if (landscape) short else long, density, insets, landscape)
}
