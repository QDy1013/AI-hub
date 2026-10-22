package com.aihub.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Violet = Color(0xFF5B4BDB)
private val VioletLight = Color(0xFFB4ABFF)

private val LightColors = lightColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEAE7FF),
    onPrimaryContainer = Color(0xFF1C1453),
    secondary = Color(0xFF0E8AD8),
    // 选中的标签和底部导航指示器用品牌紫的浅色调，保持整体色调统一
    secondaryContainer = Color(0xFFEAE7FF),
    onSecondaryContainer = Color(0xFF1C1453),
    background = Color(0xFFF5F5F9),
    onBackground = Color(0xFF14151A),
    surface = Color(0xFFF5F5F9),
    onSurface = Color(0xFF14151A),
    onSurfaceVariant = Color(0xFF6B6F7B),
    surfaceVariant = Color(0xFFECECF2),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF0F0F5),
    surfaceContainerHighest = Color(0xFFE9E9F0),
    outline = Color(0xFFB9BBC6),
    outlineVariant = Color(0xFFE6E6EE),
    error = Color(0xFFD92D20),
)

private val DarkColors = darkColorScheme(
    primary = VioletLight,
    onPrimary = Color(0xFF221A66),
    primaryContainer = Color(0xFF2E2575),
    onPrimaryContainer = Color(0xFFE6E2FF),
    secondary = Color(0xFF5CC0FF),
    secondaryContainer = Color(0xFF2E2575),
    onSecondaryContainer = Color(0xFFE6E2FF),
    background = Color(0xFF0B0C10),
    onBackground = Color(0xFFECEDF2),
    surface = Color(0xFF0B0C10),
    onSurface = Color(0xFFECEDF2),
    onSurfaceVariant = Color(0xFF9A9EAB),
    surfaceVariant = Color(0xFF22242C),
    surfaceContainerLowest = Color(0xFF0E0F14),
    surfaceContainerLow = Color(0xFF14161C),
    surfaceContainer = Color(0xFF16181F),
    surfaceContainerHigh = Color(0xFF1E2028),
    surfaceContainerHighest = Color(0xFF272932),
    outline = Color(0xFF4A4D58),
    outlineVariant = Color(0xFF262830),
    error = Color(0xFFFF6B5E),
)

/** 语义色：额度与状态提示 */
object StatusColors {
    val Good = Color(0xFF12A150)
    val Warn = Color(0xFFE08A00)
    val Bad = Color(0xFFD92D20)
    val Neutral = Color(0xFF7A7F8C)
}

/** 卡片背景用的品牌渐变 */
object Gradients {
    /** 仪表盘总览：深墨色到紫罗兰 */
    val Hero = Brush.linearGradient(listOf(Color(0xFF14123A), Color(0xFF2B2373), Color(0xFF5B3FD0)))

    /** DeepSeek 余额卡：深海蓝 */
    val DeepSeek = Brush.linearGradient(listOf(Color(0xFF0A1A4F), Color(0xFF1F3FB8), Color(0xFF4D6BFE)))
}

private val AppTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
        headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        titleSmall = base.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelSmall = base.labelSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.4.sp),
    )
}

/** 小号大写风格的分组标题 */
val SectionLabelStyle = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun AIHubTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, typography = AppTypography, shapes = AppShapes, content = content)
}
