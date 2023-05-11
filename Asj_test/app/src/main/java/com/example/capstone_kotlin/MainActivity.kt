package com.example.capstone_kotlin  // 파일이 속한 패키지 정의
import android.app.Activity
import android.content.Intent // Intent는 액티비티간 데이터를 전달하는데 사용된다.
import android.graphics.Color
import android.graphics.PointF
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity // AppCompatActivity 클래스를 임포트. AppCompatActivity는 안드로이드 앱에서 사용되는 기본 클래스
import android.os.Bundle // Bundle은 액티비티가 시스템에서 재생성될 때 데이터를 저장하고 다시 가져오는 데 사용
import android.view.*
import android.view.GestureDetector.SimpleOnGestureListener
import android.widget.*
import com.davemorrissey.labs.subscaleview.ImageSource
//import androidx.lifecycle.viewmodel.CreationExtras.Empty.map
import com.example.capstone_kotlin.DataBaseHelper
import com.google.zxing.integration.android.IntentIntegrator.REQUEST_CODE

// Activity 는 사용자와 상호작용 하기 위한 하나의 단위


class MainActivity : AppCompatActivity() {  // MainActivity정의, AppCompatActivity 클래스를 상속받음

    // 테스트위해서 lateinit 설정
    private lateinit var searchView1: SearchView
    private lateinit var infoText1: TextView
    private lateinit var infoText2: TextView
    private lateinit var map: PinView

    private lateinit var navi: Button

    private lateinit var info: FrameLayout
    private lateinit var infoPic1: ImageView
    private lateinit var infoPic2: ImageView
    private lateinit var gestureDetector: GestureDetector

    private lateinit var roadInfo: FrameLayout

    private lateinit var db: DataBaseHelper
    private lateinit var nodesPlace: List<DataBaseHelper.PlaceNode>
    private lateinit var nodesCross: List<DataBaseHelper.CrossNode>

    private var id: DataBaseHelper.PlaceNode? = null

    private var returnedData: String? = null

    private lateinit var dijk: Dijkstra

    private lateinit var startId: String
    private lateinit var endId: String


    var ratio = 0F

