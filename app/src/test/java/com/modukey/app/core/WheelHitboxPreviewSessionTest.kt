package com.modukey.app.core

import org.junit.Assert.*
import org.junit.Test

class WheelHitboxPreviewSessionTest {
    @Test fun onlyTheCurrentEditorAndOrientationCanShowTheirHitbox() {
        val session = EditorSession(); val token = Any()
        assertNull(session.showHitbox("wheel", false))
        session.begin(token, EditorSelection(panelId = "wheel", landscape = false))
        assertNull(session.showHitbox("other", false))
        assertNull(session.showHitbox("wheel", true))
        assertNotNull(session.showHitbox("wheel", false))
        session.begin(token, EditorSelection(panelId = "wheel", landscape = true))
        assertNull(session.hitboxPreview.value)
    }

    @Test fun anOldTimerOrEditorCannotRemoveTheReplacementPreview() {
        val session = EditorSession(); val oldOwner = Any(); val newOwner = Any()
        session.begin(oldOwner, EditorSelection(panelId = "wheel"))
        val old = session.showHitbox("wheel", false)!!
        session.begin(newOwner, EditorSelection(panelId = "wheel"))
        val current = session.showHitbox("wheel", false)!!
        session.clearHitbox(old); session.end(oldOwner)
        assertEquals(current, session.hitboxPreview.value)
        session.clearHitbox(current)
        assertNull(session.hitboxPreview.value)
        session.showHitbox("wheel", false)
        session.end(newOwner)
        assertNull(session.hitboxPreview.value)
    }
}
