package app.project001.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

/** A failure the backend explained (its {"error": {"code", "message"}}), or a network problem. */
class ApiException(val code: String, message: String, val status: Int = 0) : Exception(message)

/** Talks to the backend. Every request carries the signed-in user's Firebase ID token. */
class ApiClient(
    val baseUrl: String,
    val http: OkHttpClient,
    private val idToken: suspend () -> String?,
) {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    suspend fun <T> get(path: String, out: KSerializer<T>): T = send(request(path).get(), out)

    suspend fun <B, T> post(path: String, body: B, bodyType: KSerializer<B>, out: KSerializer<T>): T =
        send(request(path).post(body(body, bodyType)), out)

    suspend fun <T> put(path: String, body: JsonElement, out: KSerializer<T>): T =
        send(request(path).put(json.encodeToString(JsonElement.serializer(), body).toRequestBody(JSON)), out)

    suspend fun delete(path: String) {
        send<Unit>(request(path).delete(), null)
    }

    /** Headers for calls made outside this client (the test-call WebSocket). */
    suspend fun authHeader(): String? = idToken()?.let { "Bearer $it" }

    private fun <B> body(value: B, type: KSerializer<B>): RequestBody = json.encodeToString(type, value).toRequestBody(JSON)

    private fun request(path: String) = Request.Builder().url(baseUrl.trimEnd('/') + path)

    private suspend fun <T> send(builder: Request.Builder, out: KSerializer<T>?): T {
        authHeader()?.let { builder.header("Authorization", it) }
        val request = builder.build()
        return withContext(Dispatchers.IO) {
            val response = try {
                http.newCall(request).execute()
            } catch (e: IOException) {
                throw ApiException("network", "Can't reach the server. Check your connection.")
            }
            response.use {
                val text = it.body.string()
                if (!it.isSuccessful) {
                    val detail = runCatching { json.decodeFromString(ApiErrorBody.serializer(), text).error }.getOrNull()
                    throw ApiException(detail?.code ?: "http_${it.code}", detail?.message ?: "Something went wrong (${it.code}).", it.code)
                }
                @Suppress("UNCHECKED_CAST")
                if (out == null) Unit as T else json.decodeFromString(out, text)
            }
        }
    }

    private companion object {
        val JSON = "application/json".toMediaType()
    }
}
