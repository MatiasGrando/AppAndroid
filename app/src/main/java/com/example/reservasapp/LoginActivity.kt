package com.example.reservasapp

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.reservasapp.branding.AppRuntime
import com.example.reservasapp.firebase.FirebaseProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : BaseActivity() {

    private companion object {
        const val LOGIN_FADE_DURATION = 280L
        const val LOGIN_CONTENT_TRANSLATION_Y = 24f
        const val LOGIN_CONTENT_START_SCALE = 0.97f
        const val STARTUP_LOGO_END_SCALE = 1.04f
        const val LOGIN_CONTENT_STAGGER_DELAY = 36L
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var startupLogo: ImageView
    private lateinit var loading: ProgressBar
    private lateinit var loginLogo: ImageView
    private lateinit var loginTitle: TextView
    private lateinit var googleButton: SignInButton
    private var hasAnimatedLoginEntrance = false
    private val startupFadeInterpolator = AccelerateInterpolator(1.1f)
    private val loginEntranceInterpolator = DecelerateInterpolator(1.2f)

    private val googleSignInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(Exception::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential)
                    .addOnCompleteListener(this) { signInTask ->
                        if (signInTask.isSuccessful) {
                            cargarDatosInicialesYEntrar()
                        } else {
                            showLoggedOutState()
                            showError(getString(R.string.error_google_login))
                        }
                    }
            } catch (_: Exception) {
                showLoggedOutState()
                showError(getString(R.string.error_google_login_cancelled))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showLoginUi()

        auth = FirebaseProvider.auth()
        if (auth.currentUser != null) {
            cargarDatosInicialesYEntrar()
            return
        }

        ReservasRepository.clearCache()
        UserSession.setLoggedOut()
        showLoggedOutState()
    }

    private fun showLoginUi() {
        if (::loading.isInitialized) {
            return
        }

        setContentView(R.layout.activity_login)

        startupLogo = findViewById(R.id.startupLogo)
        loading = findViewById(R.id.loading)
        loginLogo = findViewById(R.id.ivLogo)
        loginTitle = findViewById(R.id.tvLoginTitle)
        googleButton = findViewById(R.id.googleButton)

        applyBranding()
        googleSignInClient = buildGoogleClient()

        googleButton.setSize(SignInButton.SIZE_WIDE)
        googleButton.setOnClickListener {
            showStartupState()
            googleSignInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    private fun showStartupState() {
        if (!::loading.isInitialized) {
            return
        }
        hasAnimatedLoginEntrance = false
        startupLogo.animate().cancel()
        loginLogo.animate().cancel()
        loginTitle.animate().cancel()
        googleButton.animate().cancel()
        resetAnimatedView(startupLogo)
        resetAnimatedView(loginLogo)
        resetAnimatedView(loginTitle)
        resetAnimatedView(googleButton)
        startupLogo.visibility = View.VISIBLE
        loginLogo.visibility = View.GONE
        loginTitle.visibility = View.GONE
        googleButton.visibility = View.GONE
        loading.visibility = View.GONE
    }

    private fun showLoggedOutState() {
        if (!::loading.isInitialized) {
            return
        }
        loading.visibility = View.GONE

        if (startupLogo.visibility == View.VISIBLE && !hasAnimatedLoginEntrance) {
            animateLoginEntrance()
            return
        }

        startupLogo.visibility = View.GONE
        loginLogo.visibility = View.VISIBLE
        loginTitle.setText(AppRuntime.branding.loginTitleRes)
        loginTitle.visibility = View.VISIBLE
        googleButton.visibility = View.VISIBLE
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
        val appName = getString(branding.appNameRes)
        findViewById<ViewGroup>(android.R.id.content).getChildAt(0).setBackgroundColor(
            ContextCompat.getColor(this, branding.loginBackgroundColorRes)
        )
        startupLogo.setImageResource(branding.appLogoRes)
        startupLogo.contentDescription = appName
        loginLogo.setImageResource(branding.appLogoRes)
        loginLogo.contentDescription = appName
        loginTitle.setText(branding.loginTitleRes)
        title = appName
    }

    private fun openMainScreen() {
        startActivity(Intent(this, MainActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }

    private fun animateLoginEntrance() {
        hasAnimatedLoginEntrance = true

        loginLogo.visibility = View.VISIBLE
        loginTitle.setText(AppRuntime.branding.loginTitleRes)
        loginTitle.visibility = View.VISIBLE
        googleButton.visibility = View.VISIBLE

        prepareLoginEntranceView(loginLogo)
        prepareLoginEntranceView(loginTitle)
        prepareLoginEntranceView(googleButton)

        startupLogo.alpha = 1f
        startupLogo.scaleX = 1f
        startupLogo.scaleY = 1f
        startupLogo.translationY = 0f

        startupLogo.animate()
            .alpha(0f)
            .scaleX(STARTUP_LOGO_END_SCALE)
            .scaleY(STARTUP_LOGO_END_SCALE)
            .setDuration(LOGIN_FADE_DURATION)
            .setInterpolator(startupFadeInterpolator)
            .withEndAction {
                startupLogo.visibility = View.GONE
                resetAnimatedView(startupLogo)
            }
            .start()

        animateLoginEntranceView(loginLogo, LOGIN_CONTENT_TRANSLATION_Y, 1f, LOGIN_CONTENT_STAGGER_DELAY)
        animateLoginEntranceView(loginTitle, LOGIN_CONTENT_TRANSLATION_Y, 1f, LOGIN_CONTENT_STAGGER_DELAY * 2)
        animateLoginEntranceView(googleButton, LOGIN_CONTENT_TRANSLATION_Y, 1f, LOGIN_CONTENT_STAGGER_DELAY * 3)
    }

    private fun prepareLoginEntranceView(view: View) {
        view.alpha = 0f
        view.scaleX = LOGIN_CONTENT_START_SCALE
        view.scaleY = LOGIN_CONTENT_START_SCALE
        view.translationY = LOGIN_CONTENT_TRANSLATION_Y
    }

    private fun animateLoginEntranceView(view: View, translationY: Float, targetAlpha: Float, startDelay: Long) {
        view.translationY = translationY
        view.animate()
            .alpha(targetAlpha)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(startDelay)
            .setDuration(LOGIN_FADE_DURATION)
            .setInterpolator(loginEntranceInterpolator)
            .start()
    }

    private fun resetAnimatedView(view: View) {
        view.alpha = 1f
        view.scaleX = 1f
        view.scaleY = 1f
        view.translationY = 0f
    }

    private fun cargarDatosInicialesYEntrar() {
        showStartupState()
        PerfilRepository.sincronizarPerfilConGoogle {
            ReservasRepository.cargarReservasUsuario {
                SessionBootstrap.bootstrap(forceRefresh = true) { state ->
                    if (!isActiveForUiUpdates()) {
                        return@bootstrap
                    }

                    if (state == UserSession.State.LoggedOut) {
                        showLoggedOutState()
                        showError(getString(R.string.error_google_login))
                        return@bootstrap
                    }

                    openMainScreen()
                }
            }
        }
    }

    private fun isActiveForUiUpdates(): Boolean {
        return !isFinishing && !isDestroyed
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
