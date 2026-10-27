package dev.cogitor.notekeeper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color(0xFF061018),
    primaryContainer = Color(0xFF243056),
    onPrimaryContainer = Color(0xFFDCE4FF),
    secondary = Warm,
    onSecondary = Color(0xFF1A1204),
    tertiary = Color(0xFF8BE0C4),
    background = Ink,
    onBackground = Color(0xFFE7EDF6),
    surface = Ink,
    onSurface = Color(0xFFE7EDF6),
    surfaceVariant = InkElevated,
    onSurfaceVariant = Color(0xFFB4C0D1),
    outline = Color(0xFF3E4A5C),
    error = Danger
)

private val LightColors = lightColorScheme(
    primary = AccentDim,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCE4FF),
    onPrimaryContainer = Color(0xFF0B1A40),
    secondary = Color(0xFF8A6418),
    background = Paper,
    onBackground = Color(0xFF121820),
    surface = Paper,
    onSurface = Color(0xFF121820),
    surfaceVariant = Mist,
    onSurfaceVariant = Color(0xFF445062),
    outline = Color(0xFFC5CEDA),
    error = Color(0xFFB3261E)
)

private val NkShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun NoteKeeperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = NkShapes,
        content = content
    )
}
