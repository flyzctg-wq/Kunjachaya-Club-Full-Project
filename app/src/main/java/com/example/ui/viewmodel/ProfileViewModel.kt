package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.UserEntity
import com.example.data.repository.ClubRepository
import com.example.data.repository.FirestoreRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val firestoreRepository = FirestoreRepository()
    private val clubRepository = ClubRepository(AppDatabase.getDatabase(application))

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _saveMessage = MutableStateFlow<String?>(null)
    val saveMessage: StateFlow<String?> = _saveMessage.asStateFlow()

    /**
     * Save/Update user profile in Firebase Auth, Firestore 'users' collection, and local Room DB.
     */
    fun saveUserProfile(
        updatedUser: UserEntity,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _isSaving.value = true
            _saveMessage.value = null

            // 1. Update Firebase Auth user profile display name if user is logged in
            try {
                val firebaseAuthUser = FirebaseAuth.getInstance().currentUser
                if (firebaseAuthUser != null) {
                    val profileChange = UserProfileChangeRequest.Builder()
                        .setDisplayName(updatedUser.nameEn)
                        .build()
                    firebaseAuthUser.updateProfile(profileChange).await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Push to Firestore
            val firestoreResult = firestoreRepository.saveUser(updatedUser)

            // 3. Save locally to Room DB (Offline First)
            clubRepository.updateUser(updatedUser)

            _isSaving.value = false

            if (firestoreResult.isSuccess) {
                _saveMessage.value = "Profile updated in Firebase Auth & Firestore successfully!"
                onSuccess()
            } else {
                val errorMsg = firestoreResult.exceptionOrNull()?.localizedMessage ?: "Firebase sync offline"
                _saveMessage.value = "Profile saved locally ($errorMsg)"
                // Always call onSuccess so user changes are saved locally
                onSuccess()
            }
        }
    }

    fun clearMessage() {
        _saveMessage.value = null
    }
}

