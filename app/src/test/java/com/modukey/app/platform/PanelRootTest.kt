package com.modukey.app.platform

import android.content.Context
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.core.OutsideTouch
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[31,36])
class PanelRootTest {
    private val context get()=ApplicationProvider.getApplicationContext<Context>()
    private fun touch(root:PanelRoot,action:Int,x:Float,y:Float):Boolean {
        val event=MotionEvent.obtain(0,100,action,x,y,0)
        return try { root.dispatchTouchEvent(event) } finally { event.recycle() }
    }
    @Test fun `blocking consumes whole outside gesture and dismisses only on release`() {
        val root=PanelRoot(context) { false to OutsideTouch.BLOCK }; var dismissed=0
        root.dismiss={ dismissed++ }; root.layout(0,0,100,100)
        assertTrue(touch(root,MotionEvent.ACTION_DOWN,-10f,20f)); assertEquals(0,dismissed)
        assertTrue(touch(root,MotionEvent.ACTION_MOVE,20f,20f)); assertEquals(0,dismissed)
        assertTrue(touch(root,MotionEvent.ACTION_UP,20f,20f)); assertEquals(1,dismissed)
    }
    @Test fun `pass through observes outside down while preserving handle sequences`() {
        val root=PanelRoot(context) { false to OutsideTouch.PASS_THROUGH }; var dismissed=0
        root.dismiss={ dismissed++ }; root.layout(0,0,100,100)
        assertFalse(touch(root,MotionEvent.ACTION_OUTSIDE,150f,20f)); assertEquals(1,dismissed)
        root.isHandle={ _,_ -> true }
        assertFalse(touch(root,MotionEvent.ACTION_OUTSIDE,150f,20f)); assertEquals(1,dismissed)
    }
    @Test fun `inside drag ending outside is not an outside dismissal`() {
        val root=PanelRoot(context) { false to OutsideTouch.BLOCK }; var dismissed=0
        root.dismiss={ dismissed++ }; root.addView(View(context).apply { isClickable=true },FrameLayout.LayoutParams(-1,-1))
        root.measure(View.MeasureSpec.makeMeasureSpec(100,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(100,View.MeasureSpec.EXACTLY)); root.layout(0,0,100,100)
        touch(root,MotionEvent.ACTION_DOWN,10f,20f); touch(root,MotionEvent.ACTION_UP,-10f,20f); assertEquals(0,dismissed)
    }
    @Test fun `back is consumed once and editing passes touches through`() {
        var editing=false; val root=PanelRoot(context) { editing to OutsideTouch.BLOCK }; var dismissed=0; root.dismiss={ dismissed++ }
        assertTrue(root.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK)))
        assertTrue(root.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK))); assertEquals(1,dismissed)
        editing=true; assertFalse(touch(root,MotionEvent.ACTION_DOWN,10f,10f)); assertEquals(1,dismissed)
    }
}
