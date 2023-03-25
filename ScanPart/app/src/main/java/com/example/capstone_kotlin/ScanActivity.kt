package com.example.capstone_kotlin

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
        setContentView(R.layout.activity_scan)
        val scanBtn = findViewById<Button>(R.id.scanBtn) as Button
        scanBtn.setOnClickListener(this)
    }

    override fun onClick(p0: View?) {
        scanCode()
    }

    private fun scanCode() {

        val integrator = IntentIntegrator(this)
        //integrator.captureActivity = CaptureActivity::class.java
        integrator.setOrientationLocked(false) //
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)
        integrator.setPrompt("Scanning Code")

        // change to front-camera
        integrator.setCameraId(1)
        integrator.initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null) {
            if (result.contents != null) {
                val builder = AlertDialog.Builder(this)
                builder.setMessage(result.contents)
                builder.setTitle("Scanning Result")
                builder.setPositiveButton(
                    "Scan Again"
                ) { dialogInterface, i -> scanCode() }.setNegativeButton(
                    "finish"
                ) { dialogInterface, i -> finish() }
                val dialog = builder.create()
                dialog.show()
            } else {
                Toast.makeText(this, "No Results", Toast.LENGTH_LONG).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}