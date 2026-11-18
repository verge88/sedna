package com.sedna.words

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sedna.words.ui.SednaRoot
import com.sedna.words.ui.SednaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val model: SednaViewModel = viewModel()
            SednaTheme(model.darkMode) {
                SideEffect {
                    val bars = WindowCompat.getInsetsController(window, window.decorView)
                    bars.isAppearanceLightStatusBars = !model.darkMode
                    bars.isAppearanceLightNavigationBars = !model.darkMode
                }
                SednaRoot(model)
            }
        }
    }
}
