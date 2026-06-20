package com.example.data.remote

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the NikhatGlow [NikhatGlowApi]. Singleton per process; the active token is
 * read fresh from [TokenStore] on every request so a role switch / refresh
 * takes effect immediately without rebuilding the client.
 *
 * BASE_URL defaults to production. For a local backend on the Android
 * emulator use http://10.0.2.2:8000/ (10.0.2.2 = host loopback); set it via
 * [NetworkConfig.baseUrl] before first use, or build a debug variant.
 */
object NetworkConfig {
    // Trailing slash required by Retrofit. 10.0.2.2 maps to the dev host from
    // the emulator; flip to your prod host for release builds.
    @Volatile
    var baseUrl: String = "https://odiobook.com/api/nikhatglow/v1/"
}

class ApiClient private constructor(private val tokenStore: TokenStore) {

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val authInterceptor = Interceptor { chain ->
        val token = tokenStore.accessToken()
        val req = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }
        chain.proceed(req)
    }

    // On 401, refresh the active role's token once (synchronous, OkHttp
    // Authenticator contract) and retry. §705: this is NON-DESTRUCTIVE — a
    // failed refresh only fails THIS request; it must NEVER clear the saved
    // session. Previously clearRole() here meant a single transient 401 /
    // network blip permanently logged the user out (the founder's "logs out on
    // every app close"). The session is now cleared ONLY on an explicit user
    // logout (Repository.logout) so login survives app restarts for months.
    private val tokenAuthenticator = Authenticator { _: Route?, response: Response ->
        val role = tokenStore.activeRole ?: return@Authenticator null
        val refresh = tokenStore.refreshToken(role) ?: return@Authenticator null
        // Avoid infinite loops: only retry once.
        if (responseCount(response) >= 2) {
            return@Authenticator null
        }
        val newAccess = synchronized(this) {
            // Re-check: another thread may have refreshed already.
            val current = tokenStore.accessToken(role)
            val failed = response.request.header("Authorization")
            if (current != null && "Bearer $current" != failed) {
                current
            } else {
                refreshBlocking(refresh, role)
            }
        } ?: return@Authenticator null
        response.request.newBuilder()
            .header("Authorization", "Bearer $newAccess")
            .build()
    }

    private fun refreshBlocking(refreshToken: String, role: String): String? {
        return try {
            val body = JSONObject().put("refresh_token", refreshToken).toString()
                .toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url(NetworkConfig.baseUrl + "auth/refresh")
                .post(body)
                .build()
            bareClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                val json = JSONObject(resp.body?.string() ?: return null)
                val access = json.optString("access_token", "")
                val newRefresh = json.optString("refresh_token", "")
                if (access.isEmpty()) return null
                tokenStore.updateAccess(role, access, newRefresh.ifEmpty { null })
                access
            }
        } catch (e: Exception) {
            null
        }
    }

    // Plain client (no auth/authenticator) used only for the refresh call.
    private val bareClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val okHttp = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        })
        .authenticator(tokenAuthenticator)
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .build()

    val api: NikhatGlowApi = Retrofit.Builder()
        .baseUrl(NetworkConfig.baseUrl)
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()
        .create(NikhatGlowApi::class.java)

    private fun responseCount(response: Response): Int {
        var r: Response? = response
        var count = 1
        while (r?.priorResponse != null) {
            count++
            r = r.priorResponse
        }
        return count
    }

    companion object {
        @Volatile
        private var instance: ApiClient? = null

        fun get(context: Context): ApiClient =
            instance ?: synchronized(this) {
                instance ?: ApiClient(TokenStore(context)).also { instance = it }
            }
    }
}
