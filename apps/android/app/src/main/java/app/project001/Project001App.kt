package app.project001

import android.app.Application
import android.content.Context
import app.project001.data.CallsRepository
import app.project001.data.ProfileRepository
import app.project001.data.SignInRepository
import app.project001.data.api.ApiClient
import app.project001.data.auth.AuthRepository
import app.project001.data.calls.TestCallSession
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** The app's objects, created once. Screens get them from [graph]. */
class AppGraph(context: Context) {
    val auth = AuthRepository(context)
    private val http = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    val api = ApiClient(BuildConfig.API_BASE_URL, http, auth::idToken)
    val signIn = SignInRepository(api, auth)
    val calls = CallsRepository(api)
    val profile = ProfileRepository(api)
    val testCall = TestCallSession(context, api)
}

class Project001App : Application() {
    lateinit var graph: AppGraph
        private set

    override fun onCreate() {
        super.onCreate()
        graph = AppGraph(this)
    }
}

val Context.graph: AppGraph get() = (applicationContext as Project001App).graph
