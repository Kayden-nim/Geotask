package com.example.geotask

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class AuthViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private val _userDetails = MutableLiveData<UserDetails?>()
    val userDetails: LiveData<UserDetails?> = _userDetails

    private var authListener: FirebaseAuth.AuthStateListener? = null

    init {
        checkAuthStatus()
        authListener = FirebaseAuth.AuthStateListener { checkAuthStatus() }
        auth.addAuthStateListener(authListener!!)
    }

    private fun checkAuthStatus() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _authState.value = AuthState.Unauthenticated
            _userDetails.value = null
        } else {
            _authState.value = AuthState.Authenticated
            createUserDocumentIfNotExists(currentUser) // Ensure user data exists
        }
    }

    private fun fetchUserDetails(user: FirebaseUser) {
        val userRef = db.collection("users").document(user.uid)

        userRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val username = document.getString("username") ?: "No username"
                    _userDetails.value = UserDetails(
                        displayName = user.displayName ?: "Guest",
                        email = user.email ?: "No email",
                        username = username
                    )
                }
            }
            .addOnFailureListener {
                _userDetails.value = UserDetails(
                    displayName = user.displayName ?: "Guest",
                    email = user.email ?: "No email",
                    username = "Error fetching username"
                )
            }
    }

    private fun createUserDocumentIfNotExists(user: FirebaseUser) {
        val userRef = db.collection("users").document(user.uid)

        userRef.get().addOnSuccessListener { document ->
            if (!document.exists()) {
                val userData = hashMapOf(
                    "email" to (user.email ?: ""),
                    "username" to (user.displayName ?: ""),
                    "displayName" to (user.displayName ?: ""),
                    "createdAt" to System.currentTimeMillis()
                )

                userRef.set(userData, SetOptions.merge())
                    .addOnSuccessListener {
                        Log.d("Firestore", "User document created successfully")
                        fetchUserDetails(user)
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Failed to create user document", e)
                    }
            } else {
                fetchUserDetails(user)
            }
        }.addOnFailureListener { e ->
            Log.e("Firestore", "Failed to check user document", e)
        }
    }

    private fun handleAuthTask(task: Task<AuthResult>, onSuccess: () -> Unit) {
        if (task.isSuccessful) {
            _authState.value = AuthState.Authenticated
            val user = auth.currentUser!!
            createUserDocumentIfNotExists(user)  // Ensure Firestore document is created
            onSuccess()
        } else {
            _authState.value = AuthState.Error(task.exception?.message ?: "Something went wrong")
        }
    }

    fun login(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Email or Password cannot be empty")
            return
        }

        _authState.value = AuthState.Loading

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                handleAuthTask(task) { }
            }
    }

    fun signup(email: String, password: String, username: String) {
        val trimmedEmail = email.trim()
        val trimmedUsername = username.trim()

        if (trimmedEmail.isEmpty() || password.isEmpty() || trimmedUsername.isEmpty()) {
            _authState.value = AuthState.Error("Fields cannot be empty")
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            _authState.value = AuthState.Error("Invalid email format")
            return
        }

        _authState.value = AuthState.Loading

        auth.createUserWithEmailAndPassword(trimmedEmail, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(trimmedUsername)
                            .build()

                        user.updateProfile(profileUpdates).addOnCompleteListener {
                            createUserDocumentIfNotExists(user)
                        }
                    }
                } else {
                    _authState.value = AuthState.Error(task.exception?.message ?: "Signup failed")
                }
            }
    }
    fun updateGpsSetting(enabled: Boolean) {
        val user = auth.currentUser ?: return
        val settingsRef = db.collection("users")
            .document(user.uid)
            .collection("settings")
            .document("preferences")

        val updateData = hashMapOf("gpsTracking" to enabled)

        settingsRef.set(updateData, SetOptions.merge()) // Merges without overwriting other settings
            .addOnSuccessListener {
                Log.d("Firestore", "GPS setting updated successfully")
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Failed to update GPS setting", e)
            }
    }

    fun updateUsername(newUsername: String, onComplete: (Boolean) -> Unit) {
        val user = auth.currentUser
        if (user != null) {
            val userRef = db.collection("users").document(user.uid)

            userRef.update("username", newUsername)
                .addOnSuccessListener {
                    fetchUserDetails(user)  // Fetch updated data
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    Log.e("AuthViewModel", "Failed to update username", e)
                    onComplete(false)
                }
        } else {
            onComplete(false)
        }
    }

    fun updatePassword(newPassword: String, currentPassword: String, onComplete: (Boolean, String?) -> Unit) {
        val user = auth.currentUser ?: return onComplete(false, "User not authenticated")
        val email = user.email ?: return onComplete(false, "Email not available for re-authentication")

        val credential = EmailAuthProvider.getCredential(email, currentPassword)

        user.reauthenticate(credential)
            .addOnSuccessListener {
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        onComplete(true, null)
                    }
                    .addOnFailureListener { e ->
                        onComplete(false, e.localizedMessage ?: "Failed to update password")
                    }
            }
            .addOnFailureListener { e ->
                onComplete(false, "Re-authentication failed: ${e.localizedMessage}")
            }
    }

    fun signOut(navController: NavHostController) {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
        _userDetails.value = null
        navController.navigate("login") {
            popUpTo("home") { inclusive = true }
        }
    }

    override fun onCleared() {
        super.onCleared()
        authListener?.let { auth.removeAuthStateListener(it) }
    }
}

sealed class AuthState {
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
}

data class UserDetails(
    val displayName: String = "",
    val email: String = "",
    val username: String = ""
)
