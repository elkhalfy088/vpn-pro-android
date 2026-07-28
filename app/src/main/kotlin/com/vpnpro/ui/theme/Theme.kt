package com.vpnpro.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary          = AccentCyan,
    onPrimary        = Color(0xFF001F2E),
    secondary        = AccentGreen,
    onSecondary      = Color(0xFF00210B),
    tertiary         = AccentYellow,
    background       = Surface0,
    onBackground     = OnSurface,
    surface          = Surface1,
    onSurface        = OnSurface,
    surfaceVariant   = Surface2,
    onSurfaceVariant = OnSurfaceVariant,
    outline          = Surface3,
    error            = AccentRed,
)

@Composable
fun VpnProTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography(),
        content     = content
    )
}
