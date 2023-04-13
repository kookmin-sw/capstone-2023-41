package com.example.capstone_kotlin

import android.R.id
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView


class MapActivity : AppCompatActivity() {
    var imageView: SubsamplingScaleImageView? = null
    var gestureDetector: GestureDetector? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        imageView = findViewById<View>(R.id.imageView) as SubsamplingScaleImageView
        imageView?.setImage(ImageSource.resource(R.drawable.map))

        gestureDetector = GestureDetector(this, object : SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val sCoord = imageView?.viewToSourceCoord(e.x, e.y)
                val msg = "x:" + sCoord!!.x + "y:" + sCoord.y
                Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
                return true
            }
        })
        imageView?.setOnTouchListener(OnTouchListener { view, motionEvent ->
            gestureDetector!!.onTouchEvent(
                motionEvent
            )
        })
    }
}