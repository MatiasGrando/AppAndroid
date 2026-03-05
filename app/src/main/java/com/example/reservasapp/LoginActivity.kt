package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn

class LoginActivity : AppCompatActivity() {

    private val authRepository by lazy { AuthRepository(this) }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        if (!task.isSuccessful) {
            Toast.makeText(this, R.string.error_login_google, Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        val account = task.result
        val token = account.idToken
        if (token.isNullOrBlank()) {
            Toast.makeText(this, R.string.error_login_google, Toast.LENGTH_SHORT).show()
            return@registerForActivityResult
        }

        authRepository.autenticarConGoogle(token) { authResult ->
            if (authResult.isFailure) {
                Toast.makeText(this, R.string.error_login_google, Toast.LENGTH_SHORT).show()
                return@autenticarConGoogle
            }
            startActivity(Intent(this, LauncherActivity::class.java))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        findViewById<Button>(R.id.btnGoogleLogin).setOnClickListener {
            launcher.launch(authRepository.googleSignInIntent())
        }
    }
}
