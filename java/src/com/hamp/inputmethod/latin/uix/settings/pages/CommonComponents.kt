package com.hamp.inputmethod.latin.uix.settings.pages

import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hamp.inputmethod.latin.uix.theme.Typography

@Composable
fun ParagraphText(
    text: String,
    modifier: Modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    style: androidx.compose.ui.text.TextStyle = Typography.Body.Medium,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onBackground,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(text, modifier = modifier, style = style, color = color, textAlign = textAlign)
}