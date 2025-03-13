package com.example.geotask.pages

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile = _userProfile.asStateFlow()

    private var profileListener: ListenerRegistration? = null

    init {
        listenForUserProfileChanges()
    }

    /**
     * Listens for real-time changes in the user profile.
     */
    private fun listenForUserProfileChanges() {
        val userId = auth.currentUser?.uid ?: return

        profileListener = db.collection("users")
            .document(userId)
            .addSnapshotListener { document, error ->
                if (error != null) {
                    Log.e("ProfileViewModel", "Error listening for user profile: ${error.message}")
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    _userProfile.value = UserProfile(
                        username = document.getString("username") ?: "",
                        email = document.getString("email") ?: ""
                    )
                }
            }
    }

    /**
     * Updates the username in Firestore and Firebase Authentication.
     */
    fun updateUsername(newUsername: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = auth.currentUser ?: return onError("User not authenticated")
        val userId = user.uid

        viewModelScope.launch {
            try {
                // Update Firestore
                db.collection("users").document(userId).update("username", newUsername).await()

                // Update Firebase Auth Display Name
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(newUsername)
                    .build()

                user.updateProfile(profileUpdates).await()
                onSuccess()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to update username: ${e.message}")
                onError(e.localizedMessage ?: "Failed to update username")
            }
        }
    }

    /**
     * Fetches the user profile without real-time updates.
     */
    fun fetchUserProfile() {
        val user = auth.currentUser ?: return
        val userId = user.uid

        viewModelScope.launch {
            try {
                val document = db.collection("users").document(userId).get().await()
                if (document.exists()) {
                    _userProfile.value = UserProfile(
                        username = document.getString("username") ?: "",
                        email = document.getString("email") ?: ""
                    )
                }
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error fetching user profile: ${e.message}")
            }
        }
    }

    /**
     * Updates the user password after reauthentication.
     */
    fun updatePassword(
        newPassword: String,
        currentPassword: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser ?: return onError("User not authenticated")
        val email = user.email ?: return onError("Email not available for re-authentication")

        viewModelScope.launch {
            try {
                // Reauthenticate user
                val credential = EmailAuthProvider.getCredential(email, currentPassword)
                user.reauthenticate(credential).await()

                // Update password
                user.updatePassword(newPassword).await()
                onSuccess()
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Failed to update password: ${e.message}")
                onError(e.localizedMessage ?: "Failed to update password")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        profileListener?.remove()
    }
}

data class UserProfile(
    val username: String = "",
    val email: String = ""
)
