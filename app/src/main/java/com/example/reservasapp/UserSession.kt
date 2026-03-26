package com.example.reservasapp

object UserSession {
    sealed interface State {
        data object Uninitialized : State
        data object LoggedOut : State
        data object AuthenticatedPendingRole : State
        data object AuthenticatedUser : State
        data object AuthenticatedAdmin : State
    }

    var state: State = State.Uninitialized
        private set

    val isAuthenticated: Boolean
        get() = when (state) {
            State.AuthenticatedPendingRole,
            State.AuthenticatedUser,
            State.AuthenticatedAdmin -> true
            State.Uninitialized,
            State.LoggedOut -> false
        }

    val isAdmin: Boolean
        get() = state == State.AuthenticatedAdmin

    fun setAuthenticated(isAdmin: Boolean) {
        state = if (isAdmin) State.AuthenticatedAdmin else State.AuthenticatedUser
    }

    fun setAuthenticatedPendingRole() {
        state = State.AuthenticatedPendingRole
    }

    fun setAuthenticatedUser() {
        state = State.AuthenticatedUser
    }

    fun setAuthenticatedAdmin() {
        state = State.AuthenticatedAdmin
    }

    fun setLoggedOut() {
        state = State.LoggedOut
    }

    fun setUnauthenticated() {
        setLoggedOut()
    }

    fun reset() {
        state = State.Uninitialized
    }

    internal fun updateState(newState: State) {
        state = newState
    }
}
