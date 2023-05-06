// 지도 기능을 담당하는 액티비 //

package com.example.capstone_kotlin

import android.R.*
import android.graphics.*
import android.os.Bundle
import android.view.*

import android.view.GestureDetector.SimpleOnGestureListener
import android.view.View.OnTouchListener
import android.widget.Button
// 사용자의 특정 움직임을 감지해서 이벤트가 발생하도록 만드는데 손가락으로 눌렀을때, 움직였을때, 손가락을 뗐을때 등 이런 여러가지 움직임을 감지하는 대표적인 인터페이스

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import java.util.*
import kotlin.collections.ArrayList


class MapActivity : AppCompatActivity() {
    // 이미지뷰를 저장하기 위한 PinView 변수와 제스처를 감지하기 위한 GestureDetector 변수를 초기화
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
        val btn_emergency: Button = findViewById(com.example.capstone_kotlin.R.id.btn_emergency)
        btn_emergency.bringToFront()
        // 아래는 이미지 터치에 따른 예제 코드임.
        gestureDetector = GestureDetector(this, object : SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {  // onSingleTapConfirmed() :  이미지뷰를 단일 탭할 때 호출됨

                // 터치된 곳의 좌표값 계산
                var pointt = imageView?.viewToSourceCoord(e.x, e.y)

                // 처음 터치시에 대한 이벤트 예제
                if(test_count==0)
                {
                    test_count = 1
                    imageView?.setPin(pointt) // 처음 터치할 때 setPin으로 해당 지점에 핀을 표시

                    var s = (imageView?.minScale!! + imageView?.maxScale!!)/3
                    imageView?.setScaleAndCenter(s!!, pointt) // setScaleAndCenter를 사용하여 이미지뷰를 해당 지점을 중심으로 확대.
                    //AnimationBuilder(s!!, pointt).withD
                }
                else
                {
                    //imageView?.addPin(pointt, 0, R.drawable.pushpin_blue)
                    imageView?.addLine(pointt, Color.BLUE) // 이전 지점과 현재 지점을 이어주는 선을 그림.
                }
                check_area(pointt!!.x, pointt.y) // check_area 함수를 호출하여 해당 지점이 특정 영역에 있는지 확인
                return true
            }
        })
        imageView?.setOnTouchListener(OnTouchListener { view, motionEvent -> // OnTouchListner로 터치 이벤트 감지
            gestureDetector!!.onTouchEvent( // gestureDectector로 터치 이벤트 처리
                motionEvent
            )
        })
    }

    // 입력된 x,y 좌표 값에 대한 처리 함수 예제
    private fun check_area(x: Float, y: Float) // check_area(x, y)로 입력된 x,y 좌표값이 array 배열에 저장된 값보다 작은지 비교
    {
        if (x < array[0] && y<array[1])  // 해당 영역 안에 있는 경우
        {
            imageView?.clearPin() // clearPin을 호출하여 핀을 지우기.
            val msg = "x:" + x + "y:" + y
            Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
        }
    }

/*
    var INF = 1000000
    var number = 6
    var arry = arrayOf(arrayOf(123), arrayOf(123))
    var v = arrayOf(false)
    var d = arrayOf(number)
    fun getSmallIndex() : Int {
        var min = INF
        var index = 0
        for (i in 0..number) {
            if(d[i] < min && !(v[i])) {
                min = d[i]
                index = i
            }
        }
        return index
    }
    fun dijkstra(start: Int) {
        for (i in 0..number) {
            d[i] = arry[start][i]
        }
        v[start]
        for (i in 0..number-2) {
            var current = getSmallIndex()
            v[current] = true
            for(j in 0..number) {
                if(!v[j]) {
                    if(d[current] + arry[current][j] < d[j]) {
                        d[j] = d[current] + arry[current][j]
                    }
                }
            }
        }
    }
*/
}
