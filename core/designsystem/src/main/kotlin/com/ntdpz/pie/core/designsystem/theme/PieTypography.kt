package com.ntdpz.pie.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.ntdpz.pie.core.designsystem.R

private val NotoSansThai = FontFamily(Font(R.font.noto_sans_thai))

private fun text(size: Int, weight: FontWeight = FontWeight.Normal) = TextStyle(
    fontFamily = NotoSansThai,
    fontSize = size.sp,
    lineHeight = (size * 1.5).sp,
    letterSpacing = 0.sp,
    fontWeight = weight,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
)

val PieTypography = Typography(
    displayLarge = text(40), displayMedium = text(34), displaySmall = text(30),
    headlineLarge = text(30), headlineMedium = text(29), headlineSmall = text(25),
    titleLarge = text(20), titleMedium = text(17), titleSmall = text(15),
    bodyLarge = text(18), bodyMedium = text(16), bodySmall = text(14),
    labelLarge = text(15, FontWeight.SemiBold), labelMedium = text(13), labelSmall = text(12),
)
