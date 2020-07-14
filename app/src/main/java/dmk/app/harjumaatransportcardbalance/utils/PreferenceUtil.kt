package dmk.app.harjumaatransportcardbalance.utils

import android.app.Activity
import android.content.Context

class PreferenceUtil {

    fun getPref(activity: Activity, key: String): Boolean {
        return activity.getPreferences(Context.MODE_PRIVATE).getBoolean(key, false)
    }

    fun setPref(activity: Activity, key: String, value: Boolean) {
        val sharedPref = activity.getPreferences(Context.MODE_PRIVATE) ?: return
        with (sharedPref.edit()) {
            putBoolean(key, value)
            commit()
        }
    }
}