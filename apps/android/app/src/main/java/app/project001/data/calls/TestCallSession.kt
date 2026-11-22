package app.project001.data.calls

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import android.os.Build
import app.project001.data.api.ApiClient
import app.project001.data.api.Call
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/** One line of the live transcript. Fragments from the same speaker join one line. */
data class LiveLine(val fromAssistant: Boolean, val text: String, val cutOff: Boolean = false)

data class TestCallState(
    val phase: Phase = Phase.Idle,
    val lines: List<LiveLine> = emptyList(),
    val note: String? = null,
    val speaker: Boolean = true,
    val call: Call? = null,
    val error: String? = null,
) {
    enum class Phase { Idle, Connecting, Live, Ending, Done, Failed }
}

/**
 * A test call to your own assistant, over the backend's WebSocket (/v1/calls/test):
 * the microphone streams up as 16 kHz PCM, the assistant's voice comes back as 24 kHz PCM,
 * and JSON events carry the transcript and, at the end, the saved call with its summary.
 */
class TestCallSession(private val context: Context, private val api: ApiClient) {
    private val _state = MutableStateFlow(TestCallState())
    val state: StateFlow<TestCallState> = _state.asStateFlow()

    private var socket: WebSocket? = null
    private var recorder: AudioRecord? = null
    private var micThread: Thread? = null
    private var player: AudioTrack? = null
    private var playerThread: Thread? = null
    private val playback = LinkedBlockingQueue<ByteArray>()
    @Volatile private var running = false
    private val audio = context.getSystemService(AudioManager::class.java)

    suspend fun start(callerNumber: String?) {
        if (_state.value.phase in setOf(TestCallState.Phase.Connecting, TestCallState.Phase.Live)) return
        _state.value = TestCallState(phase = TestCallState.Phase.Connecting, speaker = _state.value.speaker)
        val auth = api.authHeader()
        if (auth == null) {
            fail("Sign in first.")
            return
        }
        val url = buildString {
            append(api.baseUrl.trimEnd('/').replaceFirst("http", "ws"))
            append("/v1/calls/test")
            if (!callerNumber.isNullOrBlank()) append("?caller=").append(java.net.URLEncoder.encode(callerNumber.trim(), "UTF-8"))
        }
        val request = Request.Builder().url(url).header("Authorization", auth).build()
        val http = api.http.newBuilder().readTimeout(0, TimeUnit.MILLISECONDS).pingInterval(20, TimeUnit.SECONDS).build()
        running = true
        startPlayer()
        socket = http.newWebSocket(request, Listener())
    }

    /** Hang up. The call's summary arrives a few seconds later. */
    fun hangUp() {
        if (_state.value.phase != TestCallState.Phase.Live) return
        socket?.send("""{"type":"end"}""")
        stopMic()
        _state.update { it.copy(phase = TestCallState.Phase.Ending) }
    }

    fun setSpeaker(on: Boolean) {
        _state.update { it.copy(speaker = on) }
        routeAudio(on)
    }

    fun reset() {
        close()
        _state.value = TestCallState(speaker = _state.value.speaker)
    }

    fun close() {
        running = false
        stopMic()
        socket?.cancel()
        socket = null
        playback.clear()
        playerThread?.interrupt()
        player?.run { runCatching { stop() }; release() }
        player = null
        audio.mode = AudioManager.MODE_NORMAL
        if (Build.VERSION.SDK_INT >= 31) audio.clearCommunicationDevice()
    }

