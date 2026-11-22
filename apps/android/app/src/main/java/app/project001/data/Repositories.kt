package app.project001.data

import app.project001.data.api.ApiClient
import app.project001.data.api.Call
import app.project001.data.api.CodeBody
import app.project001.data.api.ExpectedCall
import app.project001.data.api.Me
import app.project001.data.api.NewExpectedCall
import app.project001.data.api.Profile
import app.project001.data.api.StartVerification
import app.project001.data.api.StartedVerification
import app.project001.data.api.VerificationResult
import app.project001.data.api.Voice
import app.project001.data.auth.AuthRepository
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/** Proving the phone number, then signing in. */
class SignInRepository(private val api: ApiClient, private val auth: AuthRepository) {
    /** Asks the backend to call [phoneE164] and read a code. */
    suspend fun start(phoneE164: String, language: String?): StartedVerification =
        api.post("/v1/verifications", StartVerification(phoneE164, language = language), StartVerification.serializer(), StartedVerification.serializer())

    /** Checks the code; on success the user is signed in. */
    suspend fun submitCode(verificationId: String, code: String) {
        val result = api.post("/v1/verifications/$verificationId/code", CodeBody(code), CodeBody.serializer(), VerificationResult.serializer())
        auth.signIn(result.token ?: error("The server didn't send a sign-in token."))
    }

    suspend fun me(): Me = api.get("/v1/me", Me.serializer())

    fun signOut() = auth.signOut()
}

class CallsRepository(private val api: ApiClient) {
    suspend fun recent(): List<Call> = api.get("/v1/calls?limit=50", ListSerializer(Call.serializer()))

    suspend fun get(id: String): Call = api.get("/v1/calls/$id", Call.serializer())
}

/** What the assistant knows about you: profile, voice, calls you're expecting. */
class ProfileRepository(private val api: ApiClient) {
    suspend fun profile(): Profile = api.get("/v1/context/profile", Profile.serializer())

    /** Changes only the fields given (null = leave as is; "" clears name or voice). */
    suspend fun update(name: String? = null, about: String? = null, voice: String? = null): Profile {
        val body = buildJsonObject {
            name?.let { put("name", if (it.isBlank()) JsonNull else JsonPrimitive(it.trim())) }
            about?.let { put("about", JsonPrimitive(it.trim())) }
            voice?.let { put("voice", if (it.isBlank()) JsonNull else JsonPrimitive(it)) }
        }
        return api.put("/v1/context/profile", body, Profile.serializer())
    }

    suspend fun expected(): List<ExpectedCall> = api.get("/v1/context/expected", ListSerializer(ExpectedCall.serializer()))

    suspend fun addExpected(text: String, hours: Double): ExpectedCall =
        api.post("/v1/context/expected", NewExpectedCall(text.trim(), hours), NewExpectedCall.serializer(), ExpectedCall.serializer())

    suspend fun removeExpected(id: String) = api.delete("/v1/context/expected/$id")

    suspend fun voices(): List<Voice> = api.get("/v1/voices", ListSerializer(Voice.serializer()))
}
