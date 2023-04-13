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
    val array = arrayOf(100, 100)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        imageView = findViewById<View>(R.id.imageView) as SubsamplingScaleImageView
        imageView?.setImage(ImageSource.resource(R.drawable.map))

        gestureDetector = GestureDetector(this, object : SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val sCoord = imageView?.viewToSourceCoord(e.x, e.y)
                check_area(sCoord!!.x, sCoord.y)
                return true
            }
        })
        imageView?.setOnTouchListener(OnTouchListener { view, motionEvent ->
            gestureDetector!!.onTouchEvent(
                motionEvent
            )
        })
    }

    private fun check_area(x: Float, y: Float)
    {
        if (x < array[0] && y<array[1])
        {
            val msg = "x:" + x + "y:" + y
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        }
    }
}