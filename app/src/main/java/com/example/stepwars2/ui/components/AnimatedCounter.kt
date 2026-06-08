package com.example.stepwars2.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AnimatedCounter(
    targetValue: Int,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    fontSize: TextUnit = 32.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    durationMillis: Int = 1200
) {
    var animationTarget by remember { mutableIntStateOf(0) }
    val animatedValue by animateIntAsState(
        targetValue = animationTarget,
        animationSpec = tween(durationMillis = durationMillis),
        label = "counterAnimation"
    )

    LaunchedEffect(targetValue) {
        animationTarget = targetValue
    }

    val numberFormat = remember {
        NumberFormat.getNumberInstance(Locale("tr", "TR"))
    }

    val formattedValue = remember(animatedValue) {
        numberFormat.format(animatedValue)
    }

    Text(
        text = formattedValue,
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        modifier = modifier
    )
}
