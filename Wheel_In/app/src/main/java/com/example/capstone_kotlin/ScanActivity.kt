package com.example.capstone_kotlin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator

class ScanActivity : AppCompatActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_scan)
        scanCode()
    }

    override fun onClick(p0: View?) {
//        scanCode()
    }

    private fun scanCode() {

        val integrator = IntentIntegrator(this)
        //integrator.captureActivity = scan::class.java
        integrator.setOrientationLocked(false)
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
        integrator.setPrompt("주변의 QR코드를 스캔해주세요.")

        // change to front-camera
        integrator.setCameraId(0)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                // MainActivity로 결과 전송을 위한 Intent 객체 생성
                val ToMainIntent : Intent = Intent(this@ScanActivity, MainActivity::class.java);
                ToMainIntent.putExtra("QRdata", result.contents)
                setResult(Activity.RESULT_OK, ToMainIntent)
                finish()
            } else {    // 뒤로가기 버튼을 누르는 등 아무것도 촬영 안한 경우
                Toast.makeText(this, "QR코드에 아무것도 담겨있지 않아요.", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}