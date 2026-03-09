package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)

        googleSignInClient = buildGoogleClient()

        findViewById<android.widget.Button>(R.id.btnReservar).setOnClickListener {
            startActivity(Intent(this, ReservarActivity::class.java))
        }

        findViewById<android.widget.Button>(R.id.btnMisReservas).setOnClickListener {
            startActivity(Intent(this, MisReservasActivity::class.java))
        }
    }

    override fun onStart() {
        super.onStart()
        if (FirebaseAuth.getInstance().currentUser != null) {
            ReservasRepository.cargarReservasUsuario { }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                showProfileDialog()
                true
            }

            R.id.action_logout -> {
                logout()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showProfileDialog() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val profileMessage = getString(
            R.string.profile_data_message,
            currentUser?.email ?: getString(R.string.not_available),
            getString(R.string.empty_field),
            getString(R.string.empty_field),
            getString(R.string.empty_field)
        )

        AlertDialog.Builder(this)
            .setTitle(R.string.profile_data_title)
            .setMessage(profileMessage)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun buildGoogleClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(this, gso)
    }
}
