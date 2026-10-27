package dev.cogitor.notekeeper.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance

enum class AppThemeId(val id: String, val label: String) {
    Cogitor("cogitor", "Cogitor"),
    Light("light", "Light"),
    Dark("dark", "Dark"),
    Green("green", "Green"),
    Cyan("cyan", "Cyan"),
    Blue("blue", "Blue"),
    Purple("purple", "Purple"),
    Darcula("darcula", "Darcula"),
    Rose("rose", "Rose"),
    Amber("amber", "Amber"),
    Teal("teal", "Teal"),
    Indigo("indigo", "Indigo"),
    Slate("slate", "Slate");

    companion object {
        fun from(id: String?) = entries.find { it.id == id } ?: Cogitor
    }
}

data class AppPalette(
    val primary: Color,
    val secondary: Color,
    val background: Color,
    val surface: Color,
    val text: Color,
    val textSecondary: Color,
    val border: Color,
    val hover: Color,
    val dark: Boolean,
    val tertiary: Color? = null
)

private fun hex(value: String) = Color(android.graphics.Color.parseColor(value))

val AppThemeId.palette: AppPalette
    get() = when (this) {
        AppThemeId.Cogitor -> AppPalette(
            primary = hex("#E8A33D"),
            secondary = hex("#7EE0C0"),
            background = hex("#0B0E11"),
            surface = hex("#141A21"),
            text = hex("#E9EDEF"),
            textSecondary = hex("#9AA3AB"),
            border = hex("#2C3540"),
            hover = hex("#1C252E"),
            dark = true,
            tertiary = hex("#6E8BFF")
        )

        AppThemeId.Light -> AppPalette(
            primary = hex("#2ecc71"), secondary = hex("#3498db"),
            background = hex("#f5f7fa"), surface = hex("#ffffff"),
            text = hex("#2c3e50"), textSecondary = hex("#7f8c8d"),
            border = hex("#e1e8ed"), hover = hex("#ecf0f1"), dark = false
        )

        AppThemeId.Dark -> AppPalette(
            primary = hex("#2ecc71"), secondary = hex("#3498db"),
            background = hex("#1a1a1a"), surface = hex("#2d2d2d"),
            text = hex("#e0e0e0"), textSecondary = hex("#a0a0a0"),
            border = hex("#404040"), hover = hex("#3a3a3a"), dark = true
        )

        AppThemeId.Green -> AppPalette(
            primary = hex("#27ae60"), secondary = hex("#16a085"),
            background = hex("#e8f5e9"), surface = hex("#ffffff"),
            text = hex("#1b5e20"), textSecondary = hex("#558b2f"),
            border = hex("#c8e6c9"), hover = hex("#f1f8e9"), dark = false
        )

        AppThemeId.Cyan -> AppPalette(
            primary = hex("#00bcd4"), secondary = hex("#0097a7"),
            background = hex("#e0f7fa"), surface = hex("#ffffff"),
            text = hex("#006064"), textSecondary = hex("#00838f"),
            border = hex("#b2ebf2"), hover = hex("#e0f2f1"), dark = false
        )

        AppThemeId.Blue -> AppPalette(
            primary = hex("#2196f3"), secondary = hex("#1976d2"),
            background = hex("#e3f2fd"), surface = hex("#ffffff"),
            text = hex("#0d47a1"), textSecondary = hex("#1565c0"),
            border = hex("#bbdefb"), hover = hex("#e8eaf6"), dark = false
        )

        AppThemeId.Purple -> AppPalette(
            primary = hex("#9c27b0"), secondary = hex("#7b1fa2"),
            background = hex("#f3e5f5"), surface = hex("#ffffff"),
            text = hex("#4a148c"), textSecondary = hex("#6a1b9a"),
            border = hex("#e1bee7"), hover = hex("#ede7f6"), dark = false
        )

        AppThemeId.Darcula -> AppPalette(
            primary = hex("#cc7832"), secondary = hex("#6897bb"),
            background = hex("#2b2b2b"), surface = hex("#3c3f41"),
            text = hex("#a9b7c6"), textSecondary = hex("#808080"),
            border = hex("#555555"), hover = hex("#4b4d4f"), dark = true
        )

        AppThemeId.Rose -> AppPalette(
            primary = hex("#e91e63"), secondary = hex("#c2185b"),
            background = hex("#fce4ec"), surface = hex("#ffffff"),
            text = hex("#880e4f"), textSecondary = hex("#ad1457"),
            border = hex("#f8bbd0"), hover = hex("#f3e5f5"), dark = false
        )

        AppThemeId.Amber -> AppPalette(
            primary = hex("#ff9800"), secondary = hex("#f57c00"),
            background = hex("#fff3e0"), surface = hex("#ffffff"),
            text = hex("#e65100"), textSecondary = hex("#ef6c00"),
            border = hex("#ffe0b2"), hover = hex("#fff8e1"), dark = false
        )

        AppThemeId.Teal -> AppPalette(
            primary = hex("#009688"), secondary = hex("#00796b"),
            background = hex("#e0f2f1"), surface = hex("#ffffff"),
            text = hex("#004d40"), textSecondary = hex("#00695c"),
            border = hex("#b2dfdb"), hover = hex("#e0f7fa"), dark = false
        )

        AppThemeId.Indigo -> AppPalette(
            primary = hex("#3f51b5"), secondary = hex("#303f9f"),
            background = hex("#e8eaf6"), surface = hex("#ffffff"),
            text = hex("#1a237e"), textSecondary = hex("#283593"),
            border = hex("#c5cae9"), hover = hex("#ede7f6"), dark = false
        )

        AppThemeId.Slate -> AppPalette(
            primary = hex("#607d8b"), secondary = hex("#455a64"),
            background = hex("#eceff1"), surface = hex("#ffffff"),
            text = hex("#263238"), textSecondary = hex("#37474f"),
            border = hex("#cfd8dc"), hover = hex("#f5f5f5"), dark = false
        )
    }