    override fun onCreate(savedInstanceState: Bundle?) { // onCreate 함수를 오버라이드. 이 함수는 액티비티가 생성될 때 호출됨.
        super.onCreate(savedInstanceState) // 부모 클래스의 onCreate 함수를 호출
        setContentView(R.layout.activity_main)

        // DB
        db = DataBaseHelper(this)
        nodesPlace = db.getNodesPlace()
        nodesCross = db.getNodesCross()

        // 지도
        map = findViewById<PinView>(R.id.map)
        map.setImage(ImageSource.resource(R.drawable.mirae_4f))

        // 출발지, 목적지 입력 서치뷰
        searchView1 = findViewById<SearchView>(R.id.searchView1)
        val searchView2 = findViewById<SearchView>(R.id.searchView2)
        val destination = findViewById<LinearLayout>(R.id.destination)

        // QR 촬영 버튼
        var qrButton: Button = findViewById(R.id.qrButton)

        // 층 수 스피너
        val spinner: Spinner = findViewById(R.id.spinner)

        // 정보창
        info = findViewById<FrameLayout>(R.id.info)

        // 정보창에 띄울 사진들
        infoPic1 = findViewById<ImageView>(R.id.infoPic1)
        infoPic2 = findViewById<ImageView>(R.id.infoPic2)

        // 정보창에 띄울 이름과 접근성
        infoText1 = findViewById(R.id.infoText1)
        infoText2 = findViewById(R.id.infoText2)

        // 길찾기 버튼
        navi = findViewById(R.id.navi)

        // 경로 안내 화면
        roadInfo = findViewById(R.id.roadInfo)

        // 화면 비율
        ratio = map.getResources().getDisplayMetrics().density.toFloat() // 화면에 따른 이미지의 해상도 비율

        // 출발, 도착 버튼
        var start = findViewById<Button>(R.id.start)
        var end = findViewById<Button>(R.id.end)

        // 지도 첫 크기
//        map.scaleX = 2f
//        map.scaleY = 2f
//        map.setScaleAndCenter(2f, map.center)
//        map.animateScaleAndCenter(2f, map.center)

        // 지도 크기 제한
        map.maxScale = 1f

        // 길찾기 버튼 활성화.
        navi.setOnClickListener{
            // 구현 필요.
            roadInfo.visibility = View.VISIBLE
        }

        // QR 촬영 버튼 활성화.
        qrButton.setOnClickListener{
            val intent = Intent(this, ScanActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE)
        }

        // 스피너에 항목 추가.
        val items: MutableList<String> = ArrayList()
        items.add("미래관 4층")
        items.add("미래관 3층")
        items.add("미래관 2층")

        // 출발지와 목적지 입력 서치뷰 활성화.
        searchView1.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty() && searchView2.query.isEmpty()) {
                    destination.visibility = View.GONE
                }
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                if (query.isEmpty() && searchView2.query.isEmpty()) {
                    destination.visibility = View.GONE
                } else {
                    // QR 로 데이터 받아오거나 DB에 있는 지명 검색 완료 시
                    // 정보창 보이게 함.
                    destination.visibility = View.VISIBLE
                }
                return true
            }
        })

        // searchView2(목적지 입력)이 입력되면 searchView1에 '출발지를 입력하세요.' 라고 변경.
        searchView2.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                // searchView2의 입력 상태에 따라 처리
                if (newText.isEmpty() && searchView1.query.isEmpty()) {
                    destination.visibility = View.GONE
                } else {
                    destination.visibility = View.VISIBLE
                    searchView1.queryHint = "출발지를 입력하세요."
                }
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                return true
            }
        })

        // 정보창 활성화 시 배경 가리기.
        info.setBackgroundResource(R.drawable.white_space)

        // 정보창과 map이 겹치는 부분을 클릭할 때 이벤트가 발생하지 않도록.
        info.setOnTouchListener { _, event ->
            // frameLayout을 터치할 때 이벤트가 발생하면 true를 반환하여
            // 해당 이벤트를 소비하고, map의 onTouchEvent를 호출하지 않도록 합니다.
            true
        }

        // 출발 버튼 누르면 searchView1 채우기
        start.setOnClickListener{
            searchView1.setQuery(id?.id.toString(), true)
            startId = id?.id.toString()
            info.visibility = View.GONE
        }

        // 도착 버튼 누르면 searchView2 채우기
        end.setOnClickListener{
            searchView2.setQuery(id?.id.toString(), true)
            endId = id?.id.toString()
            info.visibility = View.GONE
        }

        // DB 에 등록된 노드 정보 활성화.
        gestureDetector = GestureDetector(this, object : SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                var pointt = map.viewToSourceCoord(e.x, e.y)
                var x = pointt!!.x/ratio
                var y = pointt!!.y/ratio
                id = db.findPlacetoXY(x.toInt(), y.toInt(), nodesPlace)
                if(id != null){
                    showInfo(id)
                }
                else{
                    showInfo(null)
                }
                Toast.makeText(applicationContext, id.toString(), Toast.LENGTH_SHORT).show()
                return true
            }
        })

        // 클릭 이벤트 처리
        map.setOnTouchListener { view, event ->
            gestureDetector.onTouchEvent(
                event
            )
        }

        // 스피너 활성화
        val adapter: ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedItem: String = parent.getItemAtPosition(position) as String
                if (selectedItem == "Add New Item") {
                    // Do something
                } else {
                    Toast.makeText(applicationContext, "Selected item: $selectedItem", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // 경로 그리기
//        map.viewTreeObserver.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
//            override fun onGlobalLayout() {
//                mapInit();
//                map.viewTreeObserver.removeOnGlobalLayoutListener(this)
//            }
//        })
    }

    fun mapInit()
    {
        ratio = map.getResources().getDisplayMetrics().density.toFloat() // 화면에 따른 이미지의 해상도 비율
//
        var scid: DataBaseHelper.CrossNode? = null
        var ecid: DataBaseHelper.CrossNode? = null
        if(returnedData == null){
            return
        }
//        else if (returnedData != null)
//        {
//            scid = db.findCrosstoID(startId.toInt(), nodesCross)
//            map.addPin(PointF(scid!!.x.toFloat()*ratio, scid!!.y.toFloat()*ratio), 1, R.drawable.pushpin_blue)
//            return
//        }
//        if(startId != null){
//            scid = db.findCrosstoID(startId.toInt(), nodesCross)
//            map.addPin(PointF(scid!!.x.toFloat()*ratio, scid!!.y.toFloat()*ratio), 1, R.drawable.pushpin_blue)
//        }
//        else if(endId != null){
//            ecid = db.findCrosstoID(endId.toInt(), nodesCross)
//            map?.addPin(PointF(ecid!!.x.toFloat()*ratio!!, ecid!!.y.toFloat()*ratio!!), 1, R.drawable.pushpin_blue)
//        }
//        if (startId != null && endId != null)
//        {
////            map.clearPin();
////            map.addPin(PointF(scid!!.x.toFloat()*ratio, scid!!.y.toFloat()*ratio), 1, R.drawable.pushpin_blue)
////            map.addPin(PointF(ecid!!.x.toFloat()*ratio, ecid!!.y.toFloat()*ratio), 1, R.drawable.pushpin_blue)
//            dijk = Dijkstra(nodesCross, startId.toInt(), endId.toInt())
//            var root = dijk.findShortestPath(dijk.makeGraph())
//            for (i in root)
//            {
//                var pointt = db.findCrosstoID(i.first.toInt(), nodesCross)
//                map.addLine(PointF(pointt!!.x.toFloat()*ratio, pointt!!.y.toFloat()*ratio), Color.GREEN)
//                if (i.second != "start" && i.second != "end" && i.second != "place") {
//                    map.addPin(PointF(pointt!!.x.toFloat()*ratio, pointt!!.y.toFloat()*ratio), 1, R.drawable.pushpin_blue)
//                }
//            }
//        }
    }


    // QR 촬영 후 데이터 값 받아옴.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            returnedData = data?.getStringExtra("QRdata")
            Toast.makeText(this, returnedData, Toast.LENGTH_SHORT).show()
//            if(returnedData != null && returnedData is DataBaseHelper.PlaceNode){
//
//            }
//            showInfo(returnedData)
            showInfoTest(returnedData)
        }
    }

    // 입력된 x,y 좌표 값에 대한 처리 함수 예제
    private fun check_area(x: Float, y: Float)
    {
        var testX = x/ratio
        var testY = y/ratio

        var msg2 = testX.toString() + ":" + testY.toString()
        Toast.makeText(applicationContext, msg2, Toast.LENGTH_SHORT).show()
        var id = db.findPlacetoXY(testX.toInt(), testY.toInt(), nodesPlace)
        if (id != null)
        {
            map?.addPin(PointF(id!!.x.toFloat()*ratio, id!!.y.toFloat()*ratio), 1, R.drawable.pushpin_blue)
        }
        // 핀 지우기 예제
//        if ( 'condition' )
//        {
//            imageView?.clearPin()
//        }
    }

    // QR에서 DB 정보를 받아올 경우.
    // 추후 수정 필요 (QR에서 받아오는 데이터는 스트링 형태이므로 문자열 슬라이싱 및 타입 변환 필요)
    fun showInfo(id: DataBaseHelper.PlaceNode?){
        if (id != null) {
            // 정보 사진
            infoPic1.setImageBitmap(id?.img1)
            infoPic2.setImageBitmap(id?.img2)
            // 접근성
            if(id?.access == 0){
                infoText2.setBackgroundColor(Color.RED)
            }
            if(id?.access == 1){
                infoText2.setBackgroundColor(Color.YELLOW)
            }
            if(id?.access == 2){
                infoText2.setBackgroundColor(Color.GREEN)
            }
            // 지명
            infoText1.setText(id?.name + id?.id)
            info.visibility = View.VISIBLE
        }
        else{
            info.visibility = View.GONE
        }
    }

    // 현재 QR 에서 강의실 호수 숫자만 리턴하므로 그에 대한 테스트용 함수.
    fun showInfoTest(id: String?){
        if (id != null) {
            for (i in nodesPlace.indices) {
                if (nodesPlace[i].id == id.toInt()) {
                    // 정보 사진
                    infoPic1.setImageBitmap(nodesPlace[i].img1)
                    infoPic2.setImageBitmap(nodesPlace[i].img2)
                    // 접근성
                    if (nodesPlace[i].access == 0) {
                        infoText2.setBackgroundColor(Color.RED)
                    }
                    else if (nodesPlace[i].access == 1) {
                        infoText2.setBackgroundColor(Color.YELLOW)
                    }
                    else if (nodesPlace[i].access == 2) {
                        infoText2.setBackgroundColor(Color.GREEN)
                    }
                    // 지명
                    infoText1.setText(nodesPlace[i].name + nodesPlace[i].id)
                    info.visibility = View.VISIBLE
                }
            }
        }
        else{
            info.visibility = View.GONE
        }
    }
}