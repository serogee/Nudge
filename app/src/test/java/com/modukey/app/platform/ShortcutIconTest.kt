package com.modukey.app.platform

import android.graphics.Color
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.graphics.drawable.AdaptiveIconDrawable
import androidx.test.core.app.ApplicationProvider
import com.modukey.app.NudgeApplication
import com.modukey.app.core.*
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.util.ReflectionHelpers

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31, 36], qualifiers = "mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ShortcutIconTest {
    private val app get() = ApplicationProvider.getApplicationContext<NudgeApplication>()
    @After fun cleanup() { app.onTerminate() }
    @Test fun pinAgainRefreshesExistingArtworkAndKeepsTheSavedId() = runBlocking {
        val manager = app.getSystemService(ShortcutManager::class.java)
        shadowOf(manager).setIsRequestPinShortcutSupported(true)
        val shortcuts = PinnedShortcuts(app, app.icons)
        val original = HomeShortcut(button = PanelButton(iconContainer = IconContainer.CONTAINER))
        assertTrue(shortcuts.pin(original, "Original"))
        val updated = original.copy(button = original.button.copy(iconContainer = IconContainer.STANDALONE))
        assertTrue(shortcuts.pin(updated, "Updated"))
        val pinned = manager.pinnedShortcuts.single()
        assertEquals(original.id, pinned.id)
        assertEquals("Updated", pinned.shortLabel)
        val icon = ReflectionHelpers.callInstanceMethod<Icon>(pinned, "getIcon")
        assertEquals(Icon.TYPE_ADAPTIVE_BITMAP, icon.type)
        assertNull((icon.loadDrawable(app) as AdaptiveIconDrawable).background)
        val bitmap = shadowOf(icon).bitmap
        assertEquals(Color.TRANSPARENT, bitmap.getPixel(bitmap.width / 2, 10))
    }
    @Test fun pinnedArtworkStaysInsideTheAdaptiveSafeAreaWithoutAnOpaqueWrapper() = runBlocking {
        val manager = app.getSystemService(ShortcutManager::class.java)
        shadowOf(manager).setIsRequestPinShortcutSupported(true)
        for (container in IconContainer.entries) {
            val shortcut = HomeShortcut(button = PanelButton(icon = IconReference(builtIn = BuiltInIcon.FAVORITE), iconContainer = container))
            assertTrue(PinnedShortcuts(app, app.icons).pin(shortcut, "Favorite"))
            val pinned = manager.pinnedShortcuts.first { it.id == shortcut.id }
            val icon = ReflectionHelpers.callInstanceMethod<Icon>(pinned, "getIcon")
            assertEquals(Icon.TYPE_ADAPTIVE_BITMAP, icon.type)
            assertNull((icon.loadDrawable(app) as AdaptiveIconDrawable).background)
            val bitmap = shadowOf(icon).bitmap
            val center = bitmap.width / 2f
            val safeRadius = bitmap.width * 66f / 108f / 2f
            for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
                if (Color.alpha(bitmap.getPixel(x, y)) > 0)
                    assertTrue("$container $x,$y", kotlin.math.hypot(x + .5f - center, y + .5f - center) <= safeRadius)
            }
        }
    }
    @Test fun defaultAndStandaloneArtworkHaveNoAddedBackgroundWhileExplicitContainersRemainVisible() = runBlocking {
        val buttons = listOf(PanelButton(action = ButtonAction.VOLUME_UP),
            PanelButton(icon = IconReference(builtIn = BuiltInIcon.NUDGE)),
            PanelButton(icon = IconReference(app.packageName, "ic_modukey")))
        for (button in buttons) {
            val default = ShortcutIconRenderer.render(app, app.icons, button)
            val standalone = ShortcutIconRenderer.render(app, app.icons, button.copy(iconContainer = IconContainer.STANDALONE))
            val container = ShortcutIconRenderer.render(app, app.icons, button.copy(iconContainer = IconContainer.CONTAINER))
            try {
                assertTrue(default.sameAs(standalone))
                if (button.icon.packageName.isBlank()) assertEquals(Color.TRANSPARENT, default.getPixel(default.width / 2, 10))
                val pixels = IntArray(default.width * default.height)
                default.getPixels(pixels, 0, default.width, 0, 0, default.width, default.height)
                assertFalse(pixels.any { it == 0xFF3C3C3E.toInt() })
                assertEquals(0xFF3C3C3E.toInt(), container.getPixel(container.width / 2, 10))
                for (bitmap in listOf(default, standalone, container)) {
                    for (i in 0 until bitmap.width) {
                        assertEquals(Color.TRANSPARENT, bitmap.getPixel(i, 0))
                        assertEquals(Color.TRANSPARENT, bitmap.getPixel(i, bitmap.height - 1))
                        assertEquals(Color.TRANSPARENT, bitmap.getPixel(0, i))
                        assertEquals(Color.TRANSPARENT, bitmap.getPixel(bitmap.width - 1, i))
                    }
                }
            } finally { default.recycle(); standalone.recycle(); container.recycle() }
        }
    }
    @Test fun externalArtworkHasSmoothTransparentCornersWithoutStrayEdgePixels() = runBlocking {
        val bitmap = ShortcutIconRenderer.render(app, app.icons, PanelButton(icon = IconReference(app.packageName, "ic_modukey")))
        try {
            val inset = bitmap.width * .08f
            val radius = (bitmap.width - inset * 2) * .25f
            val center = inset + radius
            var partial = 0
            for (y in 0 until center.toInt()) for (x in 0 until center.toInt()) {
                val distance = kotlin.math.hypot(x + .5f - center, y + .5f - center)
                for (px in listOf(x, bitmap.width - 1 - x)) for (py in listOf(y, bitmap.height - 1 - y)) {
                    val alpha = Color.alpha(bitmap.getPixel(px, py))
                    if (distance > radius + 1f) assertEquals("$px,$py", 0, alpha)
                    if (alpha in 1..254) partial++
                }
            }
            assertTrue("The rounded edge must have antialiased pixels", partial > 0)
        } finally { bitmap.recycle() }
    }
    @Test fun everyStaticActionAndLibraryIconProducesPaddedArtwork() = runBlocking {
        val buttons = ButtonAction.entries.filter { it != ButtonAction.EMPTY }.map { PanelButton(action = it) } +
            BuiltInIcon.entries.map { PanelButton(icon = IconReference(builtIn = it), iconContainer = IconContainer.STANDALONE) }
        for (button in buttons) {
            val bitmap = ShortcutIconRenderer.render(app, app.icons, button)
            assertEquals(Color.TRANSPARENT, bitmap.getPixel(0, 0))
            val pixels = IntArray(bitmap.width * bitmap.height); bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
            assertTrue(button.toString(), pixels.any { Color.alpha(it) > 0 })
            if (button.icon.builtIn != null || button.action != ButtonAction.NUDGE_SETTINGS)
                assertTrue(button.toString(), pixels.any { Color.red(it) > 220 && Color.green(it) > 220 && Color.alpha(it) > 200 })
            bitmap.recycle()
        }
    }
    @Test fun handleActionsHaveDistinctArtworkAndMatchTheirLibraryIcons() = runBlocking {
        val pairs = listOf(ButtonAction.TOGGLE_HANDLES to BuiltInIcon.HANDLE,
            ButtonAction.TOGGLE_HANDLE_VISIBILITY to BuiltInIcon.HANDLE_VISIBILITY,
            ButtonAction.TOGGLE_AND_HIDE_HANDLES to BuiltInIcon.TOGGLE_AND_HIDE_HANDLES)
        val rendered = pairs.map { (action, _) -> ShortcutIconRenderer.render(app, app.icons, PanelButton(action = action)) }
        try {
            for (i in rendered.indices) {
                for (j in 0 until i) assertFalse("Handle actions must have different artwork", rendered[i].sameAs(rendered[j]))
                val library = ShortcutIconRenderer.render(app, app.icons, PanelButton(icon = IconReference(builtIn = pairs[i].second)))
                try { assertTrue("The picker must use the action artwork", rendered[i].sameAs(library)) }
                finally { library.recycle() }
            }
        } finally { rendered.forEach { it.recycle() } }
    }
}
