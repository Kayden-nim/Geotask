package com.example.geotask.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.geotask.AuthViewModel
import com.example.geotask.R
import com.example.geotask.UserDetails

@Composable
fun CustomDropdown(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    userDetails: UserDetails?,
    profileIconResId: Int = R.drawable.geotask
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Ensure recomposition when userDetails updates
    val currentUserDetails by rememberUpdatedState(newValue = userDetails)

    Box(
        modifier = Modifier
            .size(50.dp)
            .clickable { isDropdownExpanded = !isDropdownExpanded }
    ) {
        Image(
            painter = painterResource(id = profileIconResId),
            contentDescription = "Profile Picture",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .border(2.dp, Color.White, CircleShape)
        )

        Icon(
            imageVector = Icons.Default.ArrowDropDown,
            contentDescription = "Dropdown Arrow",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .size(16.dp)
        )
    }

    DropdownMenu(
        expanded = isDropdownExpanded,
        onDismissRequest = { isDropdownExpanded = false },
        modifier = Modifier.background(Color.White)
    ) {
        DropdownMenuItem(
            onClick = {
                isDropdownExpanded = false
                navController.navigate("profile")
            },
            text = {
                Column {
                    Text(
                        text = currentUserDetails?.displayName ?: "Guest",
                        color = Color.Black,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = currentUserDetails?.email ?: "No email available",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        )

        Divider(color = Color.LightGray, thickness = 1.dp)

        DropdownMenuItem(
            onClick = {
                isDropdownExpanded = false
                navController.navigate("settings")
            },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Settings", color = Color.Black)
                }
            }
        )

        DropdownMenuItem(
            onClick = {
                isDropdownExpanded = false
                authViewModel.signOut(navController)
            },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ExitToApp, contentDescription = "Sign Out", tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out", color = Color.Red)
                }
            }
        )
    }
}
