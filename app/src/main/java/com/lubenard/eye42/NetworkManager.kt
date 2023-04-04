package com.lubenard.eye42

import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.*
import org.json.JSONObject
import java.util.*

enum class ConnectionStatus {
    NOT_CONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR_CONNECTING
}

data class TokenInfo(val token: String, val tokenType: String, val tokenCreatedDate: Long, val tokenExpiration: Long)

class NetworkManager(context: Context)  {

    private val TAG = this::class.simpleName

    private val clientUid = BuildConfig.CLIENT_UID
    private val clientSecret = BuildConfig.CLIENT_SECRET
    private val apiBaseUrl = "https://api.intra.42.fr/v2"

    private val queue: RequestQueue

    private var tokenInfo: TokenInfo? = null

    val connectionStatus = MutableLiveData(ConnectionStatus.NOT_CONNECTED)

    init {
        queue = Volley.newRequestQueue(context)
        queue.addRequestEventListener { request, event ->
            Log.d(TAG, "Request $request -> Lifecycle $event")
        }
    }

    fun getServerResponse(requestMethod: Int,
                          endPoint: String? = null,
                          params: Map<String, Any>? = null,
                          successCallback: ((response: JSONObject) -> Unit)? = null,
                          errorCallback: (() -> Unit)? = null
    ) {
        var jsonData: JSONObject? = null
        if (params != null && requestMethod == Request.Method.POST) {
            jsonData = JSONObject()
            params.forEach {
                when (it.value) {
                    is Int -> jsonData.put(it.key, it.value as Int)
                    is Double -> jsonData.put(it.key, it.value as Double)
                    is String -> jsonData.put(it.key, it.value as String)
                    is Boolean -> jsonData.put(it.key, it.value as Boolean)
                }
            }
        }

        val stringRequest = object: JsonObjectRequest(
            requestMethod,
            apiBaseUrl + endPoint,
            jsonData,
            { response ->
                successCallback?.invoke(response)
            },
            { error ->
                Log.e(TAG, "Error from volley ! ${error.message}")
                errorCallback?.invoke()
            }) {
            override fun getHeaders(): MutableMap<String, String> {
                val headers = HashMap<String, String>()
                if (tokenInfo != null) {
                    Log.v(TAG, "tokenInfo is not null !")
                    headers["Authorization"] = "${tokenInfo!!.tokenType} ${tokenInfo!!.token}"
                }
                return headers
            }
        }

        // Add the request to the RequestQueue.
        queue.add(stringRequest)
    }

    fun connectToApi(context: Context) {
        val tokenEndpoint = "/oauth/token"
        val params = mapOf<String, Any>(
            Pair("grant_type", "client_credentials"),
            Pair("client_id", clientUid),
            Pair("client_secret", clientSecret)
        )
        connectionStatus.value = ConnectionStatus.CONNECTING
        getServerResponse(Request.Method.POST, tokenEndpoint, params, successCallback =  {
            Log.d(TAG, "Response from server is $it")
            if (it.has("access_token") && it.has("expires_in") && it.has("created_at") && it.has("token_type")) {
                tokenInfo = TokenInfo(
                    it.getString("access_token"),
                    it.getString("token_type"),
                    it.getLong("expires_in"),
                    it.getLong("created_at")
                )
                val sharedPreference = context.getSharedPreferences("Eye42Preferences", Context.MODE_PRIVATE)
                sharedPreference.edit()
                    .putString("access_token", tokenInfo!!.token)
                    .putString("token_type", tokenInfo!!.tokenType)
                    .putLong("token_expiration", tokenInfo!!.tokenExpiration)
                    .putLong("token_creation_date", tokenInfo!!.tokenCreatedDate)
                    .apply()
                connectionStatus.value = ConnectionStatus.CONNECTED
            } else {
                connectionStatus.value = ConnectionStatus.ERROR_CONNECTING
                Log.d(TAG, "Token does not possess all required fields !!")
            }
        },
        errorCallback = {
            connectionStatus.value = ConnectionStatus.ERROR_CONNECTING
            Log.d(TAG, "Error while getting token !")
        })
    }

    fun setToken(token: TokenInfo) {
        this.tokenInfo = token
        connectionStatus.value = ConnectionStatus.CONNECTED
    }
}