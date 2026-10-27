package dev.cogitor.notekeeper.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val NkShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(32.dp)
)

@Composable
fun NoteKeeperTheme(
    themeId: AppThemeId = AppThemeId.Cogitor,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = themeId.palette.toColorScheme(),
        typography = Typography,
        shapes = NkShapes,
        content = content
    )
}
