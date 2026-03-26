package com.example.reservasapp

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SessionBootstrapTest {

    private var currentUid: String? = null
    private var clearCacheCalls = 0
    private var resolveCalls = 0
    private var pendingResolver: ((UserSession.State) -> Unit)? = null

    @Before
    fun setUp() {
        UserSession.reset()
        SessionBootstrap.resetForTests()

        currentUid = null
        clearCacheCalls = 0
        resolveCalls = 0
        pendingResolver = null

        SessionBootstrap.currentUserUidProvider = { currentUid }
        SessionBootstrap.clearCacheAction = { clearCacheCalls += 1 }
        SessionBootstrap.resolveSessionStateAction = { callback ->
            resolveCalls += 1
            pendingResolver = callback
        }
    }

    @After
    fun tearDown() {
        SessionBootstrap.resetForTests()
        UserSession.reset()
    }

    @Test
    fun bootstrapWithoutCurrentUserLogsOutAndCompletesImmediately() {
        var callbackState: UserSession.State? = null

        SessionBootstrap.bootstrap { callbackState = it }

        assertEquals(UserSession.State.LoggedOut, UserSession.state)
        assertEquals(UserSession.State.LoggedOut, callbackState)
        assertEquals(0, resolveCalls)
        assertEquals(2, clearCacheCalls)
    }

    @Test
    fun bootstrapWhileResolvingSameUserQueuesCallbacksAndResolvesOnce() {
        currentUid = "user-1"
        val callbackStates = mutableListOf<UserSession.State>()

        SessionBootstrap.bootstrap { callbackStates += it }
        SessionBootstrap.bootstrap { callbackStates += it }

        assertEquals(UserSession.State.AuthenticatedPendingRole, UserSession.state)
        assertEquals(1, resolveCalls)
        assertTrue(callbackStates.isEmpty())

        pendingResolver!!.invoke(UserSession.State.AuthenticatedAdmin)

        assertEquals(UserSession.State.AuthenticatedAdmin, UserSession.state)
        assertEquals(
            listOf(UserSession.State.AuthenticatedAdmin, UserSession.State.AuthenticatedAdmin),
            callbackStates
        )
        assertEquals(0, clearCacheCalls)
    }

    @Test
    fun bootstrapUsesResolvedStateCacheForSameAuthenticatedUser() {
        currentUid = "user-1"
        val callbackStates = mutableListOf<UserSession.State>()

        SessionBootstrap.bootstrap { callbackStates += it }
        pendingResolver!!.invoke(UserSession.State.AuthenticatedUser)

        SessionBootstrap.bootstrap { callbackStates += it }

        assertEquals(1, resolveCalls)
        assertEquals(UserSession.State.AuthenticatedUser, UserSession.state)
        assertEquals(
            listOf(UserSession.State.AuthenticatedUser, UserSession.State.AuthenticatedUser),
            callbackStates
        )
        assertEquals(0, clearCacheCalls)
    }

    @Test
    fun forceRefreshBypassesResolvedStateCache() {
        currentUid = "user-1"

        SessionBootstrap.bootstrap()
        pendingResolver!!.invoke(UserSession.State.AuthenticatedUser)

        SessionBootstrap.bootstrap(forceRefresh = true)

        assertEquals(2, resolveCalls)
        assertEquals(UserSession.State.AuthenticatedUser, UserSession.state)
    }

    @Test
    fun bootstrapLogsOutWhenResolvedUserChangesMidFlight() {
        currentUid = "user-1"
        var callbackState: UserSession.State? = null

        SessionBootstrap.bootstrap { callbackState = it }
        currentUid = "user-2"

        pendingResolver!!.invoke(UserSession.State.AuthenticatedAdmin)

        assertEquals(UserSession.State.LoggedOut, UserSession.state)
        assertEquals(UserSession.State.LoggedOut, callbackState)
        assertFalse(UserSession.isAuthenticated)
        assertEquals(1, clearCacheCalls)
    }
}
