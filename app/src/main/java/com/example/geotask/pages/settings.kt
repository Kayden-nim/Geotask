package com.example.geotask.pages

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.geotask.location.LocationTracker
import com.google.accompanist.permissions.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.android.gms.location.*

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun SettingsScreen(navController: NavController, userId: String) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()

    var isLocationEnabled by remember { mutableStateOf(false) }
    var isMessageEnabled by remember { mutableStateOf(false) }
    var isGpsEnabled by remember { mutableStateOf(checkDeviceLocation(context)) }

    val locationTracker = remember { LocationTracker(context, userId, firestore) }


    // 🔄 Track GPS changes dynamically
    LaunchedEffect(Unit) {
        while (true) {
            isGpsEnabled = checkDeviceLocation(context)
            kotlinx.coroutines.delay(5000)  // Check every 5 seconds
        }
    }
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // 🔄 Load settings from Firestore
    LaunchedEffect(userId) {
        firestore.collection("users").document(userId)
            .collection("settings").document("location").get()
            .addOnSuccessListener { doc ->
                isLocationEnabled = doc.getBoolean("enabled") ?: false
                if (isLocationEnabled) {
                    locationTracker.startTracking() // Start tracking if already enabled
                }
            }

        firestore.collection("users").document(userId)
            .collection("settings").document("messages").get()
            .addOnSuccessListener { doc ->
                isMessageEnabled = doc.getBoolean("enabled") ?: false
            }
    }

    val locationPermissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    val notificationPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = "Settings", style = MaterialTheme.typography.headlineMedium)

        // ❗ GPS OFF Warning
        if (!isGpsEnabled) {
            Text(
                text = "⚠ GPS is turned off. Location tracking will not work.",
                color = Color.Red,
                style = MaterialTheme.typography.bodyMedium
            )
            Button(onClick = { promptEnableGPS(context) }) {
                Text(text = "Turn On GPS")
            }
        }

        // 📌 Location Toggle
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Enable Live Location")
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = isLocationEnabled,
                onCheckedChange = { enabled ->
                    isLocationEnabled = enabled
                    updateFirestore(userId, "location", mapOf("enabled" to enabled), firestore)

                    if (enabled) {
                        if (locationPermissionsState.allPermissionsGranted) {
                            if (isGpsEnabled) {
                                locationTracker.startTracking()
                            } else {
                                promptEnableGPS(context)
                            }
                        } else {
                            locationPermissionsState.launchMultiplePermissionRequest()
                        }
                    }

                }
            )
        }

        // 📌 Message Toggle
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(text = "Enable Notifications")
            Spacer(modifier = Modifier.weight(1f))
            Switch(
                checked = isMessageEnabled,
                onCheckedChange = { enabled ->
                    isMessageEnabled = enabled
                    updateFirestore(userId, "messages", mapOf("enabled" to enabled), firestore)

                    if (enabled && !notificationPermissionState.status.isGranted) {
                        notificationPermissionState.launchPermissionRequest()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ⬅ Back Button
        Button(onClick = { navController.popBackStack() }) {
            Text(text = "Back")
        }
    }
}

// ✅ Reactive GPS check
private fun checkDeviceLocation(context: Context): Boolean {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
}

// ✅ Prompt user to enable GPS
private fun promptEnableGPS(context: Context) {
    Toast.makeText(context, "Please enable GPS for location tracking", Toast.LENGTH_LONG).show()
    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
    context.startActivity(intent)
}

@SuppressLint("MissingPermission")
private fun fetchAndSaveLocation(context: Context, userId: String, firestore: FirebaseFirestore) {
    val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    val locationRequest = LocationRequest.create().apply {
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        interval = 5000  // Request location updates every 5 seconds
        fastestInterval = 2000
        numUpdates = 1  // Only fetch once
    }

    val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            val location = locationResult.lastLocation
            if (location != null) {
                val locationData = mapOf(
                    "latitude" to location.latitude,
                    "longitude" to location.longitude,
                    "enabled" to true
                )
                updateFirestore(userId, "location", locationData, firestore)
            } else {
                Toast.makeText(context, "Could not fetch location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null)
}


// ✅ Update Firestore in "settings" subcollection
private fun updateFirestore(userId: String, document: String, data: Map<String, Any?>, firestore: FirebaseFirestore) {
    firestore.collection("users").document(userId)
        .collection("settings").document(document)
        .set(data, SetOptions.merge())
        .addOnFailureListener {
            println("Failed to update Firestore: ${it.message}")
        }
}
