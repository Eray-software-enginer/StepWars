package com.example.stepwars2.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

enum class CurrencyType {
    GOLD,
    GEMS
}

@Composable
fun CurrencyDisplay(
    amount: Int,
    type: CurrencyType,
    modifier: Modifier = Modifier
) {
    val icon = when (type) {
        CurrencyType.GOLD -> "⚜\uFE0F"
        CurrencyType.GEMS -> "\uD83D\uDC8E"
    }

    val color = when (type) {
        CurrencyType.GOLD -> Color(0xFFFFD700)
        CurrencyType.GEMS -> Color(0xFFE040FB)
    }

    val numberFormat = remember {
        NumberFormat.getNumberInstance(Locale("tr", "TR"))
    }

    val formattedAmount = remember(amount) {
        numberFormat.format(amount)
    }

    GlassCard(
        modifier = modifier,
        cornerRadius = 12.dp,
        contentPadding = 8.dp,
        borderColor = color.copy(alpha = 0.2f),
        glowColor = color.copy(alpha = 0.05f)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            Text(
                text = icon,
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = formattedAmount,
                color = color,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
