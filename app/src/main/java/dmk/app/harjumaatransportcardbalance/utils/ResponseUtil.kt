package dmk.app.harjumaatransportcardbalance.utils

import org.json.JSONArray
import org.json.JSONObject

class ResponseUtil {

    companion object {
        fun parseResp(response: JSONObject): String {
            val balance = response.getJSONObject("balance")
            val content: JSONArray = balance.getJSONArray("content")
            val contentObject0: JSONObject = content.optJSONObject(0)

            val contentArray = response.getJSONObject("tickets").getJSONArray("content")
            if (contentArray.length() == 0) {
                return "0"
            }

            val contentObject1: JSONObject = contentArray.optJSONObject(0)

            val optString = contentObject1.optString("product_type")
            if (optString.contains("H")) {
                val start = parseTime(contentObject1.optString("starting"))
                val end = parseTime(contentObject1.optString("ending"))
                return "$start|$end"
            }

            return contentObject0.optString("balance")
        }

        private fun parseTime(time: String): String {
            val t = time.replace("T", " ")
            return t.substring(0, t.indexOf("."))
        }
    }
}