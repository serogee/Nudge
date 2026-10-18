package com.modukey.app.platform

import android.app.Activity
import android.os.Looper
import android.view.WindowInsets
import com.modukey.app.NudgeApplication
import com.modukey.app.core.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36])
class HandleSoftHideTest {
    @After fun cleanup() { ApplicationProvider.getApplicationContext<NudgeApplication>().onTerminate() }

    @Test fun `native handle reveals first suspends for its panel and clears on detach`() {
        val controller = Robolectric.buildActivity(Activity::class.java).setup()
        val activity = controller.get()
        var actions = 0; var reveals = 0
        val view = HandleTriggerView(activity, { actions++ }, { reveals++ })
        view.configure(TriggerSettings(softHideEnabled = true), Edge.RIGHT, false)
        activity.setContentView(view)
        shadowOf(Looper.getMainLooper()).idle()
        fun fullscreen() { view.dispatchApplyWindowInsets(WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), false).build()) }
        fullscreen()
        view.performClick(); assertEquals(0, actions); assertEquals(1, reveals)
        view.performClick(); assertEquals(1, actions)
        view.setPanelOpen(true)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(4))
        view.performClick(); assertEquals(2, actions)
        view.setPanelOpen(false)
        view.dispatchApplyWindowInsets(WindowInsets.Builder().setVisible(WindowInsets.Type.statusBars(), true).build())
        view.performClick(); assertEquals(3, actions)
        fullscreen(); view.resetSoftHide()
        view.performClick(); assertEquals(3, actions); assertEquals(2, reveals)
        view.configure(TriggerSettings(), Edge.RIGHT, false)
        view.performClick(); assertEquals(4, actions)
        controller.pause().stop().destroy()
    }
}
