package com.example.nshutiplanner.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import com.example.nshutiplanner.data.repository.FirebaseRepository
import com.example.nshutiplanner.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(repo: FirebaseRepository, onLogin: () -> Unit, onGoRegister: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AuthScaffold {
        Text("Welcome back 💜", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("NshutiTrack", style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(40.dp))

        NshutiTextField(value = email, onValueChange = { email = it }, label = "Email",
            keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(16.dp))
        NshutiTextField(value = password, onValueChange = { password = it }, label = "Password",
            keyboardType = KeyboardType.Password, isPassword = true)

        if (error.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                loading = true; error = ""
                scope.launch {
                    repo.login(email, password)
                        .onSuccess { loading = false; onLogin() }
                        .onFailure { loading = false; error = it.message ?: "Login failed" }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp),
            enabled = !loading,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 3.dp)
            else Text("Sign In", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onGoRegister, modifier = Modifier.fillMaxWidth()) {
            Text("Don't have an account? Register", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun RegisterScreen(repo: FirebaseRepository, onRegister: () -> Unit, onGoLogin: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AuthScaffold {
        Text("Join NshutiTrack 🌟", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Grow together", style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(40.dp))

        NshutiTextField(value = name, onValueChange = { name = it }, label = "Your Name")
        Spacer(Modifier.height(16.dp))
        NshutiTextField(value = email, onValueChange = { email = it }, label = "Email",
            keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(16.dp))
        NshutiTextField(value = password, onValueChange = { password = it }, label = "Password",
            keyboardType = KeyboardType.Password, isPassword = true)

        if (error.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                if (name.isBlank() || email.isBlank() || password.isBlank()) {
                    error = "Please fill in all fields"
                    return@Button
                }
                loading = true; error = ""
                scope.launch {
                    repo.register(email, password, name)
                        .onSuccess { loading = false; onRegister() }
                        .onFailure { loading = false; error = it.message ?: "Registration failed" }
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(18.dp),
            enabled = !loading
        ) {
            if (loading) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 3.dp)
            else Text("Create Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onGoLogin, modifier = Modifier.fillMaxWidth()) {
            Text("Already have an account? Sign In", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun AuthScaffold(content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Indigo900, Indigo700, MaterialTheme.colorScheme.background),
                    startY = 0f,
                    endY = 1200f
                )
            )
            .imePadding() // push content above keyboard
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Rounded.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(Modifier.height(24.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun NshutiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline
        )
    )
}
