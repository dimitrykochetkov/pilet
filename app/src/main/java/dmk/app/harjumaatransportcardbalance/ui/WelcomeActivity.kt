package dmk.app.harjumaatransportcardbalance.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import dmk.app.harjumaatransportcardbalance.R
import kotlinx.android.synthetic.main.activity_welcome.*

class WelcomeActivity : BaseActivity() {

    private val permissionId = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startActivity(Intent(this, MainActivity::class.java))
        } else {
            setContentView(R.layout.activity_welcome)

            bCheckBalance.setOnClickListener {
                requestPermission()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        when (requestCode) {
            permissionId -> {
                if ((grantResults.isNotEmpty() && grantResults[0] !=
                            PackageManager.PERMISSION_GRANTED)) {
                    redrawIfPermissionDenied()
                } else {
                    startActivity(Intent(this, MainActivity::class.java))
                }
            }
        }
    }

    private fun redrawIfPermissionDenied() {
        tvHello.setText(R.string.permission_denied)
        tvPermissionExplanation.setText(R.string.unable_use)
        ivSeagull.setImageDrawable(getDrawable(R.drawable.ic_seagull_denied))
    }

    private fun requestPermission(){
        ActivityCompat.requestPermissions(this,
            arrayOf(Manifest.permission.CAMERA), permissionId)
    }
}