    private inner class Listener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) = Unit

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            playback.offer(bytes.toByteArray())
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            val event = runCatching { api.json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return
            handle(event)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (_state.value.phase != TestCallState.Phase.Done) fail("The call dropped: ${t.message ?: "connection lost"}")
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            stopMic()
        }
    }

    private fun handle(event: JsonObject) {
        fun str(key: String) = event[key]?.jsonPrimitive?.contentOrNull
        when (str("type")) {
            "call_started" -> {
                _state.update { it.copy(phase = TestCallState.Phase.Live) }
                startMic()
                routeAudio(_state.value.speaker)
            }
            "assistant_said" -> str("text")?.let { add(true, it) }
            "caller_said" -> str("text")?.let { add(false, it) }
            "interrupted" -> {
                playback.clear()
                player?.run { pause(); flush(); play() }
                _state.update { s ->
                    val last = s.lines.lastOrNull()
                    if (last?.fromAssistant == true) s.copy(lines = s.lines.dropLast(1) + last.copy(cutOff = true)) else s
                }
            }
            "tool_call" -> if (str("name") == "end_call") _state.update { it.copy(note = "The assistant is ending the call.") }
            "call_ending" -> {
                stopMic()
                _state.update { it.copy(phase = TestCallState.Phase.Ending) }
            }
            "call_ended" -> {
                val call = event["call"]?.let { runCatching { api.json.decodeFromJsonElement(Call.serializer(), it) }.getOrNull() }
                _state.update { it.copy(phase = TestCallState.Phase.Done, call = call) }
                socket?.close(1000, null)
            }
            "error" -> fail(str("message") ?: "Something went wrong.")
        }
    }

    private fun add(fromAssistant: Boolean, fragment: String) {
        _state.update { s ->
            val last = s.lines.lastOrNull()
            if (last != null && last.fromAssistant == fromAssistant && !last.cutOff) {
                s.copy(lines = s.lines.dropLast(1) + last.copy(text = last.text + fragment))
            } else {
                s.copy(lines = s.lines + LiveLine(fromAssistant, fragment.trimStart()))
            }
        }
    }

    private fun fail(message: String) {
        stopMic()
        _state.update { it.copy(phase = TestCallState.Phase.Failed, error = message) }
    }

    // --- audio ---------------------------------------------------------------------

    @SuppressLint("MissingPermission") // the screen asks for RECORD_AUDIO before starting
    private fun startMic() {
        if (recorder != null) return
        audio.mode = AudioManager.MODE_IN_COMMUNICATION
        val min = AudioRecord.getMinBufferSize(IN_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val rec = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION, // echo cancellation and noise suppression
            IN_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(min, FRAME_BYTES * 4),
        )
        if (AcousticEchoCanceler.isAvailable()) AcousticEchoCanceler.create(rec.audioSessionId)?.enabled = true
        if (NoiseSuppressor.isAvailable()) NoiseSuppressor.create(rec.audioSessionId)?.enabled = true
        recorder = rec
        rec.startRecording()
        micThread = Thread {
            val frame = ByteArray(FRAME_BYTES) // 40 ms, sent continuously like a phone line
            while (running && recorder === rec) {
                val n = rec.read(frame, 0, frame.size)
                if (n > 0) socket?.send(frame.copyOf(n).toByteString())
            }
        }.apply { name = "test-call-mic"; start() }
    }

    private fun stopMic() {
        val rec = recorder ?: return
        recorder = null
        runCatching { rec.stop() }
        rec.release()
        micThread = null
    }

    private fun startPlayer() {
        val min = AudioTrack.getMinBufferSize(OUT_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder().setSampleRate(OUT_RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build(),
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(min * 4)
            .build()
        player = track
        track.play()
        playerThread = Thread {
            try {
                while (running) {
                    val chunk = playback.poll(200, TimeUnit.MILLISECONDS) ?: continue
                    player?.write(chunk, 0, chunk.size)
                }
            } catch (_: InterruptedException) {
            }
        }.apply { name = "test-call-speaker"; start() }
    }

    private fun routeAudio(speaker: Boolean) {
        if (Build.VERSION.SDK_INT >= 31) {
            val type = if (speaker) AudioDeviceInfo.TYPE_BUILTIN_SPEAKER else AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            audio.availableCommunicationDevices.firstOrNull { it.type == type }?.let { audio.setCommunicationDevice(it) }
        } else {
            @Suppress("DEPRECATION")
            audio.isSpeakerphoneOn = speaker
        }
    }

    private companion object {
        const val IN_RATE = 16_000
        const val OUT_RATE = 24_000
        const val FRAME_BYTES = IN_RATE / 25 * 2 // 40 ms of 16-bit mono
    }
}
