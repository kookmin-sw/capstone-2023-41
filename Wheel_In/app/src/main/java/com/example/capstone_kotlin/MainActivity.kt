package com.example.capstone_kotlin  // 파일이 속한 패키지 정의
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent // Intent는 액티비티간 데이터를 전달하는데 사용된다.
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity // AppCompatActivity 클래스를 임포트. AppCompatActivity는 안드로이드 앱에서 사용되는 기본 클래스
import android.os.Bundle // Bundle은 액티비티가 시스템에서 재생성될 때 데이터를 저장하고 다시 가져오는 데 사용
import android.os.Handler
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.davemorrissey.labs.subscaleview.ImageSource
import com.google.zxing.integration.android.IntentIntegrator.REQUEST_CODE


// 현재 문제점 (5/18, 12:54)
// 3층 지도를 띄운 후, 자율주행 스튜디오를 검색하면 3층 지도에 자주스가 찍힘.



class MainActivity : AppCompatActivity() {  // MainActivity정의, AppCompatActivity 클래스를 상속받음

    // 지도
    private lateinit var map: PinView

    // 서치뷰, QR, 취소 버튼 레이아웃
    private lateinit var searchView1: SearchView
    private lateinit var searchView2: SearchView
    private lateinit var searchView_layout: LinearLayout
    private lateinit var cancel: Button
    private lateinit var svAndCancel: LinearLayout


    // 출발지 목적지 같은 값이 들어가지 않게 확인하는 변수
    private var checkS1: String? = null
    private var checkS2: String? = null

    // 정보창 및 표시될 사진, 지명, 접근성
    private lateinit var info: FrameLayout
    private lateinit var infoPic1: ImageView
    private lateinit var infoPic2: ImageView
    private lateinit var infoText1: TextView
    private lateinit var infoText2: TextView

    private lateinit var spinner: Spinner

    private lateinit var testinfo: FrameLayout
    private lateinit var testbtn: Button

    // QR 촬영으로 값 받아올 변수
    private var id: DataBaseHelper.PlaceNode? = null

    // 교차로 터치 이벤트 변수
    private var cross: DataBaseHelper.CrossNode? = null

    // 터치 처리
    private lateinit var gestureDetector: GestureDetector

    // DB
    private lateinit var db: DataBaseHelper

    private lateinit var floorsIndoor: List<DataBaseHelper.IndoorFloor>
    private lateinit var nodesPlace: List<DataBaseHelper.PlaceNode>
    private lateinit var nodesCross: List<DataBaseHelper.CrossNode>

    // 길찾기
    private lateinit var dijk: Dijkstra

    private lateinit var root: List<Triple<Int, String, String>>

    // 건물 정보 기본 set
    private var placeid: Int = 1
    private var floorid: Int = 4

    // 지도 좌표 비율
    var ratio = 0F

    // mapvar
    var startId: String? = null;
    var endId: String? = null;

    // 터치 on/off
    var interaction: Boolean = true

    // 뒤로 가기 버튼
    private var doubleBackToExitPressedOnce = false

    // 지도 크기
    var mScale = 0f

