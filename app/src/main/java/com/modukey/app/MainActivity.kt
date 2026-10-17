package com.modukey.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.ViewModelProvider
import com.modukey.app.ui.NudgeApp
import com.modukey.app.ui.NudgeViewModel

class MainActivity : ComponentActivity() {
    private val model by viewModels<NudgeViewModel> { ViewModelProvider.AndroidViewModelFactory(application) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NudgeApp(model) }
    }
}
