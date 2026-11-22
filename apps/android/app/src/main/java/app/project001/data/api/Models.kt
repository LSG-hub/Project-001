package app.project001.data.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// The backend's JSON, as documented in services/api (see /docs on a local run).

@Serializable
data class StartVerification(
    val phone: String,
    val method: String = "voice_code",
    val language: String? = null,
)

@Serializable
data class StartedVerification(
    val id: String,
    val method: String,
    val status: String,
    @SerialName("expires_at") val expiresAt: String,
    @SerialName("call_to") val callTo: String? = null,
)

@Serializable
data class CodeBody(val code: String)

@Serializable
data class VerificationResult(val id: String, val status: String, val token: String? = null)

@Serializable
data class Me(val uid: String, val phone: String)

@Serializable
data class Profile(
    val name: String? = null,
    val about: String = "",
    val timezone: String = "Asia/Kolkata",
    val voice: String? = null,
)

@Serializable
data class ExpectedCall(val id: String, val text: String, @SerialName("expires_at") val expiresAt: String)

@Serializable
data class NewExpectedCall(val text: String, val hours: Double)

@Serializable
data class Turn(val speaker: String, val text: String, val at: Double, val interrupted: Boolean = false) {
    val fromAssistant: Boolean get() = speaker == "assistant"
}

@Serializable
data class CallSummary(
    val summary: String,
    val caller: String? = null,
    val purpose: String,
    val category: String,
    @SerialName("scam_risk") val scamRisk: String,
    @SerialName("scam_signals") val scamSignals: List<String> = emptyList(),
    @SerialName("action_items") val actionItems: List<String> = emptyList(),
    val language: String,
)

@Serializable
data class Call(
    val id: String,
    val source: String,
    @SerialName("caller_number") val callerNumber: String? = null,
    val voice: String? = null,
    @SerialName("started_at") val startedAt: String,
    @SerialName("duration_seconds") val durationSeconds: Double,
    @SerialName("ended_by") val endedBy: String,
    @SerialName("end_reason") val endReason: String? = null,
    val turns: List<Turn> = emptyList(),
    val summary: CallSummary? = null,
)

@Serializable
data class Voice(val name: String, val style: String, val gender: String, val default: Boolean = false)

@Serializable
data class ApiErrorBody(val error: ApiErrorDetail)

@Serializable
data class ApiErrorDetail(val code: String, val message: String)
