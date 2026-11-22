package app.project001.data.auth

import android.content.Context
import app.project001.BuildConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

/**
 * Sign-in. The backend verifies the phone number and hands back a Firebase custom token;
 * Firebase Auth turns it into a session that survives restarts and refreshes itself.
 * Firebase is configured in code (no google-services.json): see app/build.gradle.kts.
 */
class AuthRepository(context: Context) {
    private val auth: FirebaseAuth? = BuildConfig.FIREBASE_API_KEY.takeIf { it.isNotBlank() }?.let {
        val options = FirebaseOptions.Builder()
            .setApiKey(BuildConfig.FIREBASE_API_KEY)
            .setApplicationId(BuildConfig.FIREBASE_APP_ID)
            .setProjectId(BuildConfig.FIREBASE_PROJECT_ID)
            .build()
        val app = FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(context, options)
        FirebaseAuth.getInstance(app)
    }

    /** False when this build has no Firebase config (then nobody can sign in). */
    val configured: Boolean get() = auth != null

    private val _signedIn = MutableStateFlow(auth?.currentUser != null)
    val signedIn: StateFlow<Boolean> = _signedIn.asStateFlow()

    init {
        auth?.addAuthStateListener { _signedIn.value = it.currentUser != null }
    }

    suspend fun signIn(customToken: String) {
        val a = auth ?: error("Sign-in isn't set up in this build.")
        a.signInWithCustomToken(customToken).await()
    }

    /** A fresh ID token for the backend (Firebase refreshes it when it's about to expire). */
    suspend fun idToken(): String? = auth?.currentUser?.getIdToken(false)?.await()?.token

    fun signOut() {
        auth?.signOut()
    }
}
