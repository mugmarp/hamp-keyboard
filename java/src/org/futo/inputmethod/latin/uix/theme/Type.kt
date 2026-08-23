package org.futo.inputmethod.latin.uix.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.futo.inputmethod.latin.R

/**
 * Hamp Keyboard typography — "Charcoal & Ember" design system.
 *
 * Two bundled variable fonts (SIL OFL, see res/raw/licenses):
 *  - Space Grotesk: display/headings. Tight tracking (-0.02em) for a modern feel.
 *  - DM Sans:       body/UI text.
 *
 * Both are variable TTFs; on API 26+ Compose selects weights via FontVariation.
 * On API 24/25 the default instance (Regular 400) is used and FontWeight still
 * applies synthetic bolding, so hierarchy remains legible on older devices.
 */

val DisplayFontFamily = FontFamily(Font(R.font.space_grotesk_variable))
val BodyFontFamily = FontFamily(Font(R.font.dm_sans_variable))

data object Typography {
    data object Heading {
        val MediumMl = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.2).sp  // -0.02em @ 20sp
        )

        val Medium = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            lineHeight = 20.sp,
            letterSpacing = (-0.2).sp
        )

        val RegularMl = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            lineHeight = 30.sp,
            letterSpacing = (-0.2).sp
        )

        val Regular = TextStyle(
            fontFamily = DisplayFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            lineHeight = 20.sp,
            letterSpacing = (-0.2).sp
        )
    }

    data object Body {
        val MediumMl = TextStyle(
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 24.sp
        )

        val Medium = TextStyle(
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 16.sp
        )

        val RegularMl = TextStyle(
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp
        )

        val Regular = TextStyle(
            fontFamily = BodyFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 16.sp
        )
    }

    val SmallMl = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    )

    val Small = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 14.sp
    )

    /**
     * Section labels: uppercase micro-headers above grouped cards.
     * Web spec: 0.6875rem (11sp), 600 weight, 0.14em tracking, muted color.
     * Color comes from the caller (onSurfaceVariant) since TextStyle carries no color here.
     */
    val SectionLabel = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.54.sp  // 0.14em @ 11sp
    )
}
