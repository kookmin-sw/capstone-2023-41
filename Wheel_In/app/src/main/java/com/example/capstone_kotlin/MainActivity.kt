package com.example.capstone_kotlin  // 파일이 속한 패키지 정의
import android.Manifest
import android.app.Activity
import android.content.Intent // Intent는 액티비티간 데이터를 전달하는데 사용된다.
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PointF
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity // AppCompatActivity 클래스를 임포트. AppCompatActivity는 안드로이드 앱에서 사용되는 기본 클래스
import android.os.Bundle // Bundle은 액티비티가 시스템에서 재생성될 때 데이터를 저장하고 다시 가져오는 데 사용
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.davemorrissey.labs.subscaleview.ImageSource
import com.google.zxing.integration.android.IntentIntegrator.REQUEST_CODE

// Activity 는 사용자와 상호작용 하기 위한 하나의 단위


class MainActivity : AppCompatActivity() {  // MainActivity정의, AppCompatActivity 클래스를 상속받음

    // 테스트위해서 lateinit 설정
    private lateinit var searchView1: SearchView
    private lateinit var searchView2: SearchView

    // 정보창 및 표시될 사진, 지명, 접근성
    private lateinit var info: FrameLayout
    private lateinit var infoPic1: ImageView
    private lateinit var infoPic2: ImageView
    private lateinit var infoText1: TextView
    private lateinit var infoText2: TextView

    // QR 촬영으로 값 받아올 변수
    private var returnedData: String? = null
    private var id: DataBaseHelper.PlaceNode? = null

    // MapActivity 선언부
    private lateinit var map: PinView

    // 터치 처리
    private lateinit var gestureDetector: GestureDetector

    // DB
    private lateinit var db: DataBaseHelper
    private lateinit var nodesPlace: List<DataBaseHelper.PlaceNode>
    private lateinit var nodesCross: List<DataBaseHelper.CrossNode>

    // 길찾기
    private lateinit var dijk: Dijkstra

    // 지도 좌표 비율
    var ratio = 0F

    // mapvar
    var startId: String? = null;
    var endId: String? = null;

