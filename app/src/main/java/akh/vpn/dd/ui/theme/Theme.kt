package akh.vpn.dd.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Dark scheme — deep navy/teal VPN aesthetic
private val DarkColors = darkColorScheme(
    primary          = Color(0xFF00E5B0),
    onPrimary        = Color(0xFF003328),
    primaryContainer = Color(0xFF005140),
    onPrimaryContainer = Color(0xFF7FF8D4),
    secondary        = Color(0xFF4FC3F7),
    onSecondary      = Color(0xFF003548),
    background       = Color(0xFF0A0E1A),
    onBackground     = Color(0xFFE2E8F0),
    surface          = Color(0xFF111827),
    onSurface        = Color(0xFFE2E8F0),
    surfaceVariant   = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline          = Color(0xFF334155),
    error            = Color(0xFFFF6B6B),
    onError          = Color(0xFF2D0000),
)

// Light scheme (less-used; dark is default for VPN apps)
private val LightColors = lightColorScheme(
    primary          = Color(0xFF007A5E),
    onPrimary        = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF7FF8D4),
    onPrimaryContainer = Color(0xFF00201A),
    secondary        = Color(0xFF0277BD),
    onSecondary      = Color(0xFFFFFFFF),
    background       = Color(0xFFF0F4F8),
    onBackground     = Color(0xFF0F172A),
    surface          = Color(0xFFFFFFFF),
    onSurface        = Color(0xFF0F172A),
    surfaceVariant   = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF475569),
    outline          = Color(0xFFCBD5E1),
)

@Composable
fun VpnProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography(),
        content     = content
    )
}
