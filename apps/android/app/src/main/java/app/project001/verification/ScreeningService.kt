package app.project001.verification

import android.telecom.Call
import android.telecom.CallScreeningService

/**
 * Bound by Android for incoming calls once the app holds the call-screening
 * role (Android shows it calls from numbers that aren't in the user's
 * contacts). Today it only handles phone verification; later it will also
 * send unknown callers to the assistant.
 */
class ScreeningService : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        val incoming = callDetails.callDirection == Call.Details.DIRECTION_INCOMING
        val number = callDetails.handle?.schemeSpecificPart
        val isVerificationCall = incoming && CallVerification.onIncomingCall(number)

        val response = if (isVerificationCall) {
            CallResponse.Builder()
                .setDisallowCall(true)
                .setRejectCall(true)
                .setSkipCallLog(true)
                .setSkipNotification(true)
                .build()
        } else {
            // Leave every other call exactly as it is.
            CallResponse.Builder().build()
        }
        respondToCall(callDetails, response)
    }
}
