package com.example.geotask

import android.os.Build
import android.window.SplashScreen
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.example.geotask.pages.ProfileScreen
import com.example.geotask.pages.SettingsScreen
import com.example.geotask.pages.TodoList
import com.example.geotask.pages.TodoViewModel
import com.example.geotask.start.Homepage
import com.example.geotask.start.LoginScreen
import com.example.geotask.start.Signup
import com.example.geotask.start.SplashScreen

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun Navi(
    modifier: Modifier = Modifier,
    authViewModel: AuthViewModel = viewModel(),
    todoViewModel: TodoViewModel = viewModel(),
) {
    val navController = rememberNavController()
    val authState = authViewModel.authState.observeAsState(AuthState.Unauthenticated)

    NavHost(
        navController = navController,
        startDestination = "splash"
    ) {
        // Splash Screen
        composable("splash") {
            SplashScreen(navController, authState.value)
        }

        // Login Screen
        composable(
            route = "login",
            enterTransition = { fadeIn(animationSpec = tween(500)) },
            exitTransition = { fadeOut(animationSpec = tween(500)) }
        ) {
            LoginScreen(
                modifier = modifier,
                navController = navController,
                authViewModel = authViewModel
            )
        }

        composable("settings") {
            SettingsScreen(
                navController = navController,
                userId = "USER_ID_HERE"  // Replace with the actual user ID
            )
        }

        // Signup Screen
        composable(route = "signup") {
            Signup(
                navController = navController,
                authViewModel = authViewModel
            )
        }

        // Home Screen
        composable(route = "home") {
            Homepage(
                modifier = modifier,
                navController = navController,
                authViewModel = authViewModel
            )
        }

        // To-Do List Screen
        composable(route = "todoList") {
            TodoList(
                navController = navController,
                todoViewModel = todoViewModel
            )
        }

        composable(route = "profile") {
            ProfileScreen()
        }

    }
}
