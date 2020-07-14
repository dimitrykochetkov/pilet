package dmk.app.harjumaatransportcardbalance.network

import android.content.Context
import com.android.volley.*
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import dmk.app.harjumaatransportcardbalance.R
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class PostCardBalance {

    private val endpoint = "https://tallinn.pilet.ee/tickets/personalcode"
    private var cookie = ""

    private var watchDog = 0

    private lateinit var context: Context

    fun requestCardBalance(context: Context, cardId: String, callback: ResponseCallback) {
        this.context = context

        val jsonObject = JSONObject()

        try {
            jsonObject.put("personal_code", cardId)

            val jsonObjectRequest = object: JsonObjectRequest(Method.POST, endpoint, jsonObject,
                Response.Listener { response ->
                    parseResponseAndSendCallback(response, callback)
                    PostLog().postLog(context)
                },
                Response.ErrorListener { error ->
                    if (error is NoConnectionError || error is TimeoutError ||
                        error is NetworkError) {
                        callbackError(callback, Error.REQUEST_TIMEOUT)
                        return@ErrorListener
                    }

                    try {
                        cookie = error.networkResponse.headers.toMap()["Set-Cookie"].toString()
                        cookie = cookie.substring(0, cookie.indexOf(";"))

                        watchDog++
                        if (watchDog > 5) {
                            throw Exception()
                        }

                        requestCardBalance(context, cardId, callback)

                    } catch (ex: Exception) {
                        callbackError(callback, Error.UNKNOWN_ERROR)
                    }
                })
            {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Content-Type"] =  "application/json"
                    headers["Cookie"] = cookie
                    return headers
                }
            }

            val queue = Volley.newRequestQueue(context, object : HurlStack() {
                @Throws(IOException::class)
                override fun createConnection(url: URL?): HttpURLConnection? {
                    val connection: HttpURLConnection = super.createConnection(url)
                    connection.instanceFollowRedirects = false
                    return connection
                }
            })

            jsonObjectRequest.retryPolicy = DefaultRetryPolicy(10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
            queue.add(jsonObjectRequest)

        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    private fun parseResponseAndSendCallback(response: JSONObject, callback: ResponseCallback) {
        try {
            val balance = response.getJSONObject("balance")

            when (balance.getInt("status_code")) {
                200 -> {
                    val content: JSONArray = balance.getJSONArray("content")
                    val contentObject: JSONObject = content.optJSONObject(0)

                    callback.onSuccess(contentObject.optString("balance"))
                }

                409, 422 -> callbackError(callback, Error.INCORRECT_CARD_NUMBER)

                500, 503 -> callbackError(callback, Error.SERVER_UNAVAILABLE)

                else -> callback.onError(arrayOf(""))
            }
        } catch (e: JSONException) {
            callbackError(callback, Error.UNKNOWN_ERROR)
        }
    }

    private fun callbackError(callback: ResponseCallback, error: Error) {
        val args = when (error) {
            Error.INCORRECT_CARD_NUMBER -> arrayOf(
                context.getString(R.string.incorrect_card_number),
                context.getString(R.string.verify_card))

            Error.REQUEST_TIMEOUT -> arrayOf(
                context.getString(R.string.timeout),
                context.getString(R.string.check_connection))

            Error.SERVER_UNAVAILABLE -> arrayOf(
                context.getString(R.string.server_unavailable),
                context.getString(R.string.try_later))

            Error.UNKNOWN_ERROR -> arrayOf(
                context.getString(R.string.unknown_error),
                context.getString(R.string.sorry_not_sorry))
        }

        callback.onError(args)
    }
}