fun AppPalette.toColorScheme(): ColorScheme {
    val onPrimary = if (primary.luminance() > 0.45f) Color(0xFF121212) else Color.White
    val onSecondary = if (secondary.luminance() > 0.45f) Color(0xFF121212) else Color.White
    val tert = tertiary ?: secondary
    val onTertiary = if (tert.luminance() > 0.45f) Color(0xFF121212) else Color.White
    val primaryContainer = primary.copy(alpha = if (dark) 0.22f else 0.14f).compositeOver(surface)
    val error = if (dark) Color(0xFFFF6B6B) else Color(0xFFC62828)
    val onError = Color.White
    return if (dark) {
        darkColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = text,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondary.copy(alpha = 0.18f).compositeOver(surface),
            onSecondaryContainer = text,
            tertiary = tert,
            onTertiary = onTertiary,
            background = background,
            onBackground = text,
            surface = surface,
            onSurface = text,
            surfaceVariant = hover,
            onSurfaceVariant = textSecondary,
            outline = border,
            outlineVariant = border,
            error = error,
            onError = onError,
            surfaceContainerLowest = background,
            surfaceContainerLow = background,
            surfaceContainer = surface,
            surfaceContainerHigh = hover,
            surfaceContainerHighest = hover
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = onPrimary,
            primaryContainer = primaryContainer,
            onPrimaryContainer = text,
            secondary = secondary,
            onSecondary = onSecondary,
            secondaryContainer = secondary.copy(alpha = 0.12f).compositeOver(surface),
            onSecondaryContainer = text,
            tertiary = tert,
            onTertiary = onTertiary,
            background = background,
            onBackground = text,
            surface = surface,
            onSurface = text,
            surfaceVariant = hover,
            onSurfaceVariant = textSecondary,
            outline = border,
            outlineVariant = border,
            error = error,
            onError = onError,
            surfaceContainerLowest = background,
            surfaceContainerLow = background,
            surfaceContainer = surface,
            surfaceContainerHigh = hover,
            surfaceContainerHighest = hover
        )
    }
}
