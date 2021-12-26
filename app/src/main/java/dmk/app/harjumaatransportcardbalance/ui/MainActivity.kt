package dmk.app.harjumaatransportcardbalance.ui

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.zxing.ResultPoint
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CaptureManager
import dmk.app.harjumaatransportcardbalance.R
import dmk.app.harjumaatransportcardbalance.network.PostCardBalance
import dmk.app.harjumaatransportcardbalance.network.PostLog
import dmk.app.harjumaatransportcardbalance.network.ResponseCallback
import dmk.app.harjumaatransportcardbalance.utils.Formatter
import dmk.app.harjumaatransportcardbalance.utils.PreferenceUtil
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.layout_send_logs.*
import java.util.*


class MainActivity : BaseActivity(), ResponseCallback {

    private enum class ContainerState {
        INITIAL, WAITING, SUCCESS, ERROR
    }

    private var scanFlag = true
    private lateinit var timestamp: Date
    private var lastScannedNumber = ""
    private var resIdLayoutButton = -1

    private lateinit var captureManager: CaptureManager

    private lateinit var bBalance: MaterialButton
    private lateinit var bLogs: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bBalance = findViewById(R.id.bCheckBalance)

        redrawContainer(ContainerState.INITIAL, arrayOf(""))

        captureManager = CaptureManager(this, bvScan)
        captureManager.initializeFromIntent(intent, savedInstanceState)

        bvScan.decodeContinuous(object: BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                result?.let {
                    if (scanFlag) {
                        etCardCode.setText(it.text)

                        if (it.text.length >= 5 && (!isTrembling() || lastScannedNumber != it.text)) {
                            postRequest()
                            lastScannedNumber = it.text
                        }
                    }
                }
            }

