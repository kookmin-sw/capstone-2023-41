package com.example.capstone_kotlin

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
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

        imageView = findViewById(R.id.imageView)
        //imageView?.setImage(ImageSource.asset("map.png"))

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