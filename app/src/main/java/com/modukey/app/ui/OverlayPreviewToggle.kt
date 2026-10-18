package com.modukey.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.modukey.app.R

@Composable internal fun OverlayPreviewToggle(checked: Boolean, connected: Boolean, change: (Boolean) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).clickable(enabled = connected, role = Role.Switch) { change(!checked) },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(t(R.string.preview), Modifier.weight(1f))
            Switch(checked, change, enabled = connected, modifier = Modifier.semanticsDescription(t(R.string.overlay_preview)))
        }
        if (!connected) Help(R.string.preview_unavailable)
    }
}
