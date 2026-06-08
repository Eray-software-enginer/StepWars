package com.example.stepwars2.ui.screens.auth

import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stepwars2.R
import com.example.stepwars2.ui.viewmodel.AuthState
import com.example.stepwars2.ui.viewmodel.AuthViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    val darkBg = Color(0xFF0D1117)
    val surfaceDark = Color(0xFF161B22)
    val primaryPurple = Color(0xFF6C63FF)
    val turquoise = Color(0xFF00D2FF)
    val textPrimary = Color(0xFFE6EDF3)
    val textSecondary = Color(0xFF8B949E)
    val borderColor = Color(0xFF30363D)
    val errorColor = Color(0xFFFF6B6B)
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordConfirm by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    var usernameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmError by remember { mutableStateOf<String?>(null) }

    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val isLoading = authState is AuthState.Loading
    var hasNavigated by remember { mutableStateOf(false) }

    // React to auth state changes
    LaunchedEffect(authState) {
        if (authState is AuthState.Success && !hasNavigated) {
            hasNavigated = true
            viewModel.resetState()
            onNavigateToHome()
        }
    }

    // Floating orbs animation (same style as LoginScreen)
    val infiniteTransition = rememberInfiniteTransition(label = "orbs")
    val orbOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 30f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb1"
    )
    val orbOffset2 by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = -20f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orb2"
    )

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = textPrimary,
        unfocusedTextColor = textPrimary,
        cursorColor = primaryPurple,
        focusedBorderColor = primaryPurple,
        unfocusedBorderColor = borderColor,
        focusedLeadingIconColor = primaryPurple,
        unfocusedLeadingIconColor = textSecondary,
        focusedLabelColor = primaryPurple,
        unfocusedLabelColor = textSecondary,
        focusedTrailingIconColor = primaryPurple,
        unfocusedTrailingIconColor = textSecondary,
        focusedContainerColor = surfaceDark,
        unfocusedContainerColor = surfaceDark
    )

    val errorTextFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = textPrimary,
        unfocusedTextColor = textPrimary,
        cursorColor = primaryPurple,
        focusedBorderColor = errorColor,
        unfocusedBorderColor = errorColor,
        focusedLeadingIconColor = errorColor,
        unfocusedLeadingIconColor = errorColor,
        focusedLabelColor = errorColor,
        unfocusedLabelColor = errorColor,
        focusedTrailingIconColor = primaryPurple,
        unfocusedTrailingIconColor = textSecondary,
        focusedContainerColor = surfaceDark,
        unfocusedContainerColor = surfaceDark
    )

    fun validateInputs(): Boolean {
        var valid = true

        if (username.isBlank() || username.length < 3) {
            usernameError = "Kullanıcı adı en az 3 karakter olmalı"
            valid = false
        } else if (username.contains(" ")) {
            usernameError = "Kullanıcı adı boşluk içeremez"
            valid = false
        } else {
            usernameError = null
        }

        if (email.isBlank() || !email.contains("@")) {
            emailError = "Geçerli bir e-posta adresi girin"
            valid = false
        } else {
            emailError = null
        }

        if (password.isBlank() || password.length < 6) {
            passwordError = "Şifre en az 6 karakter olmalı"
            valid = false
        } else {
            passwordError = null
        }

        if (passwordConfirm != password) {
            confirmError = "Şifreler eşleşmiyor"
            valid = false
        } else {
            confirmError = null
        }

        return valid
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(darkBg)
    ) {
        // Decorative glowing orbs
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.4f)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryPurple.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    radius = 200f
                ),
                radius = 200f,
                center = Offset(80f + orbOffset1, 200f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        turquoise.copy(alpha = 0.2f),
                        Color.Transparent
                    ),
                    radius = 150f
                ),
                radius = 150f,
                center = Offset(size.width - 60f + orbOffset2, size.height * 0.3f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryPurple.copy(alpha = 0.15f),
                        Color.Transparent
                    ),
                    radius = 180f
                ),
                radius = 180f,
                center = Offset(size.width * 0.5f, size.height * 0.85f + orbOffset1)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp)
                .padding(top = 80.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Title
            Text(
                text = "STEPWARS",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 4.sp,
                style = androidx.compose.ui.text.TextStyle(
                    brush = Brush.linearGradient(
                        colors = listOf(primaryPurple, turquoise)
                    )
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Yeni Hesap Oluştur",
                fontSize = 18.sp,
                color = textSecondary,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Username Field
            OutlinedTextField(
                value = username,
                onValueChange = {
                    username = it
                    usernameError = null
                },
                label = { Text("Kullanıcı Adı") },
                leadingIcon = {
                    Icon(Icons.Filled.Person, contentDescription = "Kullanıcı Adı")
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = if (usernameError != null) errorTextFieldColors else textFieldColors,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                isError = usernameError != null
            )
            if (usernameError != null) {
                Text(
                    text = usernameError!!,
                    color = errorColor,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = {
                    email = it
                    emailError = null
                },
                label = { Text("E-posta") },
                leadingIcon = {
                    Icon(Icons.Filled.Email, contentDescription = "E-posta")
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = if (emailError != null) errorTextFieldColors else textFieldColors,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                isError = emailError != null
            )
            if (emailError != null) {
                Text(
                    text = emailError!!,
                    color = errorColor,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    passwordError = null
                },
                label = { Text("Şifre") },
                leadingIcon = {
                    Icon(Icons.Filled.Lock, contentDescription = "Şifre")
                },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Şifreyi Gizle" else "Şifreyi Göster"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = if (passwordError != null) errorTextFieldColors else textFieldColors,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                isError = passwordError != null
            )
            if (passwordError != null) {
                Text(
                    text = passwordError!!,
                    color = errorColor,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password Field
            OutlinedTextField(
                value = passwordConfirm,
                onValueChange = {
                    passwordConfirm = it
                    confirmError = null
                },
                label = { Text("Şifre Tekrar") },
                leadingIcon = {
                    Icon(Icons.Filled.Lock, contentDescription = "Şifre Tekrar")
                },
                trailingIcon = {
                    IconButton(onClick = { confirmVisible = !confirmVisible }) {
                        Icon(
                            imageVector = if (confirmVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (confirmVisible) "Şifreyi Gizle" else "Şifreyi Göster"
                        )
                    }
                },
                visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = if (confirmError != null) errorTextFieldColors else textFieldColors,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                isError = confirmError != null
            )
            if (confirmError != null) {
                Text(
                    text = confirmError!!,
                    color = errorColor,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            // Register Button
            Button(
                onClick = {
                    if (validateInputs()) {
                        viewModel.register(username.trim(), email.trim(), password)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent
                ),
                enabled = !isLoading
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = if (isLoading) listOf(
                                    primaryPurple.copy(alpha = 0.5f),
                                    Color(0xFF9C7CFF).copy(alpha = 0.5f)
                                ) else listOf(primaryPurple, Color(0xFF9C7CFF))
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Kayıt Ol",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Auth error message
            if (authState is AuthState.Error) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = (authState as AuthState.Error).message,
                    color = errorColor,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = borderColor
                )
                Text(
                    text = "  veya  ",
                    color = textSecondary,
                    fontSize = 14.sp
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = borderColor
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Google Sign In Button
            OutlinedButton(
                onClick = {
                    coroutineScope.launch {
                        try {
                            val credentialManager = CredentialManager.create(context)
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId(context.getString(R.string.default_web_client_id))
                                .build()
                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()
                            val result = credentialManager.getCredential(context, request)
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                            val idToken = googleIdTokenCredential.idToken
                            viewModel.signInWithGoogle(idToken)
                        } catch (e: Exception) {
                            Log.e("RegisterScreen", "Google Sign-In failed", e)
                            Toast.makeText(context, "Google girişi başarısız: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(14.dp),
                border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                    brush = Brush.horizontalGradient(
                        colors = listOf(borderColor, borderColor)
                    )
                ),
                enabled = !isLoading
            ) {
                Text(
                    text = "G",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = "Google ile Kayıt Ol",
                    fontSize = 15.sp,
                    color = textPrimary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Login link
            Row(
                modifier = Modifier.padding(top = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Zaten hesabın var mı? ",
                    color = textSecondary,
                    fontSize = 14.sp
                )
                TextButton(onClick = { onNavigateToLogin() }) {
                    Text(
                        text = "Giriş Yap",
                        color = primaryPurple,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
