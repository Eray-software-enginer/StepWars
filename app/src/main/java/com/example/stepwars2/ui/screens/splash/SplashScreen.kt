package com.example.stepwars2.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stepwars2.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val darkBg = Color(0xFF0D1117)
    val primaryPurple = Color(0xFF6C63FF)
    val turquoise = Color(0xFF00D2FF)

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    // Scale animation
    val scale = remember { Animatable(0.3f) }
    // Fade animation
    val alpha = remember { Animatable(0f) }
    // Subtitle animation
    val subtitleAlpha = remember { Animatable(0f) }

    // Pulsing glow
    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    LaunchedEffect(Unit) {
        // Animate in
        scale.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
    }
    LaunchedEffect(Unit) {
        alpha.animateTo(1f, animationSpec = tween(1000))
    }
    LaunchedEffect(Unit) {
        delay(600)
        subtitleAlpha.animateTo(1f, animationSpec = tween(800))
    }

    // Navigation based on auth state
    LaunchedEffect(Unit) {
        delay(2500)
        when (isLoggedIn) {
            true -> onNavigateToHome()
            false -> onNavigateToLogin()
            null -> {
                // Still checking, wait a bit more then default to login
                delay(500)
                if (isLoggedIn == true) {
                    onNavigateToHome()
                } else {
                    onNavigateToLogin()
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg),
        contentAlignment = Alignment.Center
    ) {
        // Pulsing circular glow behind logo
        Canvas(
            modifier = Modifier
                .size(280.dp)
                .scale(pulseScale)
                .alpha(pulseAlpha)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryPurple.copy(alpha = 0.4f),
                        turquoise.copy(alpha = 0.1f),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2, size.height / 2),
                    radius = size.minDimension / 2
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scale.value)
                .alpha(alpha.value)
        ) {
            // App name with gradient
            Text(
                text = "STEPWARS",
                fontSize = 52.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 6.sp,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(primaryPurple, turquoise)
                    )
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subtitle
            Text(
                text = "Yürü. Güçlen. Savaş.",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF8B949E),
                letterSpacing = 3.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(subtitleAlpha.value)
            )
        }

        // Loading indicator at bottom
        CircularProgressIndicator(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp)
                .size(24.dp)
                .alpha(alpha.value),
            color = primaryPurple,
            strokeWidth = 2.dp,
            trackColor = Color(0xFF21262D)
        )
    }
}
