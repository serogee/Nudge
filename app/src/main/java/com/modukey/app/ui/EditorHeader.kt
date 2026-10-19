package com.modukey.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.modukey.app.R

/** Reserve the action width before measuring the title, including while edits are pending. */
@Composable fun EditorHeader(
    title: String, changed: Boolean, @StringRes renameLabel: Int,
    rename: () -> Unit, save: () -> Unit, cancel: () -> Unit,
    badge: @Composable () -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        badge()
        Text(title, Modifier.weight(1f).testTag("editor-title"), style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        Row {
            if (changed) {
                EditIcon(Icons.Outlined.Close, R.string.cancel, action = cancel)
                EditIcon(AppIcons.Check, R.string.save, action = save)
            } else EditIcon(AppIcons.Edit, renameLabel, action = rename)
        }
    }
}
