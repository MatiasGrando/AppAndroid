package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth

class MainActivity : BaseActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ensureAuthenticatedSession()) {
            return
        }

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

        SessionBootstrap.bootstrap { state ->
            if (isFinishing || isDestroyed) {
                return@bootstrap
            }

            if (!isSessionValidForUi(state)) {
                ensureAuthenticatedSession()
                return@bootstrap
            }

            invalidateOptionsMenu()
            ReservasRepository.cargarReservasUsuario { }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.findItem(R.id.action_admin_panel)?.isVisible = UserSession.isAdmin
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val titleRes = if (AppThemePreference.isDarkModeEnabled(this)) {
            R.string.menu_theme_to_light
        } else {
            R.string.menu_theme_to_dark
        }
        menu.findItem(R.id.action_toggle_theme)?.setTitle(titleRes)
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                startActivity(Intent(this, PerfilDatosPersonalesActivity::class.java))
                true
            }

            R.id.action_toggle_theme -> {
                AppThemePreference.toggle(this)
                invalidateOptionsMenu()
                true
            }

            R.id.action_logout -> {
                logout()
                true
            }

            R.id.action_admin_panel -> {
                if (UserSession.isAdmin) {
                    startActivity(Intent(this, AdminActivity::class.java))
                }
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        ReservasRepository.clearCache()
        UserSession.setUnauthenticated()
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

    private fun isSessionValidForUi(state: UserSession.State): Boolean {
        return state == UserSession.State.AuthenticatedUser ||
            state == UserSession.State.AuthenticatedAdmin
    }

}
