package com.example.reservasapp

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UserSessionTest {

    @Before
    fun setUp() {
        UserSession.reset()
    }

    @After
    fun tearDown() {
        UserSession.reset()
    }

    @Test
    fun setAuthenticatedAsAdminMarksSessionAsAuthenticatedAdmin() {
        UserSession.setAuthenticated(isAdmin = true)

        assertEquals(UserSession.State.AuthenticatedAdmin, UserSession.state)
        assertTrue(UserSession.isAuthenticated)
        assertTrue(UserSession.isAdmin)
    }

    @Test
    fun pendingRoleIsAuthenticatedButNotAdmin() {
        UserSession.setAuthenticatedPendingRole()

        assertEquals(UserSession.State.AuthenticatedPendingRole, UserSession.state)
        assertTrue(UserSession.isAuthenticated)
        assertFalse(UserSession.isAdmin)
    }

    @Test
    fun setUnauthenticatedLeavesSessionLoggedOut() {
        UserSession.setAuthenticatedUser()

        UserSession.setUnauthenticated()

        assertEquals(UserSession.State.LoggedOut, UserSession.state)
        assertFalse(UserSession.isAuthenticated)
        assertFalse(UserSession.isAdmin)
    }

    @Test
    fun resetReturnsSessionToUninitialized() {
        UserSession.setAuthenticatedAdmin()

        UserSession.reset()

        assertEquals(UserSession.State.Uninitialized, UserSession.state)
        assertFalse(UserSession.isAuthenticated)
        assertFalse(UserSession.isAdmin)
    }
}