    override fun onCreate(savedInstanceState: Bundle?) { // onCreate 함수를 오버라이드. 이 함수는 액티비티가 생성될 때 호출됨.
        super.onCreate(savedInstanceState) // 부모 클래스의 onCreate 함수를 호출
        setContentView(R.layout.activity_main)

        map = findViewById(R.id.map);

        // 출발지, 목적지 입력 서치뷰
        searchView1 = findViewById(R.id.searchView1)
        searchView2 = findViewById(R.id.searchView2)

        // QR 촬영 버튼
        var qrButton: Button = findViewById(R.id.qrButton)

        // 층 수 스피너
        val spinner: Spinner = findViewById(R.id.spinner)

        // 정보창
        info = findViewById(R.id.info)

        // 정보창에 띄울 정보들
        infoText1 = findViewById(R.id.text1)
        infoText2 = findViewById(R.id.text2)

        infoPic1 = findViewById(R.id.infoPic1)
        infoPic2 = findViewById(R.id.infoPic2)

        // 출발, 도착 버튼
        var start = findViewById<Button>(R.id.start)
        var end = findViewById<Button>(R.id.end)

        // 지도 크기 제한
        map.maxScale = 1f

        db = DataBaseHelper(this)
        nodesPlace = db.getNodesPlace()
        nodesCross = db.getNodesCross()

        // QR 촬영 버튼 활성화.
        qrButton.setOnClickListener{
            val intent = Intent(this, ScanActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE)
        }

        // 비상 연락망 변수 추가
        val btn_emergency: Button = findViewById(R.id.btn_emergency)
        btn_emergency.setOnClickListener {
            showEmergencyPopup()
        }

        // 스피너에 항목 추가.
        val items: MutableList<String> = ArrayList()
        items.add("미래관 4층")
        items.add("미래관 3층")
        items.add("미래관 2층")

        // 출발지와 목적지 입력 서치뷰 활성화.
        // 2023-05-27 15시 기준 : 현재 serachView 를 입력 시 serachView2가 보임.
        // 두 searchView 모두 비어있어야 searchView2 사라짐.
        searchView1.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty() && searchView2.query.isEmpty()) {
                    searchView2.visibility = View.GONE
                }
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                if (query.isEmpty() && searchView2.query.isEmpty()) {
                    searchView2.visibility = View.GONE
                } else {
                    searchView2.visibility = View.VISIBLE
                }
                return true
            }
        })

        searchView2.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                // searchView2의 입력 상태에 따라 처리
                if (newText.isEmpty() && searchView1.query.isEmpty()) {
                    searchView2.visibility = View.GONE
                } else {
                    searchView2.visibility = View.VISIBLE
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
            searchView1.setQuery(id?.name, true)
            if (startId != null) {
                map.clearPin()
                map.addPin((PointF(id!!.x.toFloat()*ratio, id!!.y.toFloat()*ratio)),1, R.drawable.pushpin_blue)
            }
            startId = id?.id.toString()
            Toast.makeText(applicationContext, startId, Toast.LENGTH_SHORT).show()
            mapInit()
            info.visibility = View.GONE
        }

        // 도착 버튼 누르면 searchView2 채우기
        end.setOnClickListener{
            searchView2.setQuery(id!!.name, true)
            if (endId != null) {
                map?.clearPin()
                map?.addPin((PointF(id!!.x.toFloat()*ratio!!, id!!.y.toFloat()*ratio!!)),1, R.drawable.pushpin_blue)
            }
            endId = id!!.id.toString()
            Toast.makeText(applicationContext, endId, Toast.LENGTH_SHORT).show()
            mapInit()
            info.visibility = View.GONE
        }
        // 화면 비율
        ratio = map.getResources().getDisplayMetrics().density.toFloat() // 화면에 따른 이미지의 해상도 비율

        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {

                var pointt = map.viewToSourceCoord(e.x, e.y);
                var x = pointt!!.x/ratio
                var y = pointt!!.y/ratio
                id = db.findPlacetoXY(x.toInt(), y.toInt(), nodesPlace)
                if (id != null)
                {
                    showInfoTap(id)
                }
                else if (info.visibility == View.VISIBLE) {
                    info.visibility = View.GONE
                }

                return true
            }
        })
        map?.setOnTouchListener(View.OnTouchListener { view, motionEvent -> // OnTouchListner로 터치 이벤트 감지
            gestureDetector!!.onTouchEvent( // gestureDectector로 터치 이벤트 처리
                motionEvent
            )
        })

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



        // MapActivity 연결부
        map?.setImage(ImageSource.resource(R.drawable.mirae_4f))

        // 그려졌을때 실행되는 함수
        map?.viewTreeObserver!!.addOnGlobalLayoutListener(object: ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                mapInit();
                map?.viewTreeObserver!!.removeOnGlobalLayoutListener(this)
            }
        })
    }

    fun mapInit()
    {
        ratio = map?.getResources()!!.getDisplayMetrics().density.toFloat() // 화면에 따른 이미지의 해상도 비율

        var scid: DataBaseHelper.CrossNode? = null
        var ecid: DataBaseHelper.CrossNode? = null

        var navi = findViewById<FrameLayout>(R.id.navi)

        var navi_img1 = findViewById<ImageView>(R.id.navi_img1)
        var navi_img2 = findViewById<ImageView>(R.id.navi_img2)

        var navi_start = findViewById<Button>(R.id.navi_start)
        var navi_end = findViewById<Button>(R.id.navi_end)

        if (returnedData != null)
        {
            scid = db!!.findCrosstoID(startId!!.toInt(), nodesCross!!)
        }
        else if (startId != null) {
            scid = db!!.findCrosstoID(startId!!.toInt(), nodesCross!!)
            //map?.addPin(PointF(cid!!.x.toFloat()*ratio!!, cid!!.y.toFloat()*ratio!!), 1, R.drawable.pushpin_blue)
        }
        if (endId != null)
        {
            ecid = db!!.findCrosstoID(endId!!.toInt(), nodesCross!!)
            //map?.addPin(PointF(cid!!.x.toFloat()*ratio!!, cid!!.y.toFloat()*ratio!!), 1, R.drawable.pushpin_blue)
        }
        if (startId != null && endId != null)
        {
            map?.clearPin();
            map?.addPin(PointF(scid!!.x.toFloat()*ratio!!, scid!!.y.toFloat()*ratio!!), 1, R.drawable.pushpin_blue)
            map?.addPin(PointF(ecid!!.x.toFloat()*ratio!!, ecid!!.y.toFloat()*ratio!!), 1, R.drawable.pushpin_blue)
            dijk = Dijkstra(nodesCross!!, startId!!.toInt(), endId!!.toInt())
            var root = dijk!!.findShortestPath(dijk!!.makeGraph())
            for (i in root)
            {
                var pointt = db!!.findCrosstoID(i.first, nodesCross!!)
                map?.addLine(PointF(pointt!!.x.toFloat()*ratio!!, pointt!!.y.toFloat()*ratio!!), Color.GREEN)

                if (i.second != "start" && i.second != "end" && i.second != "place") {
                    map?.addPin(PointF(pointt!!.x.toFloat()*ratio!!, pointt!!.y.toFloat()*ratio!!), 1, R.drawable.crossroad)
                }

                if (i.second == "east") {
                    navi_img1.setImageBitmap(pointt!!.imgEast)
                }

                if (i.second == "south") {
                    navi_img2.setImageBitmap(pointt!!.imgSouth)
                }
            }

            startId = ""
            endId = ""

            navi.visibility = View.VISIBLE

            navi_img1.visibility = View.GONE
            navi_img2.visibility = View.GONE

            navi_start.setOnClickListener {
                navi.setBackgroundResource(R.drawable.white_space)

                navi.setOnTouchListener { _, event ->
                    // frameLayout을 터치할 때 이벤트가 발생하면 true를 반환하여
                    // 해당 이벤트를 소비하고, map의 onTouchEvent를 호출하지 않도록 합니다.
                    true
                }

                navi_start.visibility = View.GONE

                navi_img1.visibility = View.VISIBLE
                navi_img2.visibility = View.VISIBLE
            }

            navi_end.setOnClickListener{

                searchView2.setQuery("", true)
                searchView1.setQuery("", true)

                map?.clearPin()

                navi.setBackgroundResource(0)

                startId = null
                endId = null

                navi_start.visibility = View.VISIBLE

                navi.visibility = View.GONE
            }
        }
    }



    // QR 촬영 후 데이터 값 받아옴.
    // 2023-05-27 15시 기준 현재 받아온 데이터 값을 searchView1 에 넣음.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            returnedData = data?.getStringExtra("QRdata")
            Toast.makeText(this, returnedData, Toast.LENGTH_SHORT).show()
            var test = db.findPlacetoID(returnedData!!.toInt(), nodesPlace)
            showInfoTap(test)
            startId = returnedData
        }
    }



    // 비상 연락처 호출 함수
    private fun showEmergencyPopup() {
        val contactNumber = "02-0000-000"

        val inflater = layoutInflater
        val popupView = inflater.inflate(R.layout.emergency_popup, null)

        val alertDialog = AlertDialog.Builder(this)
            .setView(popupView)
            .setCancelable(true)
            .create()

        val contactTextView = popupView.findViewById<TextView>(R.id.contactNumber)
        contactTextView.text = contactNumber

        popupView.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 1)
            } else {
                val intent = Intent(Intent.ACTION_CALL)
                intent.data = Uri.parse("tel:$contactNumber")
                startActivity(intent)
            }
            alertDialog.dismiss()
        }

        alertDialog.show()
    }


    // Map Func
    // 입력된 x,y 좌표 값에 대한 처리 함수 예제
    private fun check_area(x: Float, y: Float) : DataBaseHelper.PlaceNode?
    {
        var testX = x/ratio!!
        var testY = y/ratio!!

        var id = db!!.findPlacetoXY(testX.toInt(), testY.toInt(), nodesPlace!!)
        if (id != null)
        {
            //map?.clearPin();
            if (startId == "") {
                return null
            }

            if (startId != null) {
                map?.clearPin()
                map?.addPin((PointF(db!!.findPlacetoID(startId!!.toInt(), nodesPlace!!)!!.x.toFloat()*ratio!!, db!!.findPlacetoID(startId!!.toInt(), nodesPlace!!)!!.y.toFloat()*ratio!!)),1, R.drawable.pushpin_blue)
            }
            else if (endId != null) {
                map?.clearPin()
                map?.addPin((PointF(db!!.findPlacetoID(endId!!.toInt(), nodesPlace!!)!!.x.toFloat()*ratio!!, db!!.findPlacetoID(endId!!.toInt(), nodesPlace!!)!!.y.toFloat()*ratio!!)),1, R.drawable.pushpin_blue)
            }
            else {
                map?.clearPin()
            }

            map?.addPin(PointF(id!!.x.toFloat()*ratio!!, id!!.y.toFloat()*ratio!!), 1, R.drawable.pushpin_blue)
            return id
        }

        if (startId == "") {
            return null
        }

        if (startId != null) {
            map?.clearPin()
            map?.addPin((PointF(db!!.findPlacetoID(startId!!.toInt(), nodesPlace!!)!!.x.toFloat()*ratio!!, db!!.findPlacetoID(startId!!.toInt(), nodesPlace!!)!!.y.toFloat()*ratio!!)),1, R.drawable.pushpin_blue)
        }
        else if (endId != null) {
            map?.clearPin()
            map?.addPin((PointF(db!!.findPlacetoID(endId!!.toInt(), nodesPlace!!)!!.x.toFloat()*ratio!!, db!!.findPlacetoID(endId!!.toInt(), nodesPlace!!)!!.y.toFloat()*ratio!!)),1, R.drawable.pushpin_blue)
        }
        else {
            map?.clearPin()
        }

        return null
    }

    // 현재 QR 에서 강의실 호수 숫자만 리턴하므로 그에 대한 테스트용 함수.
    fun showInfoQR(id: String?){
        if (id != null) {
            for (i in nodesPlace!!.indices) {
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


                    map.animateScaleAndCenter(1f,PointF(nodesPlace[i].x.toFloat()*ratio, nodesPlace[i].y.toFloat()*ratio))?.start()
                    map.addPin(PointF(nodesPlace[i].x.toFloat()*ratio, nodesPlace[i].y.toFloat()*ratio), 1, R.drawable.pushpin_blue)
                    return
                }
            }
        }
        else{
            info.visibility = View.GONE
        }
    }

    // QR에서 DB 정보를 받아올 경우.
    // 추후 수정 필요 (QR에서 받아오는 데이터는 스트링 형태이므로 문자열 슬라이싱 및 타입 변환 필요)
    fun showInfoTap(id: DataBaseHelper.PlaceNode?){
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

            map.animateScaleAndCenter(1f,PointF(id.x.toFloat()*ratio, id.y.toFloat()*ratio))?.start()
            map.addPin(PointF(id.x.toFloat()*ratio, id.y.toFloat()*ratio), 1, R.drawable.pushpin_blue)
            return
        }
        else{
            info.visibility = View.GONE
            map.clearPin()
        }
    }
}