    override fun onCreate(savedInstanceState: Bundle?) { // onCreate 함수를 오버라이드. 이 함수는 액티비티가 생성될 때 호출됨.
        super.onCreate(savedInstanceState) // 부모 클래스의 onCreate 함수를 호출

        // DB
        db = DataBaseHelper(this)
        nodesPlace = db.getNodesPlace()
        nodesCross = db.getNodesCross()
        floorsIndoor = db.getFloorsIndoor()

        setContentView(R.layout.activity_main)

        // 지도
        map = findViewById(R.id.map)


        // 서치뷰, QR, 취소 버튼 레이아웃
        // 출발지, 목적지 입력 서치뷰
        searchView1 = findViewById(R.id.searchView1)
        searchView2 = findViewById(R.id.searchView2)
        // 두번째 searchView와 취소버튼이 나타남에 따라 레이아웃 비율 조절.
        searchView_layout = findViewById(R.id.searchView_layout)

        // 취소 버튼
        cancel = findViewById(R.id.cancel)
        // 두번째 서치뷰와 취소 버튼 레이아웃
        svAndCancel = findViewById(R.id.svAndCancel)
        // 자동완성
        val listView1 = findViewById<ListView>(R.id.listView1)
        val listView2 = findViewById<ListView>(R.id.listView2)


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



        // QR 촬영 버튼
        var qrButton: Button = findViewById(R.id.qrButton)


        // 층 수 스피너
        spinner = findViewById(R.id.spinner)



        // 지도 크기 제한
        map.maxScale = 1f

        // 화면 비율
        ratio = map.getResources().getDisplayMetrics().density.toFloat() // 화면에 따른 이미지의 해상도 비율



        // 자동 완성
        var autoComplete = ArrayList<String>()
        for(i in nodesPlace){
            autoComplete.add(i.id.toString())
            autoComplete.add(i.name)
        }
        val ACadapter: ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_list_item_1, autoComplete)
        listView1.adapter = ACadapter
        val autoCom = findViewById<FrameLayout>(R.id.autoCom)

        val autoCom2 = findViewById<FrameLayout>(R.id.autoCom2)
        listView2.adapter = ACadapter



        // 출발지와 목적지 입력 서치뷰 활성화.
        // 두번째 searchView 와 취소 버튼을 같이 나타나고 사라지게 조절.
        searchView1.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                ACadapter.filter.filter(newText)
                if (newText.isEmpty() && searchView2.query.isEmpty()) {
                    if(!interaction){
                        setSearchLayout(View.VISIBLE)
                    }
                    else{
                        setSearchLayout(View.GONE)
                    }
                    // 입력창이 비어있으면 안보이게.
                    autoCom.visibility = View.GONE
                }
                else if(newText.isNotEmpty()){
                    autoCom.visibility = View.VISIBLE
                    // 완전히 동일하게 입력되어도 확인을 누르기 전까진 안없어짐. -> 확인 안눌러도 완전이 동일하게 입력되면 안보이도록 수정.
                    var temp = db.findPlacetoID(newText, nodesPlace)
                    if(temp != null){
                        autoCom.visibility = View.GONE
                    }
                }

                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                if (query.isEmpty() && searchView2.query.isEmpty()) {
                    setSearchLayout(View.GONE)
                } else {
                    checkS1 = query
                    id = db.findPlacetoID(query, nodesPlace)
                    if(id != null){

                        setSearchLayout(View.VISIBLE)
                        // 키보드 없애기
                        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        inputMethodManager.hideSoftInputFromWindow(searchView1.windowToken, 0)

                        floorid = id!!.id / 100
                        spinner.setSelection(db.findIdxtoFloor(floorid, floorsIndoor))
                        val handler = Handler()
                        handler.postDelayed({
                            showInfo(id)
                        }, 500)
                    }
                    else{
                        Toast.makeText(applicationContext, "입력하신 장소가 없습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
                return true
            }

        })

