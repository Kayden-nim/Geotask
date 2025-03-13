package com.example.geotask.pages

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.geotask.AuthViewModel
import com.example.geotask.R
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(authViewModel: AuthViewModel = viewModel()) {
    val userDetails by authViewModel.userDetails.observeAsState()
    val context = LocalContext.current

    var isEditing by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    // Load username when screen opens
    LaunchedEffect(userDetails) {
        userDetails?.let {
            username = it.username
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(221, 239, 241))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "User Icon",
            tint = Color.Gray,
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Logged in as: ${userDetails?.email ?: "No Email"}",
            fontSize = 18.sp,
            color = Color.DarkGray
        )

        ProfileTextField(
            label = "Username",
            value = username,
            isEditing = isEditing,
            onValueChange = { username = it },
            onEditClick = { isEditing = true }
        )

        Spacer(modifier = Modifier.height(20.dp))

        errorMessage?.let {
            Text(text = it, color = Color.Red, modifier = Modifier.padding(8.dp))
        }

        if (isEditing) {
            Button(
                onClick = {
                    isLoading = true
                    errorMessage = null

                    coroutineScope.launch {
                        authViewModel.updateUsername(username) { success ->
                            if (success) {
                                Toast.makeText(context, "Username updated!", Toast.LENGTH_SHORT).show()
                                isEditing = false
                            } else {
                                errorMessage = "Failed to update username"
                            }
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Text("Save Changes")
                }
            }
        }
    }
}


@Composable
fun ProfileTextField(
    label: String,
    value: String,
    isEditing: Boolean,
    onValueChange: (String) -> Unit,
    onEditClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.weight(1f))
            if (label != "Email") {
                IconButton(onClick = onEditClick) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit")
                }
            }
        }

        if (isEditing && label != "Email") {
            TextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.LightGray, shape = CircleShape)
                    .padding(12.dp),
                textStyle = TextStyle(fontSize = 16.sp)
            )
        } else {
            Text(
                text = value,
                fontSize = 16.sp,
                color = Color.Black,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .background(Color.Transparent)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}
