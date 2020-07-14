package dmk.app.harjumaatransportcardbalance.utils

import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.RelativeSizeSpan

object Formatter {

    fun formatText(txt: String): Spannable {
        val text = txt.replace("\n", "\n\u0000")
        val spannable: Spannable = SpannableString(text)
        for (i in 0 until text.length - 1) {
            if (text[i] == '\n') {
                spannable.setSpan(
                    RelativeSizeSpan(1.5f),
                    i + 1,
                    i + 2,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
        }

        return spannable
    }
}