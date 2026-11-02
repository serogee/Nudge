package com.modukey.app.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import com.modukey.app.NudgeApplication
import com.modukey.app.core.PanelButton
import com.modukey.app.platform.ShortcutIconRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Show exactly the bitmap passed to Android, including its padding and original app colors. */
@Composable internal fun ShortcutGraphic(button: PanelButton, size: Dp) {
    val context = LocalContext.current
    val app = context.applicationContext as NudgeApplication
    val image by produceState<ImageBitmap?>(null, button) {
        value = withContext(Dispatchers.Default) { ShortcutIconRenderer.render(context, app.icons, button).asImageBitmap() }
    }
    if (image == null) Box(Modifier.size(size))
    else Image(image!!, context.buttonLabel(button), Modifier.size(size))
}
