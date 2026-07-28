package com.vpnpro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColors = darkColorScheme(
    primary          = AccentCyan,
    onPrimary        = BgDeep,
    secondary        = AccentBlue,
    onSecondary      = BgDeep,
    tertiary         = AccentGreen,
    background       = BgDark,
    onBackground     = OnSurface,
    surface          = Surface1,
    onSurface        = OnSurface,
    surfaceVariant   = Surface2,
    onSurfaceVariant = OnSurfaceVariant,
    error            = AccentRed,
    onError          = BgDeep,
    outline          = Surface3
)

@Composable
fun VpnProTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content     = content
    )
}
