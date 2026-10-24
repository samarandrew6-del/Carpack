package pl.statusaut

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Styl w duchu Samsung One UI: czarne tło, zaokrąglone karty, niebieski akcent.
private val DarkColors = darkColorScheme(
    primary = Color(0xFF3E91FF),
    onPrimary = Color.White,
    background = Color.Black,
    onBackground = Color(0xFFF2F2F4),
    surface = Color(0xFF1B1B1D),
    onSurface = Color(0xFFF2F2F4),
    surfaceVariant = Color(0xFF2A2A2D),
    onSurfaceVariant = Color(0xFF9A9AA0),
    outline = Color(0xFF3A3A3E),
    secondaryContainer = Color(0xFF16263D),
    onSecondaryContainer = Color(0xFF7DB8FF),
    error = Color(0xFFFF6B6B)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF3E91FF),
    onPrimary = Color.White,
    background = Color(0xFFF6F6F6),
    onBackground = Color(0xFF111111),
    surface = Color.White,
    onSurface = Color(0xFF111111),
    surfaceVariant = Color(0xFFEFEFF2),
    onSurfaceVariant = Color(0xFF6F6F76),
    outline = Color(0xFFD6D6DB),
    secondaryContainer = Color(0xFFE6F0FF),
    onSecondaryContainer = Color(0xFF1F6FEB),
    error = Color(0xFFD93025)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun StatusAutTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = AppShapes,
        content = content
    )
}
