package com.example.geotask.start

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import com.example.geotask.AuthState
import com.example.geotask.R // Import for resource access

@Composable
fun SplashScreen(navController: NavController, authState: AuthState) {
    LaunchedEffect(Unit) {
        delay(2000) // Show splash for 2 seconds
        when (authState) {
            is AuthState.Authenticated -> navController.navigate("home") {
                popUpTo("splash") { inclusive = true }
            }
            else -> navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    // Set background color to match image color (Replace with actual color)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(221, 239, 241)), // Change this to match the dominant color of your image
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.banner), // Replace with your image name
                contentDescription = "Splash Logo",
                modifier = Modifier.fillMaxSize()// Adjust size as needed
            )

        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    SplashScreen(rememberNavController(), AuthState.Unauthenticated)
}
