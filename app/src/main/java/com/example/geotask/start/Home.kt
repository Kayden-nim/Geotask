package com.example.geotask.start

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.geotask.AuthViewModel
import com.example.geotask.R
import com.example.geotask.pages.CustomDropdown
import androidx.compose.runtime.getValue
import androidx.compose.ui.draw.shadow

@Composable
fun Homepage(
    modifier: Modifier = Modifier,
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    // Observe user details safely
    val userDetails by authViewModel.userDetails.observeAsState()

    // Root container with gradient background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient( // Changed to vertical for a smoother effect
                    colors = listOf(Color(152,235,238) ,Color(172,228,230))
                )
            )
            .padding(16.dp) // Added padding for better layout
    ) {
        // Profile Dropdown at the top-right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 16.dp) // Responsive positioning
        ) {
            CustomDropdown(
                navController = navController,
                authViewModel = authViewModel,
                userDetails = userDetails,
                profileIconResId = R.drawable.geotask
            )
        }

        // To-Do List Button at the center-right
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.CenterEnd // Aligns the Box to the right-center
        ) {
            IconButton(
                onClick = { navController.navigate("todoList") },
                modifier = Modifier
                    .size(50.dp)  // Adjusted to a small button size
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .shadow(8.dp, shape = CircleShape) // Added shadow effect
//            .offset(y = (-30).dp)  // Moves the button slightly upward
            ) {
                Image(
                    painter = painterResource(id = R.drawable.todo),
                    contentDescription = "Show To-Do List",
                    contentScale = ContentScale.Crop,
//            modifier = Modifier.size(32.dp)  // Image size within the button
                )
            }
        }


    }
}

