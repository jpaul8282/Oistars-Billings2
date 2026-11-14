package com.example.data.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.oistars.billings.R
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

private const val TAG = "FirebaseAuthManager"

class FirebaseAuthManager(
    private val context: Context,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    private fun getFirestoreDb(): FirebaseFirestore {
        return try {
            FirebaseFirestore.getInstance(
                FirebaseApp.getInstance(),
                context.getString(R.string.firestore_database_id)
            )
        } catch (e: Exception) {
            FirebaseFirestore.getInstance()
        }
    }

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun registerWithEmail(
        email: String,
        pass: String,
        displayName: String,
        businessName: String = ""
    ): Result<FirebaseUser> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user
            if (user != null) {
                if (displayName.isNotBlank()) {
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName.trim())
                        .build()
                    user.updateProfile(profileUpdates).await()
                }

                // Provision user profile record in Firestore
                try {
                    val profileData = mapOf(
                        "uid" to user.uid,
                        "email" to (user.email ?: email.trim()),
                        "displayName" to displayName.trim(),
                        "businessName" to businessName.trim(),
                        "role" to "BILLING_OWNER",
                        "createdAt" to System.currentTimeMillis(),
                        "registeredVia" to "FIREBASE_EMAIL_PASSWORD"
                    )
                    getFirestoreDb().collection("users")
                        .document(user.uid)
                        .set(profileData, SetOptions.merge())
                        .await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed writing user profile to Firestore: ${e.message}")
                }

                Result.success(user)
            } else {
                Result.failure(Exception("Registration failed: Firebase user is null"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering user with Firebase: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(Exception("Sign-in failed: Firebase user is null"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error signing in with email: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email.trim()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending password reset: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogle(): Result<FirebaseUser> {
        return try {
            val serverClientId = context.getString(R.string.default_web_client_id)
            val credentialManager = CredentialManager.create(context)

            val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(serverClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInWithGoogleOption)
                .build()

            val result = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = result.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = auth.signInWithCredential(authCredential).await()
                val user = authResult.user
                if (user != null) {
                    // Sync Google user profile to Firestore
                    try {
                        val profileData = mapOf(
                            "uid" to user.uid,
                            "email" to (user.email ?: ""),
                            "displayName" to (user.displayName ?: "Google User"),
                            "photoUrl" to (user.photoUrl?.toString() ?: ""),
                            "role" to "BILLING_OWNER",
                            "lastLoginAt" to System.currentTimeMillis(),
                            "registeredVia" to "GOOGLE_SIGN_IN"
                        )
                        getFirestoreDb().collection("users")
                            .document(user.uid)
                            .set(profileData, SetOptions.merge())
                            .await()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed syncing Google user to Firestore: ${e.message}")
                    }

                    Result.success(user)
                } else {
                    Result.failure(Exception("Firebase user is null after sign in"))
                }
            } else {
                Result.failure(Exception("Unsupported credential type: ${credential::class.java.name}"))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Google Sign-in was cancelled by user", e)
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.e(TAG, "Credential Manager error during Google Sign-in: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during Google Sign-in: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
