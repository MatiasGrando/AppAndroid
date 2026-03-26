package com.example.reservasapp

import com.google.firebase.auth.FirebaseAuth

object SessionBootstrap {
    private val auth by lazy { FirebaseAuth.getInstance() }

    internal var currentUserUidProvider: () -> String? = { auth.currentUser?.uid }
    internal var clearCacheAction: () -> Unit = { ReservasRepository.clearCache() }
    internal var resolveSessionStateAction: ((UserSession.State) -> Unit) -> Unit = {
        PerfilRepository.resolverEstadoSesionActual(it)
    }
    internal var isAuthenticatedProvider: () -> Boolean = { UserSession.isAuthenticated }
    internal var currentSessionStateProvider: () -> UserSession.State = { UserSession.state }
    internal var setAuthenticatedPendingRoleAction: () -> Unit = { UserSession.setAuthenticatedPendingRole() }
    internal var updateSessionStateAction: (UserSession.State) -> Unit = { UserSession.updateState(it) }

    private var isBootstrapping = false
    private var currentBootstrapUid: String? = null
    private var resolvedUid: String? = null
    private val pendingCallbacks = mutableListOf<(UserSession.State) -> Unit>()

    fun bootstrap(
        forceRefresh: Boolean = false,
        onComplete: ((UserSession.State) -> Unit)? = null
    ) {
        onComplete?.let { pendingCallbacks += it }

        val uid = currentUserUidProvider()
        if (uid == null) {
            resolvedUid = null
            currentBootstrapUid = null
            clearCacheAction()
            complete(UserSession.State.LoggedOut)
            return
        }

        if (!forceRefresh && resolvedUid == uid && isAuthenticatedProvider() && !isBootstrapping) {
            complete(currentSessionStateProvider(), cacheHit = true)
            return
        }

        if (resolvedUid != null && resolvedUid != uid) {
            clearCacheAction()
        }

        if (!isAuthenticatedProvider() || currentBootstrapUid != uid) {
            setAuthenticatedPendingRoleAction()
        }

        if (isBootstrapping && currentBootstrapUid == uid) {
            return
        }

        isBootstrapping = true
        currentBootstrapUid = uid

        resolveSessionStateAction { state ->
            val sameUser = currentUserUidProvider() == uid
            resolvedUid = if (sameUser) uid else null
            currentBootstrapUid = if (sameUser) uid else null
            complete(if (sameUser) state else UserSession.State.LoggedOut)
        }
    }

    private fun complete(state: UserSession.State, cacheHit: Boolean = false) {
        if (!cacheHit) {
            updateSessionStateAction(state)
        }

        isBootstrapping = false

        if (state == UserSession.State.LoggedOut) {
            resolvedUid = null
            currentBootstrapUid = null
            clearCacheAction()
        }

        val callbacks = pendingCallbacks.toList()
        pendingCallbacks.clear()
        callbacks.forEach { it(state) }
    }

    internal fun resetForTests() {
        isBootstrapping = false
        currentBootstrapUid = null
        resolvedUid = null
        pendingCallbacks.clear()
        currentUserUidProvider = { auth.currentUser?.uid }
        clearCacheAction = { ReservasRepository.clearCache() }
        resolveSessionStateAction = { PerfilRepository.resolverEstadoSesionActual(it) }
        isAuthenticatedProvider = { UserSession.isAuthenticated }
        currentSessionStateProvider = { UserSession.state }
        setAuthenticatedPendingRoleAction = { UserSession.setAuthenticatedPendingRole() }
        updateSessionStateAction = { UserSession.updateState(it) }
    }
}
