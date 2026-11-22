package app.project001.verification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Phone verification by call ("flash call").
 *
 * The server rings the user's number from a known verification number. The
 * app's call-screening service sees the call arrive, recognises the expected
 * caller, declines it silently and marks the number verified. Only a phone
 * whose SIM holds that number can receive the call, so this proves the SIM is
 * in this phone. Nothing to type, and an unanswered call costs nothing.
 *
 * Only the one expected call, within its time window, is ever declined. Every
 * other call is left alone.
 */
object CallVerification {

    sealed interface State {
        data object Idle : State
        data class Waiting(val expectedCaller: String, val deadlineMs: Long) : State
        data object Verified : State
        data object TimedOut : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    /** Start waiting for a call from [expectedCaller] for [timeoutMs]. */
    fun expectCall(expectedCaller: String, timeoutMs: Long = 60_000, now: Long = System.currentTimeMillis()) {
        _state.value = State.Waiting(expectedCaller, now + timeoutMs)
    }

    /**
     * Called by the screening service for every incoming call. Returns true
     * only for the expected verification call, which should then be declined.
     */
    fun onIncomingCall(number: String?, now: Long = System.currentTimeMillis()): Boolean {
        val waiting = _state.value as? State.Waiting ?: return false
        if (now > waiting.deadlineMs) {
            _state.value = State.TimedOut
            return false
        }
        if (!sameNumber(number, waiting.expectedCaller)) return false
        _state.value = State.Verified
        return true
    }

    fun timeOutIfExpired(now: Long = System.currentTimeMillis()) {
        val waiting = _state.value as? State.Waiting ?: return
        if (now > waiting.deadlineMs) _state.value = State.TimedOut
    }

    fun reset() {
        _state.value = State.Idle
    }

    /** Compares the last 10 digits, so "+91 99999 00001", "09999900001" and "9999900001" match. */
    fun sameNumber(a: String?, b: String?): Boolean {
        val da = a.orEmpty().filter(Char::isDigit).takeLast(10)
        val db = b.orEmpty().filter(Char::isDigit).takeLast(10)
        return da.length == 10 && da == db
    }
}
