package com.example.reservasapp

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class AuthRepository(context: Context) {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val googleSignInClient: GoogleSignInClient

    init {
        val webClientId = context.getString(R.string.default_web_client_id)
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun usuarioAutenticado() = auth.currentUser

    fun googleSignInIntent(): Intent = googleSignInClient.signInIntent

    fun autenticarConGoogle(idToken: String, onResult: (Result<String>) -> Unit) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid.isNullOrBlank()) {
                    onResult(Result.failure(IllegalStateException("UID inválido")))
                } else {
                    onResult(Result.success(uid))
                }
            }
            .addOnFailureListener { error ->
                onResult(Result.failure(error))
            }
    }

    fun logout(activity: Activity, onDone: () -> Unit) {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            onDone()
        }
    }
}
