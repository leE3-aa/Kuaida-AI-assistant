package com.kuaida.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.kuaida.app.navigation.QuickAnswerApp
import com.kuaida.app.ui.theme.QuickAnswerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QuickAnswerTheme {
                QuickAnswerApp()
            }
        }
    }
}
