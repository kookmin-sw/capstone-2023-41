package com.example.capstone_kotlin

import android.R.*
import android.graphics.*
import android.os.Bundle
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.View.OnTouchListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import java.util.*
import kotlin.collections.ArrayList


class MapActivity : AppCompatActivity() {
    var imageView: PinView? = null
    var gestureDetector: GestureDetector? = null

    // 테스트용
    val array = arrayOf(100, 100)
    var test_count = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        imageView = findViewById(R.id.imageView)
        imageView?.setImage(ImageSource.resource(R.drawable.mirae_4f))


        // 그려졌을때 실행되는 함수
        imageView?.viewTreeObserver!!.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                    val intent = getIntent()
                    val myData = intent.getStringExtra("QRdata")!!.split(":")
                    if (myData != null) {
                        println("알라딘")
                        var pointt2 = PointF(myData[0].toFloat(), myData[1].toFloat())
                        imageView?.setPin(pointt2)
                    }
                imageView?.viewTreeObserver!!.removeOnGlobalLayoutListener(this)
            }
        })

        // 아래는 이미지 터치에 따른 예제 코드임.
        gestureDetector = GestureDetector(this, object : SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                // 터치된 곳의 좌표 pointt
                var pointt = imageView?.viewToSourceCoord(e.x, e.y)

               // 처음 터치시에 대한 이벤트 예제
                if(test_count==0)
                {
                    test_count = 1
                    imageView?.setPin(pointt!!)
                }
               else
                {
                    imageView?.addPin(pointt, 0, R.drawable.pushpin_blue)
                //Toast.makeText()
                    imageView?.addLine(pointt, Color.BLUE)
                }
                check_area(pointt!!.x, pointt.y)
                return true
            }
        })
        imageView?.setOnTouchListener(OnTouchListener { view, motionEvent ->
            gestureDetector!!.onTouchEvent(
                motionEvent
            )
        })

    }

    // 입력된 x,y 좌표 값에 대한 처리 함수 예제
    private fun check_area(x: Float, y: Float)
    {
        if (x < array[0] && y<array[1])
        {
            imageView?.clearPin()
            val msg = "x:" + x + "y:" + y
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        }
    }
    private fun from_intent() : Boolean
    {
        // intent로 데이터 받아오기
        val intent = getIntent()
        val myData = intent.getStringExtra("QRdata")!!.split(":")
        if (myData == null) {return false}
        var pointt2 = PointF(myData[0].toFloat(), myData[1].toFloat())
        imageView?.addPin(pointt2,1,R.drawable.pushpin_blue)
        intent.removeExtra("QRdata")
        return true
    }

}
