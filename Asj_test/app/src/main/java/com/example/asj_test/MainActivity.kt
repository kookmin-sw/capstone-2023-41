package com.example.asj_test

import android.graphics.PointF
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.davemorrissey.labs.subscaleview.ImageSource
import java.util.*
import kotlin.collections.ArrayList

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

    fun dijkstra(graph: Array<ArrayList<Edge>>, start: Int, end: Int): Pair<Int, List<Int>> {

        // dist : 최단 거리 담을 배열. 초기엔 max_value 초기화
        val dist = IntArray(graph.size) { Int.MAX_VALUE }
        dist[start] = 0 // 시작 노드는 거리를 0으로.

        // 가장 짧은 노드 선택하기 위해 우선순위 큐
        val pq = PriorityQueue<Pair<Int, Int>>(compareBy { it.second })
        pq.add(start to 0) // 출발노드

        // prev : 최단 경로상에서 현재 노드의 직전 노드 기록
        val path = IntArray(graph.size) { -1 }

        while (pq.isNotEmpty()) {
            // poll : 우선순위 큐에서 헤드(= 가장 짧은 거리의 노드) 추출
            val (cur, curDist) = pq.poll()

            // 도착 노드에 도달하면 현 최단거리 리턴하고 종료
            if (cur == end) {
                val path2 = path.distinct().toMutableList()
                for (i in path2.indices.reversed()){
                    if (path2[i] == -1){
                        path2.removeAt(i)
                    }
                }
                return curDist to path2
            }

            // 현재 노드를 기준으로 인접노드들 불러옴.
            for (edge in graph[cur]) {
                // 시작노드 ~ 현재노드 거리 + 현재노드 ~ 인접노드 거리
                // 거리 비교 후 더 짧은 걸로 갱신
                val newDist = curDist + edge.weight
                if (newDist < dist[edge.near]) {
                    dist[edge.near] = newDist
                    pq.add(edge.near to newDist)
                    path[edge.near] = cur // 최단 경로 상의 바로 직전 노드 기록

                }
            }
        }
        return -1 to ArrayList() // 도착 노드에 도달하지 못한 경우 -1 반환
    }
}