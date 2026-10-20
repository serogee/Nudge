package com.modukey.app.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.modukey.app.R

@Composable fun DurationInput(@StringRes title: Int, value: Long, range: LongRange, default: Long, change: (Long) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toString()) }
    var focused by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val finish = LocalFinishAdjustment.current
    val name = t(title)
    val number = text.toLongOrNull()
    fun commit() {
        val bounded = (text.toLongOrNull() ?: value).coerceIn(range)
        text = bounded.toString()
        if (bounded != value) change(bounded)
        finish()
    }
    Column {
        SettingTitle(name, reset = if (value != default) ({ text = default.toString(); change(default) }) else null)
        OutlinedTextField(value = text, onValueChange = { input ->
            if (input.all { it in '0'..'9' }) {
                text = input
                input.toLongOrNull()?.takeIf { it in range }?.let(change)
            }
        }, singleLine = true, suffix = { Text(t(R.string.milliseconds_unit)) },
            supportingText = { Text(t(R.string.duration_range, range.first, range.last)) },
            isError = number == null || number !in range,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
            modifier = Modifier.fillMaxWidth().semanticsDescription(name).onFocusChanged {
                if (focused && !it.isFocused) commit()
                focused = it.isFocused
            })
    }
}
