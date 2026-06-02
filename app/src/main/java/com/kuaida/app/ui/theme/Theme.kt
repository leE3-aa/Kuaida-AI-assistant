package com.kuaida.app.ui.theme

import androidx.compose.runtime.Composable

@Composable
fun QuickAnswerTheme(
    darkTheme: Boolean = androidx.compose.foundation.isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    com.kuaida.core.common.ui.theme.QuickAnswerTheme(
        darkTheme = darkTheme,
        content = content,
    )
}
