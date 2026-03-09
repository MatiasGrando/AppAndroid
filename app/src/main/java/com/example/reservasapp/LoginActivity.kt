package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var loading: ProgressBar

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(Exception::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener(this) { signInTask ->
                        setLoading(false)
                        if (signInTask.isSuccessful) {
                            ReservasRepository.cargarReservasUsuario {
                                openMainScreen()
                            }
                        } else {
                            showError(getString(R.string.error_google_login))
                        }
                    }
            } catch (_: Exception) {
                setLoading(false)
                showError(getString(R.string.error_google_login_cancelled))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            ReservasRepository.cargarReservasUsuario {
                openMainScreen()
            }
            return
        }

        setContentView(R.layout.activity_login)

        loading = findViewById(R.id.loading)
        val googleButton = findViewById<SignInButton>(R.id.googleButton)

        googleSignInClient = buildGoogleClient()

        googleButton.setSize(SignInButton.SIZE_WIDE)
        googleButton.setOnClickListener {
            setLoading(true)
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun buildGoogleClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(this, gso)
    }

    private fun openMainScreen() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun setLoading(show: Boolean) {
        loading.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
