package com.example.asj_test

import android.graphics.PointF
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.davemorrissey.labs.subscaleview.ImageSource

class MainActivity : AppCompatActivity() {

    private lateinit var Map: PinView
    private lateinit var gestureDetector: GestureDetector

    var pinNum = 0 // pin 개수
    var startPin: PointF? = null // 시작 핀 좌표

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Map = findViewById(R.id.Map)
        Map.setImage(ImageSource.resource(R.drawable.map))
        Map.maxScale = 0.5f // 최대 크기 제한

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                var TouchedPoint = Map?.viewToSourceCoord(e.x, e.y)

                if (pinNum == 0) { // 시작 핀
                    pinNum++
                    Map.setPin(TouchedPoint)
                    startPin = TouchedPoint
                } else if (pinNum == 1) { // 두번째 핀
                    pinNum++
                    Map.addPin(TouchedPoint, 0, R.drawable.pushpin_green)

                } else { // 핀이 두개 있으면 없앰
                    Map.clearPin()
                    pinNum = 0
                }

                return true
            }
        })
        Map.setOnTouchListener(View.OnTouchListener { view, motionEvent ->
            gestureDetector.onTouchEvent(
                motionEvent

            )
        })


    }
}