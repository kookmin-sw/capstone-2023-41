package com.example.capstone_kotlin

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var intent: Intent = Intent(this, MapActivity::class.java)
        intent.putExtra("QRstart", "429")
        intent.putExtra("QRend", "464")
        startActivity(intent)
    }
}