            override fun possibleResultPoints(resultPoints: MutableList<ResultPoint>?) {}
        })

        etCardCode.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                tvUnderline.setBackgroundColor(ContextCompat.getColor(this@MainActivity,
                    R.color.colorPrimary))
                if (s.length > 5) {
                    redrawButton(true)
                } else {
                    redrawButton(false)
                }

                if (s.isNotEmpty()) {
                    ivDelete.visibility = View.VISIBLE
                }

                if (s.isEmpty()) {
                    tvUnderline.setBackgroundColor(ContextCompat.getColor(this@MainActivity,
                        R.color.colorCardNumber))
                    ivDelete.visibility = View.INVISIBLE
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })

        ivDelete.setOnClickListener {
            etCardCode.setText("")
        }

        showDisclaimerDialog()
    }

    override fun onPause() {
        super.onPause()
        captureManager.onPause()
    }

    override fun onResume() {
        super.onResume()
        captureManager.onResume()

        timestamp = Date()
    }

    override fun onDestroy() {
        super.onDestroy()
        captureManager.onDestroy()
    }

    override fun onSuccess(value: String) {
        redrawContainer(ContainerState.SUCCESS, arrayOf(value))
        scanFlag = true
    }

    override fun onError(values: Array<String>) {
        redrawContainer(ContainerState.ERROR, values)
        scanFlag = true
    }

    override fun onBackPressed() {
        finishAffinity()
    }

    private fun postRequest() {
        scanFlag = false
        redrawContainer(ContainerState.WAITING, arrayOf(""))
        PostCardBalance().requestCardBalance(applicationContext, etCardCode.text.toString(),
            this)
    }

    private fun redrawButton(enabled: Boolean) {
        if (resIdLayoutButton == R.layout.layout_send_logs) {
            redrawButtonViewToInitialState()
        }

        if (enabled) {
            bBalance.backgroundTintList = ColorStateList.valueOf(ContextCompat
                .getColor(this, R.color.colorPrimary))
            bBalance.isEnabled = true
        } else {
            bBalance.backgroundTintList = ColorStateList.valueOf(ContextCompat
                .getColor(this, R.color.colorCardNumber))
            bBalance.isEnabled = false
        }

        bBalance.setOnClickListener {
            postRequest()
        }
    }

    private fun redrawContainer(containerState: ContainerState, data: Array<String>) {
        layoutContainer.removeAllViews()
        val layoutInflater: LayoutInflater = LayoutInflater.from(this)

        when(containerState) {
            ContainerState.INITIAL -> {
                layoutInflater.inflate(R.layout.layout_no_balance, layoutContainer, true)
                redrawButton(false)
            }

            ContainerState.WAITING -> {
                val view: View = layoutInflater.inflate(R.layout.layout_no_balance, layoutContainer,
                    true)
                view.findViewById<TextView>(R.id.tvGuide).setText(R.string.catching_balance)
            }
            ContainerState.SUCCESS -> {
                val balance = data[0]
                if (balance.contains("|")) {
                    val view: View = layoutInflater.inflate(R.layout.layout_pass, layoutContainer,
                        true)
                    view.findViewById<TextView>(R.id.tvPassStart).text =
                        balance.substring(0, balance.indexOf("|"))
                    view.findViewById<TextView>(R.id.tvPassEnd).text =
                        balance.substring(balance.indexOf("|") + 1)
                } else {
                    val view: View = layoutInflater.inflate(R.layout.layout_balance, layoutContainer,
                        true)
                    view.findViewById<TextView>(R.id.tvBalanceResult).text = String.format(
                        "%s €", balance)
                }

                tvUnderline.setBackgroundColor(ContextCompat.getColor(this@MainActivity,
                    R.color.colorGreen))
            }
            ContainerState.ERROR -> {
                val view: View = layoutInflater.inflate(R.layout.layout_error, layoutContainer,
                    true)
                view.findViewById<TextView>(R.id.tvErrorTitle).text = data[0]
                view.findViewById<TextView>(R.id.tvErrorBody).text = data[1]
                tvUnderline.setBackgroundColor(ContextCompat.getColor(this@MainActivity,
                    R.color.colorRed))

                if (data[1] == getString(R.string.sorry_not_sorry)) {
                    redrawButtonViewForSendingLogs()
                }
            }
        }
    }

    private fun redrawButtonViewForSendingLogs() {
        layoutContainerButton.removeAllViews()
        resIdLayoutButton = R.layout.layout_send_logs
        val view: View =  LayoutInflater.from(this).inflate(resIdLayoutButton,
            layoutContainerButton, true)
        bLogs = view.findViewById(R.id.bSendLogs)
        bLogs.paintFlags = bSendLogs.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        bLogs.setOnClickListener {
            PostLog().postLog(this, etCardCode.text.toString())

            etCardCode.setText("")

            Toasty.success(this, getString(R.string.logs_sent), Toast.LENGTH_SHORT).show()
        }
    }

    private fun redrawButtonViewToInitialState() {
        layoutContainerButton.removeAllViews()
        resIdLayoutButton = R.layout.layout_button_get_balance
        val view: View = LayoutInflater.from(this).inflate(R.layout.layout_button_get_balance,
            layoutContainerButton, true)
        bBalance = view.findViewById(R.id.bCheckBalance)
        redrawContainer(ContainerState.INITIAL, arrayOf(""))
    }

    @SuppressLint("InflateParams")
    private fun showDisclaimerDialog() {
        if (!PreferenceUtil().getPref(this, getString(R.string.prefs_key))) {
            val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_disclaimer,
                null)
            val builder = AlertDialog.Builder(this).setView(dialogView).setCancelable(false)
            val dialog = builder.show()
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            val textViewDisclaimer = dialog.findViewById<TextView>(R.id.tvDisclaimer)
            textViewDisclaimer?.text = Formatter.formatText(getString(R.string.disclaimer))

            dialog.findViewById<MaterialButton>(R.id.bGotIt)?.setOnClickListener {
                dialog.dismiss()
                PreferenceUtil().setPref(this, getString(R.string.prefs_key), true)
            }
        }
    }

    fun isTrembling(): Boolean {
        val seconds: Long = (Date().time - timestamp.time) / 1000
        timestamp = Date()
        return seconds < 5
    }
}
