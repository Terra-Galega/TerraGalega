package com.example.terragalegamovile.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.terragalegamovile.R

// Títulos (font-serif en la web)
val Playfair = FontFamily(
    Font(R.font.playfair_display_regular, FontWeight.Normal),
    Font(R.font.playfair_display_semibold, FontWeight.SemiBold),
    Font(R.font.playfair_display_bold, FontWeight.Bold)
)

// Texto general (font-sans en la web)
val SourceSans = FontFamily(
    Font(R.font.source_sans_3_regular, FontWeight.Normal),
    Font(R.font.source_sans_3_medium, FontWeight.Medium),
    Font(R.font.source_sans_3_semibold, FontWeight.SemiBold)
)

// Logo: "Terra" en cursiva y "GALEGA" con serifa
val PinyonScript = FontFamily(Font(R.font.pinyon_script_regular))
val NotoSerif = FontFamily(Font(R.font.noto_serif_regular))

val Typography = Typography(
    displayMedium = TextStyle(fontFamily = Playfair, fontWeight = FontWeight.SemiBold, fontSize = 44.sp),
    headlineLarge = TextStyle(fontFamily = Playfair, fontWeight = FontWeight.SemiBold, fontSize = 32.sp),
    headlineSmall = TextStyle(fontFamily = Playfair, fontWeight = FontWeight.SemiBold, fontSize = 24.sp),
    titleLarge = TextStyle(fontFamily = Playfair, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = Playfair, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    bodyLarge = TextStyle(fontFamily = SourceSans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontFamily = SourceSans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = SourceSans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = SourceSans, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = SourceSans, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 1.sp)
)