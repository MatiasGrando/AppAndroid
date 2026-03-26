package com.example.reservasapp

import org.junit.Assert.assertEquals
import org.junit.Test

class BaseActivityGuardsTest {

    @Test
    fun authenticatedGuardAllowsAuthenticatedStates() {
        assertEquals(
            AuthenticatedGuardAction.ALLOW,
            BaseActivityGuards.resolveAuthenticatedGuard(UserSession.State.AuthenticatedPendingRole, false)
        )
        assertEquals(
            AuthenticatedGuardAction.ALLOW,
            BaseActivityGuards.resolveAuthenticatedGuard(UserSession.State.AuthenticatedUser, false)
        )
        assertEquals(
            AuthenticatedGuardAction.ALLOW,
            BaseActivityGuards.resolveAuthenticatedGuard(UserSession.State.AuthenticatedAdmin, false)
        )
    }

    @Test
    fun authenticatedGuardBootstrapsOnlyForUninitializedStateWithFirebaseUser() {
        assertEquals(
            AuthenticatedGuardAction.BOOTSTRAP_AND_ALLOW,
            BaseActivityGuards.resolveAuthenticatedGuard(UserSession.State.Uninitialized, true)
        )
        assertEquals(
            AuthenticatedGuardAction.REDIRECT_LOGIN,
            BaseActivityGuards.resolveAuthenticatedGuard(UserSession.State.Uninitialized, false)
        )
    }

    @Test
    fun authenticatedGuardRedirectsLoggedOutState() {
        assertEquals(
            AuthenticatedGuardAction.REDIRECT_LOGIN,
            BaseActivityGuards.resolveAuthenticatedGuard(UserSession.State.LoggedOut, true)
        )
    }

    @Test
    fun adminGuardAllowsOnlyAdmin() {
        assertEquals(AdminGuardAction.ALLOW, BaseActivityGuards.resolveAdminGuard(UserSession.State.AuthenticatedAdmin))
    }

    @Test
    fun adminGuardRedirectsNonAdminAuthenticatedStatesToMain() {
        assertEquals(
            AdminGuardAction.REDIRECT_MAIN,
            BaseActivityGuards.resolveAdminGuard(UserSession.State.AuthenticatedUser)
        )
        assertEquals(
            AdminGuardAction.REDIRECT_MAIN,
            BaseActivityGuards.resolveAdminGuard(UserSession.State.AuthenticatedPendingRole)
        )
    }

    @Test
    fun adminGuardRedirectsAnonymousStatesToLogin() {
        assertEquals(
            AdminGuardAction.REDIRECT_LOGIN,
            BaseActivityGuards.resolveAdminGuard(UserSession.State.Uninitialized)
        )
        assertEquals(
            AdminGuardAction.REDIRECT_LOGIN,
            BaseActivityGuards.resolveAdminGuard(UserSession.State.LoggedOut)
        )
    }
}
