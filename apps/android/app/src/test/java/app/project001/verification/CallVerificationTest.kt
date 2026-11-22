package app.project001.verification

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CallVerificationTest {

    @After fun tearDown() = CallVerification.reset()

    @Test fun `matches numbers written differently`() {
        assertTrue(CallVerification.sameNumber("+91 99999 00001", "09999900001"))
        assertTrue(CallVerification.sameNumber("9999900001", "+919999900001"))
        assertFalse(CallVerification.sameNumber("+919999900002", "+919999900001"))
        assertFalse(CallVerification.sameNumber("12345", "12345"))
        assertFalse(CallVerification.sameNumber(null, "+919999900001"))
    }

    @Test fun `declines only the expected call`() {
        CallVerification.expectCall("+919999900001", timeoutMs = 60_000, now = 0)
        assertFalse(CallVerification.onIncomingCall("+919876543210", now = 1_000))
        assertTrue(CallVerification.state.value is CallVerification.State.Waiting)
        assertTrue(CallVerification.onIncomingCall("9999900001", now = 2_000))
        assertEquals(CallVerification.State.Verified, CallVerification.state.value)
    }

    @Test fun `ignores calls when not waiting`() {
        assertFalse(CallVerification.onIncomingCall("+919999900001"))
        assertEquals(CallVerification.State.Idle, CallVerification.state.value)
    }

    @Test fun `times out after the window`() {
        CallVerification.expectCall("+919999900001", timeoutMs = 60_000, now = 0)
        assertFalse(CallVerification.onIncomingCall("+919999900001", now = 61_000))
        assertEquals(CallVerification.State.TimedOut, CallVerification.state.value)
    }
}
