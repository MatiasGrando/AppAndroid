package com.example.reservasapp

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import com.example.reservasapp.branding.AppRuntime
import com.example.reservasapp.firebase.FirebaseProvider
import com.google.android.material.button.MaterialButton
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.appbar.MaterialToolbar

class MainActivity : BaseActivity() {

    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!ensureAuthenticatedSession()) {
            return
        }

        setContentView(R.layout.activity_main)
        applyBranding()

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        findViewById<View>(R.id.btnProfileMenu).setOnClickListener { view ->
            showMainMenu(view)
        }

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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (handleMenuItemSelection(item)) {
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }

    private fun showMainMenu(anchor: View) {
        val popupMenu = PopupMenu(this, anchor)
        popupMenu.menuInflater.inflate(R.menu.menu_main, popupMenu.menu)
        popupMenu.menu.findItem(R.id.action_admin_panel)?.isVisible = UserSession.isAdmin
        popupMenu.setOnMenuItemClickListener { item -> handleMenuItemSelection(item) }
        popupMenu.show()
    }

    private fun handleMenuItemSelection(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                startActivity(Intent(this, PerfilDatosPersonalesActivity::class.java))
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

            else -> false
        }
    }

    private fun logout() {
        ReservasRepository.clearCache()
        UserSession.setUnauthenticated()
        FirebaseProvider.auth().signOut()
        googleSignInClient.signOut().addOnCompleteListener {
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun buildGoogleClient(): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(FirebaseProvider.googleWebClientId())
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(this, gso)
    }

    private fun applyBranding() {
        val branding = AppRuntime.branding
        findViewById<ViewGroup>(android.R.id.content).getChildAt(0).setBackgroundResource(branding.homeBackgroundRes)
        findViewById<View>(R.id.overlay).setBackgroundColor(
            ContextCompat.getColor(this, branding.homeOverlayColorRes)
        )
        val accentColor = ContextCompat.getColor(this, branding.confirmationTitleColorRes)
        val subtitleColor = ContextCompat.getColor(this, branding.confirmationBodyTextColorRes)

        findViewById<TextView>(R.id.tvTitle).apply {
            setText(branding.appNameRes)
            setTextColor(accentColor)
        }

        findViewById<TextView>(R.id.tvSubtitle).apply {
            if (branding.homeSubtitleRes != 0) {
                visibility = View.VISIBLE
                setText(branding.homeSubtitleRes)
                setTextColor(subtitleColor)
            } else {
                visibility = View.GONE
            }
        }

        findViewById<ImageView>(R.id.ivBrandLogo).apply {
            if (branding.appLogoRes != 0) {
                visibility = View.VISIBLE
                setImageResource(branding.appLogoRes)
                contentDescription = getString(branding.appNameRes)
            } else {
                visibility = View.GONE
            }
        }

        val reservarButton = findViewById<MaterialButton>(R.id.btnReservar)
        val misReservasButton = findViewById<MaterialButton>(R.id.btnMisReservas)

        reservarButton.setText(branding.homePrimaryActionRes)
        reservarButton.setTextColor(ContextCompat.getColor(this, branding.actionTextColorRes))
        reservarButton.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, branding.primaryActionColorRes)
        )

        misReservasButton.setText(branding.homeSecondaryActionRes)
        misReservasButton.setTextColor(ContextCompat.getColor(this, branding.actionTextColorRes))
        misReservasButton.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(this, branding.secondaryActionColorRes)
        )

        title = getString(branding.appNameRes)
    }

    private fun isSessionValidForUi(state: UserSession.State): Boolean {
        return state == UserSession.State.AuthenticatedUser ||
            state == UserSession.State.AuthenticatedAdmin
    }

}
