package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Full Material3 color-role fill-in, warm-neutral instead of Material's default cool gray, so every
// surface reads as part of the same "Granary" palette instead of clashing with it. Dynamic color
// (Material You, wallpaper-derived) is deliberately not used — a bar's branded app shouldn't look
// different depending on whose phone it's opened on.
private val LightColorScheme = lightColorScheme(
    primary = Terracotta40,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCB),
    onPrimaryContainer = Color(0xFF3A0900),
    secondary = Ochre40,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF5DFA6),
    onSecondaryContainer = Color(0xFF2B1D00),
    tertiary = Millet40,
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC4EFB6),
    onTertiaryContainer = Color(0xFF032100),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFFFFBF7),
    onBackground = Color(0xFF21190F),
    surface = Color(0xFFFFFBF7),
    onSurface = Color(0xFF21190F),
    surfaceVariant = Color(0xFFF1E0D0),
    onSurfaceVariant = Color(0xFF504539),
    outline = Color(0xFF837568),
    outlineVariant = Color(0xFFD5C4B4),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF362F27),
    inverseOnSurface = Color(0xFFFBEEE4),
    inversePrimary = Terracotta80,
    surfaceTint = Terracotta40,
)

private val DarkColorScheme = darkColorScheme(
    primary = Terracotta80,
    onPrimary = Color(0xFF5F1600),
    primaryContainer = Color(0xFF7F2D0C),
    onPrimaryContainer = Color(0xFFFFDBCB),
    secondary = Ochre80,
    onSecondary = Color(0xFF452D00),
    secondaryContainer = Color(0xFF614300),
    onSecondaryContainer = Color(0xFFF5DFA6),
    tertiary = Millet80,
    onTertiary = Color(0xFF103907),
    tertiaryContainer = Color(0xFF25511C),
    onTertiaryContainer = Color(0xFFC4EFB6),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF17120D),
    onBackground = Color(0xFFEDE0D4),
    surface = Color(0xFF17120D),
    onSurface = Color(0xFFEDE0D4),
    surfaceVariant = Color(0xFF504539),
    onSurfaceVariant = Color(0xFFD5C4B4),
    outline = Color(0xFF9E8F80),
    outlineVariant = Color(0xFF504539),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFEDE0D4),
    inverseOnSurface = Color(0xFF362F27),
    inversePrimary = Terracotta40,
    surfaceTint = Terracotta80,
)

// Slightly softer, more generous rounding than Material3's defaults (4/8/12/16/28dp) — a common
// visual cue for a premium, considered feel over a stock/generic one.
private val GranaryShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = GranaryShapes,
        content = content,
    )
}
