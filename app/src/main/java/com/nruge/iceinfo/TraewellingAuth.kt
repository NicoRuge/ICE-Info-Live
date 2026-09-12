package com.nruge.iceinfo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import com.nruge.iceinfo.model.TraewellingAccount
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * OAuth2-Anbindung an Träwelling (Laravel Passport) per Authorization-Code-Flow
 * mit PKCE — public client, kein Secret. Token liegen in einer app-privaten
 * Prefs-Datei; der Verbindungsstatus wird als [account]-StateFlow veröffentlicht.
 *
 * Redirect läuft über https://api.iceinfo.de/callback (Träwelling verlangt https),
 * das serverseitig per 302 auf den App-Deep-Link iceinfo://traewelling/callback
 * weiterleitet (siehe [TraewellingCallbackActivity]).
 */
object TraewellingAuth {

    const val CLIENT_ID = "375"
    const val REDIRECT_URI = "https://api.iceinfo.de/callback"
    private const val AUTHORIZE_URL = "https://traewelling.de/oauth/authorize"
    private const val TOKEN_URL = "https://traewelling.de/oauth/token"
    private const val USER_URL = "https://traewelling.de/api/v1/auth/user"
    private const val SCOPES = "write-statuses"

    val USER_AGENT = "ICEInfo/${BuildConfig.VERSION_NAME} (https://github.com/NicoRuge/ICE-Info-Live)"

    private const val PREFS = "traewelling_auth"
    private const val KEY_ACCESS = "access_token"
    private const val KEY_REFRESH = "refresh_token"
    private const val KEY_EXPIRES_AT = "expires_at"
    private const val KEY_USERNAME = "username"
    private const val KEY_VERIFIER = "pkce_verifier"
    private const val KEY_STATE = "oauth_state"

    private val json = Json { ignoreUnknownKeys = true }

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(json) }
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 8000
            socketTimeoutMillis = 15000
        }
        defaultRequest {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, "application/json")
        }
    }

    private val _account = MutableStateFlow<TraewellingAccount?>(null)
    val account: StateFlow<TraewellingAccount?> = _account.asStateFlow()

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Beim App-Start aufrufen: lädt den gespeicherten Verbindungszustand. */
    fun init(context: Context) {
        val p = prefs(context)
        _account.value = if (p.getString(KEY_ACCESS, null) != null) {
            TraewellingAccount(p.getString(KEY_USERNAME, null))
        } else null
    }

    /** Baut den Browser-Intent zum Authorize-Endpoint (erzeugt PKCE-Verifier + State). */
    fun buildAuthorizeIntent(context: Context): Intent {
        val verifier = randomUrlSafe(64)
        val state = randomUrlSafe(24)
        prefs(context).edit()
            .putString(KEY_VERIFIER, verifier)
            .putString(KEY_STATE, state)
            .apply()
        val url = Uri.parse(AUTHORIZE_URL).buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("scope", SCOPES)
            .appendQueryParameter("state", state)
            .appendQueryParameter("code_challenge", s256(verifier))
            .appendQueryParameter("code_challenge_method", "S256")
            .build()
        return Intent(Intent.ACTION_VIEW, url).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    /** Verarbeitet den Redirect: prüft State, tauscht Code gegen Token. true = verbunden. */
    suspend fun handleRedirect(context: Context, code: String, state: String?): Boolean =
        withContext(Dispatchers.IO) {
            val p = prefs(context)
            val savedState = p.getString(KEY_STATE, null)
            val verifier = p.getString(KEY_VERIFIER, null)
            if (verifier == null || savedState == null || state != savedState) {
                Log.w("TraewellingAuth", "State/Verifier stimmen nicht überein")
                return@withContext false
            }
            try {
                val token: TokenResponse = client.submitForm(
                    url = TOKEN_URL,
                    formParameters = Parameters.build {
                        append("grant_type", "authorization_code")
                        append("client_id", CLIENT_ID)
                        append("redirect_uri", REDIRECT_URI)
                        append("code_verifier", verifier)
                        append("code", code)
                    }
                ).body()
                storeToken(context, token)
                val username = fetchUsername(token.accessToken)
                p.edit()
                    .putString(KEY_USERNAME, username)
                    .remove(KEY_VERIFIER)
                    .remove(KEY_STATE)
                    .apply()
                _account.value = TraewellingAccount(username)
                true
            } catch (e: Exception) {
                Log.w("TraewellingAuth", "Token-Tausch fehlgeschlagen: ${e.message}")
                false
            }
        }

    /** Verbindung trennen: lokale Token verwerfen. */
    fun disconnect(context: Context) {
        prefs(context).edit().clear().apply()
        _account.value = null
    }

    /**
     * Liefert einen gültigen Access-Token — refresht bei (nahendem) Ablauf automatisch.
     * null, wenn keine Verbindung besteht. Von API-Aufrufen (z. B. Check-in) zu nutzen.
     */
    suspend fun validAccessToken(context: Context): String? = withContext(Dispatchers.IO) {
        val p = prefs(context)
        val access = p.getString(KEY_ACCESS, null) ?: return@withContext null
        val refresh = p.getString(KEY_REFRESH, null)
        val expiresAt = p.getLong(KEY_EXPIRES_AT, 0L)
        // 60-Sekunden-Puffer, damit ein Aufruf nicht mitten im Ablauf scheitert
        if (System.currentTimeMillis() < expiresAt - 60_000L || refresh.isNullOrBlank()) {
            return@withContext access
        }
        try {
            val token: TokenResponse = client.submitForm(
                url = TOKEN_URL,
                formParameters = Parameters.build {
                    append("grant_type", "refresh_token")
                    append("refresh_token", refresh)
                    append("client_id", CLIENT_ID)
                    append("scope", SCOPES)
                }
            ).body()
            storeToken(context, token)
            token.accessToken
        } catch (e: Exception) {
            Log.w("TraewellingAuth", "Token-Refresh fehlgeschlagen: ${e.message}")
            access // transienter Fehler → alten Token zurückgeben (Aufrufer behandelt evtl. 401)
        }
    }

    private fun storeToken(context: Context, t: TokenResponse) {
        val edit = prefs(context).edit()
            .putString(KEY_ACCESS, t.accessToken)
            .putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + t.expiresIn * 1000L)
        // Refresh-Token nur überschreiben, wenn der Server ein neues liefert
        if (t.refreshToken.isNotBlank()) edit.putString(KEY_REFRESH, t.refreshToken)
        edit.apply()
    }

    private suspend fun fetchUsername(accessToken: String): String? = try {
        val resp: UserResponse = client.get(USER_URL) {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }.body()
        resp.data.displayName?.takeIf { it.isNotBlank() } ?: resp.data.username
    } catch (e: Exception) {
        null
    }

    private fun randomUrlSafe(numBytes: Int): String {
        val bytes = ByteArray(numBytes)
        SecureRandom().nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    private fun s256(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    @Serializable
    private data class TokenResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String = "",
        @SerialName("expires_in") val expiresIn: Long = 0,
        @SerialName("token_type") val tokenType: String = "Bearer"
    )

    @Serializable
    private data class UserResponse(val data: UserData)

    @Serializable
    private data class UserData(
        val username: String? = null,
        @SerialName("displayName") val displayName: String? = null
    )
}