        searchView2.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                ACadapter.filter.filter(newText)
                // searchView2의 입력 상태에 따라 처리
                if (newText.isEmpty() && searchView1.query.isEmpty()) {
                    if(!interaction){
                        setSearchLayout(View.VISIBLE)
                    }
                    else{
                        setSearchLayout(View.GONE)
                    }
                    autoCom2.visibility = View.GONE
                } else {
                    setSearchLayout(View.VISIBLE)
                    autoCom2.visibility = View.VISIBLE
                    if(newText.isNotEmpty()){
                        autoCom2.visibility = View.VISIBLE
                        if(db.findPlacetoID(newText, nodesPlace) != null){
                            autoCom2.visibility = View.GONE
                        }
                    }
                    else if(newText.isEmpty()){
                        autoCom2.visibility = View.GONE
                    }
                }
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                checkS2 = query
                id = db.findPlacetoID(query, nodesPlace)
                if(id != null){
                    // 키보드 없애기
                    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(searchView1.windowToken, 0)

                    floorid = id!!.id / 100
                    spinner.setSelection(db.findIdxtoFloor(floorid, floorsIndoor))
                    val handler = Handler()
                    handler.postDelayed({
                        showInfo(id)
                    }, 500)
                }
                else{
                    Toast.makeText(applicationContext, "입력하신 장소가 없습니다.", Toast.LENGTH_SHORT).show()
                }
                return true
            }
        })

        // 자동완성 아이템 선택
        listView1.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = parent.getItemAtPosition(position) // 선택된 아이템 가져오기
            // 선택된 아이템에 대한 처리 로직을 작성하세요
            // 예: 선택된 아이템을 텍스트뷰에 설정하거나 원하는 동작을 수행합니다
            searchView1.setQuery(selectedItem.toString(), true)
            // 자동완성 레이아웃을 숨깁니다
            autoCom.visibility = View.GONE
        }
        listView2.setOnItemClickListener { parent, view, position, id ->
            val selectedItem = parent.getItemAtPosition(position) // 선택된 아이템 가져오기
            // 선택된 아이템에 대한 처리 로직을 작성하세요
            // 예: 선택된 아이템을 텍스트뷰에 설정하거나 원하는 동작을 수행합니다
            searchView2.setQuery(selectedItem.toString(), true)
            // 자동완성 레이아웃을 숨깁니다
            autoCom2.visibility = View.GONE
        }



        // QR 촬영 버튼 활성화.
        qrButton.setOnClickListener{
            val intent = Intent(this, ScanActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE)
        }

        // 취소 버튼 활성화.
        cancel.setOnClickListener{
            searchView2.setQuery("", false)
            searchView1.setQuery("", false)
            setSearchLayout(View.GONE)
            map.clearPin()
            map.clearStartPin()
            map.clearEndPin()

            startId = null
            endId = null

            interaction = true

            testinfo.visibility = View.GONE
            testbtn.text = "탑승"
        }



        // 화장실 검색 버튼
        var toiletButton: Button = findViewById(R.id.btn_toilet)
        // 출입문 검색 버튼
        var enterButton: Button = findViewById(R.id.btn_enter)
        // 엘레베이터
        var elevatorButton: Button = findViewById(R.id.btn_elevator)

        // 층수 값 n = 400~499
        // 화장실 찾기 버튼 활성화. //1
        toiletButton.setOnClickListener(){
            map.clearPin()
            for (i in nodesPlace)
            {
                if(i.checkplace == 1)
                {
                    map.addPin(PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 1, R.drawable.pushpin_blue)
                }
            }
        }
        // 엘레베이터 찾기 버튼 활성화. //2
        elevatorButton.setOnClickListener(){
            map.clearPin()
            for (i in nodesPlace)
            {
                if(i.checkplace == 2)
                {
                    map.addPin(PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 1, R.drawable.pushpin_blue)
                }
            }
        }
        // 출입문 찾기 버튼 활성화. //3
        enterButton.setOnClickListener(){
            map.clearPin()
            for (i in nodesPlace)
            {
                if(i.checkplace == 3)
                {
                    map.addPin(PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 1, R.drawable.pushpin_blue)
                }
            }
        }



        // 정보창 설정
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
            if(checkS2 == id?.name || checkS2 == id?.id.toString()){
                searchView2.setQuery(null, false)
                endId = null
                map.clearEndPin()
                interaction = true
            }
            checkS1 = id?.name
            searchView1.setQuery(id?.name, false)
            setSearchLayout(View.VISIBLE)
            startId = id?.id.toString()
            map.clearStartPin()
            map.clearPin()
            map.addStartPin((PointF(id!!.x.toFloat()*ratio, id!!.y.toFloat()*ratio)),1, R.drawable.pushpin_blue)
            mapInit()
            info.visibility = View.GONE
        }

        // 도착 버튼 누르면 searchView2 채우기
        end.setOnClickListener{
            if(checkS1 == id?.name || checkS1 == id?.id.toString()){
                searchView1.setQuery(null, false)
                startId = null
                map.clearStartPin()
                interaction = true
            }
            checkS2 = id?.name
            searchView2.setQuery(id?.name, false)
            map.clearPin()
            map.clearEndPin()
            map.addEndPin((PointF(id!!.x.toFloat()*ratio!!, id!!.y.toFloat()*ratio!!)),1, R.drawable.pushpin_blue)
            endId = id?.id.toString()
            mapInit()
            info.visibility = View.GONE
        }


        // 확대 축소 버튼
        val plus: Button = findViewById(R.id.plus)
        val minus: Button = findViewById(R.id.minus)


        plus.setOnClickListener{
            val visibleRect = Rect()

//            val tempRect = map.visibleFileRect(visibleRect)
            val centerX = (visibleRect.left + visibleRect.right) / 2f
            val centerY = (visibleRect.top + visibleRect.bottom) / 2f
            mScale += 0.4f
            map.animateScaleAndCenter(mScale, PointF(centerX, centerY))?.start()

        }
        minus.setOnClickListener{
            mScale -= 0.4f
            map.animateScaleAndCenter(mScale, PointF(map.width*ratio, (map.height/2)*ratio))?.start()
        }


        // 비상 연락망 변수 추가
        val btn_emergency: Button = findViewById(R.id.btn_emergency)
        btn_emergency.setOnClickListener {
            showEmergencyPopup()
        }

        // 스피너에 항목 추가.
        val items: MutableList<String> = ArrayList()

        for (i in floorsIndoor) {
            if (i.placeid == placeid) {
                items.add(i.name)
            }
        }

        // 스피너 활성화
        val adapter: ArrayAdapter<String> = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter

        spinner.setSelection(1)

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                var drawableName: String?
                var drawableId: Int?
                val selectedItem: String = parent.getItemAtPosition(position) as String
                val sss = parent.getItemAtPosition(position) as String
                if (selectedItem == "Add New Item") {
                    // Do something
                }


                var floorNum = selectedItem.substring(0,1).toInt()
                drawableName = db.findMaptoFloor(floorNum, floorsIndoor)
                drawableId = resources.getIdentifier(drawableName, "drawable", packageName)
                floorid = floorNum
                map.setImage(ImageSource.resource(drawableId))

                map.clearPin()
                map.clearStartPin()
                map.clearEndPin()
                map.clearPin("icon")
                addIcon(nodesPlace, floorid)

