package com.example.reservasapp

import android.app.ActivityManager
import android.graphics.BitmapFactory
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.reservasapp.branding.AppRuntime
import com.example.reservasapp.firebase.FirebaseProvider

open class BaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        AppThemePreference.applySavedMode(this)
        super.onCreate(savedInstanceState)
        applyBuyerVisibleIdentity()

        if (UserSession.state == UserSession.State.Uninitialized) {
            SessionBootstrap.bootstrap()
        }
    }

    protected fun applyBuyerVisibleIdentity() {
        if (!AppRuntime.isInitialized) {
            return
        }

        val branding = AppRuntime.branding
        title = getString(branding.appNameRes)
        setTaskDescription(
            ActivityManager.TaskDescription(
                getString(branding.appNameRes),
                BitmapFactory.decodeResource(resources, branding.appLogoRes)
            )
        )
    }

    protected fun ensureAuthenticatedSession(): Boolean {
        return when (BaseActivityGuards.resolveAuthenticatedGuard(UserSession.state, FirebaseProvider.auth().currentUser != null)) {
            AuthenticatedGuardAction.ALLOW -> true
            AuthenticatedGuardAction.BOOTSTRAP_AND_ALLOW -> {
                SessionBootstrap.bootstrap()
                true
            }
            AuthenticatedGuardAction.REDIRECT_LOGIN -> {
                UserSession.setLoggedOut()
                redirectToLogin()
                false
            }
        }
    }

    protected fun ensureAdminAccess(): Boolean {
        return when (BaseActivityGuards.resolveAdminGuard(UserSession.state)) {
            AdminGuardAction.ALLOW -> true
            AdminGuardAction.REDIRECT_MAIN -> {
                startActivity(Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                })
                finish()
                false
            }
            AdminGuardAction.REDIRECT_LOGIN -> {
                redirectToLogin()
                false
            }
        }
    }

    private fun redirectToLogin() {
        startActivity(Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}

internal enum class AuthenticatedGuardAction {
    ALLOW,
    BOOTSTRAP_AND_ALLOW,
    REDIRECT_LOGIN
}

internal enum class AdminGuardAction {
    ALLOW,
    REDIRECT_MAIN,
    REDIRECT_LOGIN
}

internal object BaseActivityGuards {
    fun resolveAuthenticatedGuard(
        state: UserSession.State,
        hasFirebaseUser: Boolean
    ): AuthenticatedGuardAction {
        return when (state) {
            UserSession.State.AuthenticatedPendingRole,
            UserSession.State.AuthenticatedUser,
            UserSession.State.AuthenticatedAdmin -> AuthenticatedGuardAction.ALLOW

            UserSession.State.Uninitialized -> {
                if (hasFirebaseUser) {
                    AuthenticatedGuardAction.BOOTSTRAP_AND_ALLOW
                } else {
                    AuthenticatedGuardAction.REDIRECT_LOGIN
                }
            }

            UserSession.State.LoggedOut -> AuthenticatedGuardAction.REDIRECT_LOGIN
        }
    }

    fun resolveAdminGuard(state: UserSession.State): AdminGuardAction {
        return when (state) {
            UserSession.State.AuthenticatedAdmin -> AdminGuardAction.ALLOW
            UserSession.State.AuthenticatedUser,
            UserSession.State.AuthenticatedPendingRole -> AdminGuardAction.REDIRECT_MAIN
            UserSession.State.Uninitialized,
            UserSession.State.LoggedOut -> AdminGuardAction.REDIRECT_LOGIN
        }
    }
}
