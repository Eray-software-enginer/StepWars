package com.example.stepwars2.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

@Composable
fun StepProgressRing(
    currentSteps: Int,
    goalSteps: Int = 10_000,
    modifier: Modifier = Modifier
) {
    val progress = (currentSteps.toFloat() / goalSteps.toFloat()).coerceIn(0f, 1f)

    var animationTarget by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = animationTarget,
        animationSpec = tween(durationMillis = 1500),
        label = "progressAnimation"
    )

    LaunchedEffect(progress) {
        animationTarget = progress
    }

    val numberFormat = remember {
        NumberFormat.getNumberInstance(Locale("tr", "TR"))
    }

    val formattedSteps = remember(currentSteps) {
        numberFormat.format(currentSteps)
    }

    val formattedGoal = remember(goalSteps) {
        numberFormat.format(goalSteps)
    }

    val ringThickness = 12.dp
    val purpleColor = Color(0xFF6C63FF)
    val turquoiseColor = Color(0xFF4ECDC4)
    val backgroundRingColor = Color(0xFF2A2A2A).copy(alpha = 0.5f)
    val glowColor = Color(0xFF6C63FF).copy(alpha = 0.3f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(250.dp)
    ) {
        Canvas(modifier = Modifier.size(250.dp)) {
            val strokeWidth = ringThickness.toPx()
            val glowStrokeWidth = strokeWidth + 8.dp.toPx()
            val arcSize = size.minDimension - strokeWidth
            val topLeft = Offset(
                (size.width - arcSize) / 2f,
                (size.height - arcSize) / 2f
            )

            // Background ring
            drawArc(
                color = backgroundRingColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcSize, arcSize),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Glow effect behind the progress arc
            if (animatedProgress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(purpleColor.copy(alpha = 0.4f), turquoiseColor.copy(alpha = 0.4f))
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = glowStrokeWidth, cap = StrokeCap.Round)
                )
            }

            // Progress ring with gradient
            if (animatedProgress > 0f) {
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(purpleColor, turquoiseColor, purpleColor)
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animatedProgress,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        // Text content inside the ring
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Bugünkü Adımlar",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = formattedSteps,
                color = Color.White,
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "/ $formattedGoal hedef",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
