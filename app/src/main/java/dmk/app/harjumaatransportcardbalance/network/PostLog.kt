package dmk.app.harjumaatransportcardbalance.network

import android.content.Context
import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Response
import com.android.volley.toolbox.HurlStack
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class PostLog {

    private val endpoint = "https://logs3.papertrailapp.com:23561"

    fun postLog(context: Context) {
        postLog(context, "")
    }

    fun postLog(context: Context, cardNumber: String) {
        val jsonObject = JSONObject()
        if (cardNumber.isNotEmpty()) {
            jsonObject.put("personal_code", cardNumber)
        } else {
            jsonObject.put("statistics", "new usage")
        }

        val jsonObjectRequest = object: JsonObjectRequest(
            Method.POST, endpoint, jsonObject,
            Response.Listener {},
            Response.ErrorListener {})
            {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val headers = HashMap<String, String>()
                    headers["Content-Type"] =  "application/json"
                    if (cardNumber.isNotEmpty()) {
                        headers["Ticket"] = cardNumber
                    } else {
                        headers["Stats"] = "new usage"
                    }
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

        jsonObjectRequest.retryPolicy = DefaultRetryPolicy(500,
            0, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT)
        queue.add(jsonObjectRequest)
    }
}