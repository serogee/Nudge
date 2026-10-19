package com.modukey.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.modukey.app.R

/** Reserve the action width before measuring the title, including while edits are pending. */
@Composable fun EditorHeader(
    title: String, changed: Boolean, @StringRes renameLabel: Int,
    rename: () -> Unit, save: () -> Unit, cancel: () -> Unit, saving: Boolean = false,
    badge: @Composable () -> Unit,
) {
    Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("editor-header").padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        badge()
        Text(title, Modifier.weight(1f).testTag("editor-title"), style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
        Row {
            if (changed || saving) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Row(Modifier.padding(2.dp).testTag("editor-actions"), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilledTonalIconButton(onClick = cancel, enabled = !saving, modifier = Modifier.width(44.dp).height(36.dp),
                            shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp, topEnd = 8.dp, bottomEnd = 8.dp)) {
                            Icon(Icons.Outlined.Close, t(R.string.cancel))
                        }
                        FilledIconButton(onClick = save, enabled = !saving, modifier = Modifier.width(44.dp).height(36.dp),
                            shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp, topEnd = 24.dp, bottomEnd = 24.dp)) {
                            if (saving) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            else Icon(AppIcons.Check, t(R.string.save))
                        }
                    }
                }
                }
            } else {
                EditIcon(AppIcons.Edit, renameLabel, action = rename)
            }
        }
    }
}