//                if(interaction){
//                    val handler = Handler()
//                    handler.postDelayed({
//                        map.animateScaleAndCenter(0.5f,PointF(map.width*ratio, map.height*ratio))?.start()
//                    }, 500)
//                }
            }


            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }



        // 터치 이벤트
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                autoCom.visibility = View.GONE
                autoCom2.visibility = View.GONE
                // 키보드 없애기
                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(searchView1.windowToken, 0)
                if(interaction){
                    var pointt = map.viewToSourceCoord(e.x, e.y);
                    var x = pointt!!.x/ratio
                    var y = pointt!!.y/ratio

                    id = db.findPlacetoXY(x.toInt(), y.toInt(), nodesPlace, floorid)
                    if (id != null)
                    {
                        map.clearPin()
                        showInfo(id)
                    }
                    else if(id == null){
                        showInfo(null)
                    }
                    return true
                }
                else{
                    var pointt = map.viewToSourceCoord(e.x, e.y);
                    var x = pointt!!.x/ratio
                    var y = pointt!!.y/ratio
                    cross = db.findCrosstoXY(x.toInt(), y.toInt(), nodesCross, floorid)
                    if (cross != null) {
                        showCross(cross, root)
                    }
                    else if (cross == null) {
                        showCross(null, root)
                    }
                    return true
                }
            }
        })
        map.setOnTouchListener(View.OnTouchListener { view, motionEvent -> // OnTouchListner로 터치 이벤트 감지
            gestureDetector.onTouchEvent( // gestureDectector로 터치 이벤트 처리
                motionEvent
            )
        })
    }


    // 서치뷰 레이아웃 조절
    fun setSearchLayout(v: Int){
        var layoutParams = searchView_layout.layoutParams as LinearLayout.LayoutParams
        if(v == View.VISIBLE){
            svAndCancel.visibility = View.VISIBLE
            layoutParams.weight = 3f
            searchView_layout.layoutParams = layoutParams
        }
        else{
            svAndCancel.visibility = View.GONE
            layoutParams.weight = 1.3f
            searchView_layout.layoutParams = layoutParams
        }
    }

    // 뒤로가기 버튼
    override fun onBackPressed() {
        if(info.visibility == View.VISIBLE){
            info.visibility = View.GONE
            return
        }
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        this.doubleBackToExitPressedOnce = true
        Toast.makeText(this, "뒤로 버튼을 한 번 더 누르면 종료됩니다.", Toast.LENGTH_SHORT).show()

        Handler().postDelayed({
            doubleBackToExitPressedOnce = false
        }, 2000)
    }


    // 길 찾기
    fun mapInit()
    {
        testinfo = findViewById(R.id.testinfo)
        testbtn = findViewById(R.id.testbtn)

        var scid = db.findCrosstoID(startId?.toInt(), nodesCross)
        var ecid = db.findCrosstoID(endId?.toInt(), nodesCross)

        var root1 = mutableListOf<Triple<Int, String, String>>()
        var root2 = mutableListOf<Triple<Int, String, String>>()
        var startfloor = 0
        var endfloor = 0

        var check = 0

        if (startId != null && endId != null)
        {
            interaction = false
            map.clearStartPin()
            map.clearEndPin()

            dijk = Dijkstra(nodesCross, startId!!.toInt(), endId!!.toInt())
            root = dijk.findShortestPath(dijk!!.makeGraph())

            if (root[0].first / 100 != root[root.size - 1].first / 100) {
                startfloor = root[0].first / 100
                endfloor = root[root.size - 1].first / 100

                for (i in root) {
                    if (i.first / 100 == startfloor) {
                        root1.add(i)
                    }

                    else if (i.first / 100 == endfloor) {
                        root2.add(i)
                    }
                }
            }

            if (root1.isEmpty()) {
                map.addPin(PointF(scid!!.x.toFloat()*ratio, scid!!.y.toFloat()*ratio), 1, R.drawable.pushpin_blue)
                map.addPin(PointF(ecid!!.x.toFloat()*ratio, ecid!!.y.toFloat()*ratio), 1, R.drawable.pushpin_blue)

                makeLine()
            }
            else {
                testinfo.visibility = View.VISIBLE

                spinner.setSelection(db.findIdxtoFloor(startfloor, floorsIndoor))

                root = root1

                val handler = Handler()
                handler.postDelayed({
                    map.addPin(PointF(scid!!.x.toFloat()*ratio, scid!!.y.toFloat()*ratio), 1, R.drawable.pushpin_blue)

                    makeLine()
                }, 1000)

                testbtn.setOnClickListener(){
                    if (check == 0) {
                        spinner.setSelection(db.findIdxtoFloor(endfloor, floorsIndoor))

                        root = root2

                        val handler = Handler()
                        handler.postDelayed({
                            map.addPin(PointF(ecid!!.x.toFloat()*ratio, ecid!!.y.toFloat()*ratio), 1, R.drawable.pushpin_blue)

                            makeLine()
                        }, 500)

                        testbtn.text = "뒤로"

                        check = 1
                    }

                    else if (check == 1) {
                        spinner.setSelection(db.findIdxtoFloor(startfloor, floorsIndoor))

                        root = root1

                        val handler = Handler()
                        handler.postDelayed({
                            map.addPin(PointF(scid!!.x.toFloat()*ratio, scid!!.y.toFloat()*ratio), 1, R.drawable.pushpin_blue)

                            makeLine()
                        }, 500)

                        testbtn.text = "탑승"

                        check = 0
                    }
                }
            }
        }
    }



    // QR 촬영 후 데이터 값 받아옴.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val returnedData = data?.getStringExtra("QRdata")
            Toast.makeText(this, returnedData, Toast.LENGTH_SHORT).show()
            val parts = returnedData!!.split(" ")
            placeid = parts[0].toInt()
            floorid = parts[1].toInt() / 100

            spinner.setSelection(db.findIdxtoFloor(floorid, floorsIndoor))

            id = db.findPlacetoID(parts[1], nodesPlace)

            val handler = Handler()
            handler.postDelayed({
                showInfo(id)
            }, 600)

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

    fun showInfo(id: DataBaseHelper.PlaceNode?) {
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
            infoText1.setText(id?.name)
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

    fun showCross(cross: DataBaseHelper.CrossNode?, root: List<Triple<Int, String, String>>) {
        if (cross != null) {
            for (i in root) {
                if (cross.id == i.first) {
                    // 정보 사진
                    if (i.second == "east") {
                        infoPic1.setImageBitmap(cross.imgEast)
                    }
                    else if (i.second == "west") {
                        infoPic1.setImageBitmap(cross.imgWest)
                    }
                    else if (i.second == "south") {
                        infoPic1.setImageBitmap(cross.imgSouth)
                    }
                    else if (i.second == "north") {
                        infoPic1.setImageBitmap(cross.imgNorth)
                    }

                    infoPic2.setImageResource(choiceArrow(i.second, i.third))

                    break
                }
            }

            // 지명
            infoText1.setText(cross?.nodes.toString())
            info.visibility = View.VISIBLE

            map.animateScaleAndCenter(1f,PointF(cross.x.toFloat()*ratio, cross.y.toFloat()*ratio))?.start()
            return
        }
        else{
            info.visibility = View.GONE
        }
    }

    fun addIcon(nodesPlace: List<DataBaseHelper.PlaceNode>, floorId: Int) {
        for (i in nodesPlace) {
            if (i.id / 100 == floorId) {
                map.addPin("icon", PointF(i.x.toFloat()*ratio, i.y.toFloat()*ratio), 0, R.drawable.icon, 2.0f, 2.0f, i.nickname)
            }
        }
    }

    fun choiceArrow(second: String, third: String): Int {
        if (second == "east") {
            if (third == "south") {
                return R.drawable.east_arrow
            }
            else if (third == "north") {
                return R.drawable.west_arrow
            }
            else {
                return R.drawable.north_arrow
            }
        }
        else if (second == "west") {
            if (third == "north") {
                return R.drawable.east_arrow
            }
            else if (third == "south") {
                return R.drawable.west_arrow
            }
            else {
                return R.drawable.north_arrow
            }
        }
        else if (second == "south") {
            if (third == "west") {
                return R.drawable.east_arrow
            }
            else if (third == "east") {
                return R.drawable.west_arrow
            }
            else {
                return R.drawable.north_arrow
            }
        }
        else if (second == "north") {
            if (third == "east") {
                return R.drawable.east_arrow
            }
            else if (third == "west") {
                return R.drawable.west_arrow
            }
            else {
                return R.drawable.north_arrow
            }
        }
        else {
            return 0
        }
    }

    fun makeLine() {
        for (i in root)
        {
            var pointt = db.findCrosstoID(i.first, nodesCross!!)
            var tempX = pointt!!.x.toFloat()*ratio
            var tempY = pointt!!.y.toFloat()*ratio
            map.addLine(PointF(tempX, tempY), Color.GREEN)

            if ((i.first % 100) > 70) {
                if (i.third == "east") {
                    map.addPin(PointF(tempX, tempY), 1, R.drawable.east_arrow)
                }
                else if (i.third == "west") {
                    map.addPin(PointF(tempX, tempY), 1, R.drawable.west_arrow)
                }
                else if (i.third == "south") {
                    map.addPin(PointF(tempX, tempY), 1, R.drawable.south_arrow)
                }
                else if (i.third == "north") {
                    map.addPin(PointF(tempX, tempY), 1, R.drawable.north_arrow)
                }
            }
        }
    }
}