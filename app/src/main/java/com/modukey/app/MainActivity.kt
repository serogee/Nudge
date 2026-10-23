package com.modukey.app

import android.os.Bundle
import android.animation.ValueAnimator
import android.window.SplashScreenView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import com.modukey.app.ui.NudgeApp
import com.modukey.app.ui.NudgeViewModel
import java.time.Duration
import java.time.Instant

class MainActivity : ComponentActivity() {
    private val model by viewModels<NudgeViewModel> { ViewModelProvider.AndroidViewModelFactory(application) }
    private var launchView: SplashScreenView? = null
    private val finishLaunch = Runnable { removeLaunch() }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        splashScreen.setOnExitAnimationListener { splash ->
            removeLaunch(); launchView = splash
            val duration = splash.iconAnimationDuration?.toMillis() ?: 0L
            val elapsed = splash.iconAnimationStart?.let { Duration.between(it, Instant.now()).toMillis() } ?: duration
            val remaining = if (ValueAnimator.areAnimatorsEnabled()) (duration - elapsed).coerceIn(0L, 600L) else 0L
            if (remaining == 0L) removeLaunch() else splash.postDelayed(finishLaunch, remaining)
        }
        enableEdgeToEdge()
        setContent { NudgeApp(model) }
    }
    private fun removeLaunch() { val view = launchView; launchView = null; view?.removeCallbacks(finishLaunch); view?.remove() }
    override fun onStop() { removeLaunch(); super.onStop() }
    override fun onDestroy() {
        if (isFinishing && !isChangingConfigurations) {
            val calibration = (application as NudgeApplication).automaticCalibration
            if (calibration.state.value.busy) calibration.cancel() else calibration.discard()
        }
        removeLaunch(); super.onDestroy()
    }
}
