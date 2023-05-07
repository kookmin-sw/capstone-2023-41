package com.example.capstone_kotlin

import android.graphics.*
import android.os.Bundle
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.View.OnTouchListener
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.toBitmap
import com.davemorrissey.labs.subscaleview.ImageSource
import java.io.File


class MapActivity : AppCompatActivity() {
    var map: PinView? = null
    var gestureDetector: GestureDetector? = null

    var db : DataBaseHelper? = null
    var nodesPlace: List<DataBaseHelper.PlaceNode>? = null
    var nodesCross: List<DataBaseHelper.CrossNode>? = null

    var dijk : Dijkstra? = null
    var ratio : Float? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        map = findViewById(R.id.map)
        map?.setImage(ImageSource.resource(R.drawable.mirae_4f))


        db = DataBaseHelper(this)
        nodesPlace = db!!.getNodesPlace()
        nodesCross = db!!.getNodesCross()

        // 그려졌을때 실행되는 함수
        map?.viewTreeObserver!!.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val intent = getIntent()

                ratio = map?.getResources()!!.getDisplayMetrics().density.toFloat() // 화면에 따른 이미지의 해상도 비율
                val startId = intent.getStringExtra("QRstart")
                val endId = intent.getStringExtra("QRend")

                intent.removeExtra("QRstart")
                intent.removeExtra("QRend")
                if (startId != null) {
                    var cid = db!!.findCrosstoID(startId.toInt(), nodesCross!!)
                    map?.addPin(PointF(cid!!.x.toFloat()*ratio!!, cid!!.y.toFloat()*ratio!!), 1, R.drawable.pushpin_blue)
                }
                if (endId != null)
                {
                    var cid = db!!.findCrosstoID(endId.toInt(), nodesCross!!)
                    map?.addPin(PointF(cid!!.x.toFloat()*ratio!!, cid!!.y.toFloat()*ratio!!), 1, R.drawable.pushpin_blue)
                }
                if (startId != null && endId != null)
                {
                    dijk = Dijkstra(nodesCross!!, startId.toInt(), endId.toInt())
                    var root = dijk!!.findShortestPath(dijk!!.makeGraph())
                    for (i in root)
                    {
                        var pointt = db!!.findCrosstoID(i.first.toInt(), nodesCross!!)
                        map?.addLine(PointF(pointt!!.x.toFloat()*ratio!!, pointt!!.y.toFloat()*ratio!!), Color.GREEN)
                    }
                }
                map?.viewTreeObserver!!.removeOnGlobalLayoutListener(this)
            }
        })
        // 아래는 이미지 터치에 따른 예제 코드임.
        gestureDetector = GestureDetector(this, object : SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                // 터치된 곳의 좌표 pointt
                var pointt = map?.viewToSourceCoord(e.x, e.y)

                // var msg2 = 213
                //Toast.makeText(applicationContext, msg2.toString(), Toast.LENGTH_SHORT).show()
                check_area(pointt!!.x, pointt.y)
                return true
            }
        })
        map?.setOnTouchListener(OnTouchListener { view, motionEvent ->
            gestureDetector!!.onTouchEvent(
                motionEvent
            )
        })
    }

    // 입력된 x,y 좌표 값에 대한 처리 함수 예제
    private fun check_area(x: Float, y: Float)
    {
        var testX = x/ratio!!
        var testY = y/ratio!!

        var msg2 = testX.toString() + ":" + testY.toString()
        Toast.makeText(applicationContext, msg2, Toast.LENGTH_SHORT).show()
        var id = db!!.findPlacetoXY(testX.toInt(), testY.toInt(), nodesPlace!!)
        if (id != null)
        {
            map?.addPin(PointF(id!!.x.toFloat()*ratio!!, id!!.y.toFloat()*ratio!!), 1, R.drawable.pushpin_blue)
        }
        // 핀 지우기 예제
//        if ( 'condition' )
//        {
//            map?.clearPin()
//        }
    }
}
