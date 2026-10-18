package com.modukey.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.modukey.app.core.Edge
import com.modukey.app.core.HandleConfig

@Composable fun HandleBadge(handle: HandleConfig, name: String, modifier: Modifier = Modifier) {
    val shape = when(handle.trigger.portrait.edge) {
        Edge.LEFT -> RoundedCornerShape(topStart = 2.dp, bottomStart = 2.dp, topEnd = 16.dp, bottomEnd = 16.dp)
        Edge.RIGHT -> RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp, topEnd = 2.dp, bottomEnd = 2.dp)
        Edge.BOTTOM -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 2.dp)
    }
    val color = Color(handle.trigger.indicatorColor)
    Surface(modifier.size(44.dp), shape = shape, color = color, contentColor = contrastOn(color)) {
        Box(contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), style = MaterialTheme.typography.titleMedium) }
    }
}
