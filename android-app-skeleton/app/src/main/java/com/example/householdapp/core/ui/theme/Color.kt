package com.example.householdapp.core.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val Teal = Color(0xFF0B6E6E)
val TealLight = Color(0xFF9EF1F1)
val TealDark = Color(0xFF005151)
val Gold = Color(0xFF7A5B00)
val GoldLight = Color(0xFFFFE08D)
val GoldDark = Color(0xFF564400)
val Coral = Color(0xFF964F00)
val CoralLight = Color(0xFFFFDCC2)
val CoralDark = Color(0xFF6D3A00)

val LightBackground = Color(0xFFFAFDFB)
val DarkBackground = Color(0xFF0F1515)

val LightSurfaceVariant = Color(0xFFDAE5E4)
val DarkSurfaceVariant = Color(0xFF3F4949)

val PriorityHigh = Color(0xFFC62828)
val PriorityMedium = Color(0xFFF9A825)
val PriorityLow = Color(0xFF2E7D32)

val LightColors = lightColorScheme(
    primary = Teal,
    onPrimary = Color.White,
    primaryContainer = TealLight,
    onPrimaryContainer = Color(0xFF002020),
    secondary = Gold,
    onSecondary = Color.White,
    secondaryContainer = GoldLight,
    onSecondaryContainer = Color(0xFF231B00),
    tertiary = Coral,
    onTertiary = Color.White,
    tertiaryContainer = CoralLight,
    onTertiaryContainer = Color(0xFF311500),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = LightBackground,
    onBackground = Color(0xFF191C1C),
    surface = LightBackground,
    onSurface = Color(0xFF191C1C),
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = Color(0xFF3F4949),
    outline = Color(0xFF6F7979),
    outlineVariant = Color(0xFFBEC9C8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF3F7F5),
    surfaceContainer = Color(0xFFEDF2F0),
    surfaceContainerHigh = Color(0xFFE8ECEA),
    surfaceContainerHighest = Color(0xFFE2E6E5)
)

val DarkColors = darkColorScheme(
    primary = Color(0xFF82D5D5),
    onPrimary = Color(0xFF003737),
    primaryContainer = TealDark,
    onPrimaryContainer = TealLight,
    secondary = Color(0xFFFFC14A),
    onSecondary = Color(0xFF3B2F00),
    secondaryContainer = GoldDark,
    onSecondaryContainer = GoldLight,
    tertiary = Color(0xFFFFB77C),
    onTertiary = Color(0xFF4A2800),
    tertiaryContainer = CoralDark,
    onTertiaryContainer = CoralLight,
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = DarkBackground,
    onBackground = Color(0xFFDFE4E3),
    surface = DarkBackground,
    onSurface = Color(0xFFDFE4E3),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFBEC9C8),
    outline = Color(0xFF889392),
    outlineVariant = DarkSurfaceVariant,
    surfaceContainerLowest = Color(0xFF0A100F),
    surfaceContainerLow = Color(0xFF171D1D),
    surfaceContainer = Color(0xFF1B2221),
    surfaceContainerHigh = Color(0xFF252C2C),
    surfaceContainerHighest = Color(0xFF303737)
)
