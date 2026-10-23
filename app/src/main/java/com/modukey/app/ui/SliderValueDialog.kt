package com.modukey.app.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import com.modukey.app.R
import kotlin.math.roundToInt

@Composable internal fun SliderValueDialog(name: String, value: Float, range: ClosedFloatingPointRange<Float>,
    valueScale: Float, dismiss: () -> Unit, save: (Float) -> Unit) {
    var input by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        val text = (value.coerceIn(range) * valueScale).roundToInt().toString()
        mutableStateOf(TextFieldValue(text, TextRange(0, text.length)))
    }
    val focus = remember { FocusRequester() }
    val number = input.text.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }
    fun commit() {
        number?.let { save((it / valueScale).coerceIn(range.start.toDouble(), range.endInclusive.toDouble()).toFloat()) }
    }
    AlertDialog(onDismissRequest = dismiss, title = { Text(name) }, text = {
        OutlinedTextField(value = input, onValueChange = { input = it }, singleLine = true,
            isError = number == null && input.text.isNotEmpty(),
            keyboardOptions = KeyboardOptions(keyboardType = if (range.start < 0f) KeyboardType.Ascii else KeyboardType.Decimal,
                imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { commit() }),
            modifier = Modifier.fillMaxWidth().focusRequester(focus).semanticsDescription(name))
        LaunchedEffect(Unit) { focus.requestFocus() }
    }, confirmButton = { TextButton(onClick = { commit() }, enabled = number != null) { Text(t(R.string.save)) } },
        dismissButton = { TextButton(onClick = dismiss) { Text(t(R.string.cancel)) } })
}
