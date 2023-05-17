package com.example.capstone_kotlin  // 파일이 속한 패키지 정의
import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent // Intent는 액티비티간 데이터를 전달하는데 사용된다.
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PointF
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity // AppCompatActivity 클래스를 임포트. AppCompatActivity는 안드로이드 앱에서 사용되는 기본 클래스
import android.os.Bundle // Bundle은 액티비티가 시스템에서 재생성될 때 데이터를 저장하고 다시 가져오는 데 사용
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.davemorrissey.labs.subscaleview.ImageSource
import com.google.zxing.integration.android.IntentIntegrator.REQUEST_CODE

// Activity 는 사용자와 상호작용 하기 위한 하나의 단위

// 5/16 기준 검색 기능 활성화 및 뒤로가기 버튼 활성화.
// 자동완성 기능 아직 미구현.
// 자동완성 넣을 경우 화면 비율 조정 때문에 화면이 뭉개짐.
// 위 두 기능 구현하면서 미처 발견한 여러 자잘한 문제 해결 및 아래 새로 발생한 문제 해결.

// 현재 문제점
// searchView1에 자주스 입력 후, 도착 버튼 누르면 s1, s2에 둘다 자주스가 입력됨. -> 해결
// 근데 자주스를 가리키는 단어가 429, 자주스 두개라서 s1에 429 입력하고 도착 버튼 누를 시 s1엔 429, s2엔 자주스 입력됨
// 지명을 어떻게 선정할 지 기준 필요.

// 출발지와 도착지를 같게 설정하면, 취소를 누르기 전까지 터치가 먹히지 않음.
// ex) 자주스 출발지로 선택 -> 테스트 목적지로 선택 -> 자주스 목적지로 선택 -> 터치 안먹음. -> 해결
// 추가 문제 발생 : 자주스 출발지 선택 -> 자주스 목적지 선택 -> 엘리베이터 목적지 선택 시 터치 안먹음. -> 해결
// 추가 문제 발생 : 위 문제 과정 후, 자주스 출발 지 선택 시 목적지 사라짐. -> 해결
// 위 문제에서 터치로 자주스 선택 시 서치뷰, 취소버튼 invisible -> 해결
// 추가 문제 발생 : 자주스 출발 -> 엘리베이터 도착 -> 자주스 도착 선택 시 터치 안먹음. -> 해결

// 뒤로가기 버튼 구현 후, 장소 한 곳 터치 -> 활성화된 정보창 뒤로가기로 제거 -> 다른 장소 터치 -> 기존 장소 핀 유지 문제 발생 -> 해결





class MainActivity : AppCompatActivity() {  // MainActivity정의, AppCompatActivity 클래스를 상속받음

    // 지도
    private lateinit var map: PinView

    // 서치뷰, QR, 취소 버튼 레이아웃
    private lateinit var searchView1: SearchView
    private lateinit var searchView2: SearchView
    private lateinit var searchView_layout: LinearLayout
    private lateinit var cancel: Button
    private lateinit var svAndCancel: LinearLayout

    private lateinit var svAdapter: ArrayAdapter<String>


    private var checkS1: String? = null
    private var checkS2: String? = null

    // 정보창 및 표시될 사진, 지명, 접근성
    private lateinit var info: FrameLayout
    private lateinit var infoPic1: ImageView
    private lateinit var infoPic2: ImageView
    private lateinit var infoText1: TextView
    private lateinit var infoText2: TextView

    // QR 촬영으로 값 받아올 변수
    private var id: DataBaseHelper.PlaceNode? = null

    // 터치 처리
    private lateinit var gestureDetector: GestureDetector

    // DB
    private lateinit var db: DataBaseHelper
    private lateinit var floorsIndoor: List<DataBaseHelper.IndoorFloor>
    private lateinit var nodesPlace: List<DataBaseHelper.PlaceNode>
    private lateinit var nodesCross: List<DataBaseHelper.CrossNode>

    // 길찾기
    private lateinit var dijk: Dijkstra

    // 지도 좌표 비율
    var ratio = 0F

    // mapvar
    var startId: String? = null;
    var endId: String? = null;

    // 터치 on/off
    var interaction: Boolean = true

    // 뒤로 가기 버튼
    private var doubleBackToExitPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) { // onCreate 함수를 오버라이드. 이 함수는 액티비티가 생성될 때 호출됨.
        super.onCreate(savedInstanceState) // 부모 클래스의 onCreate 함수를 호출
        setContentView(R.layout.activity_main)

        // 지도
        map = findViewById(R.id.map)

        // 서치뷰, QR, 취소 버튼 레이아웃
        // 출발지, 목적지 입력 서치뷰
        searchView1 = findViewById(R.id.searchView1)
        searchView2 = findViewById(R.id.searchView2)
        // 두번째 searchView와 취소버튼이 나타남에 따라 레이아웃 비율 조절.
        searchView_layout = findViewById(R.id.searchView_layout)
        var layoutParams = searchView_layout.layoutParams as LinearLayout.LayoutParams
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

        // QR 촬영 버튼
        var qrButton: Button = findViewById(R.id.qrButton)

        // DB
        db = DataBaseHelper(this)
        nodesPlace = db.getNodesPlace()
        nodesCross = db.getNodesCross()
        floorsIndoor = db.getFloorsIndoor()



        // 지도 설정
        val drawableName = db.findMaptoFloor(4, floorsIndoor)
        val drawableId = resources.getIdentifier(drawableName, "drawable", packageName)
        map.setImage(ImageSource.resource(drawableId))
        // 지도 크기 제한
        map.maxScale = 1f



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
                        svAndCancel.visibility = View.VISIBLE
                        layoutParams.weight = 3f
                        searchView_layout.layoutParams = layoutParams
                    }
                    else{
                        svAndCancel.visibility = View.GONE
                        layoutParams.weight = 1.3f
                        searchView_layout.layoutParams = layoutParams
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
                    svAndCancel.visibility = View.GONE
                    layoutParams.weight = 1.3f
                    searchView_layout.layoutParams = layoutParams
                } else {
                    checkS1 = query
                    id = db.findPlacetoID(query, nodesPlace)
                    if(id != null){
                        showInfo(id)
                        layoutParams.weight = 3f
                        searchView_layout.layoutParams = layoutParams
                        svAndCancel.visibility = View.VISIBLE
                        // 키보드 없애기
                        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        inputMethodManager.hideSoftInputFromWindow(searchView1.windowToken, 0)
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
                        svAndCancel.visibility = View.VISIBLE
                        layoutParams.weight = 3f
                        searchView_layout.layoutParams = layoutParams
                    }
                    else{
                        svAndCancel.visibility = View.GONE
                        layoutParams.weight = 1.3f
                        searchView_layout.layoutParams = layoutParams
                    }
                    autoCom2.visibility = View.GONE
                } else {
                    svAndCancel.visibility = View.VISIBLE
                    layoutParams.weight = 3f
                    searchView_layout.layoutParams = layoutParams
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
                    showInfo(id)
                    // 키보드 없애기
                    val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputMethodManager.hideSoftInputFromWindow(searchView1.windowToken, 0)
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
            svAndCancel.visibility = View.GONE
            map.clearPin()
            map.clearStartPin()
            map.clearEndPin()

            startId = null
            endId = null

            svAndCancel.visibility = View.GONE
            layoutParams.weight = 1.3f
            searchView_layout.layoutParams = layoutParams

            interaction = true
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
        // 출발, 도착 버튼
        var start = findViewById<Button>(R.id.start)
        var end = findViewById<Button>(R.id.end)

        // 출발 버튼 누르면 searchView1 채우기
        start.setOnClickListener{
            if(checkS2 == id?.name || checkS2 == id?.id.toString()){
                searchView2.setQuery(null, true)
                endId = null
                map.clearEndPin()
                interaction = true
            }
            checkS1 = id?.name
            searchView1.setQuery(id?.name, true)
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
                searchView1.setQuery(null, true)
                startId = null
                map.clearStartPin()
                interaction = true
            }
            checkS2 = id?.name
            searchView2.setQuery(id?.name, true)
            map.clearPin()
            map.clearEndPin()
            map.addEndPin((PointF(id!!.x.toFloat()*ratio!!, id!!.y.toFloat()*ratio!!)),1, R.drawable.pushpin_blue)
            endId = id?.id.toString()
            mapInit()
            info.visibility = View.GONE
        }



        // 비상 연락망 변수 추가
        val btn_emergency: Button = findViewById(R.id.btn_emergency)
        btn_emergency.setOnClickListener {
            showEmergencyPopup()
        }

        // 층 수 스피너
        val spinner: Spinner = findViewById(R.id.spinner)
        // 스피너에 항목 추가.
        val items: MutableList<String> = ArrayList()
        items.add("미래관 4층")
        items.add("미래관 3층")
        items.add("미래관 2층")
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
                    id = db.findPlacetoXY(x.toInt(), y.toInt(), nodesPlace)
                    if (id != null)
                    {
                        showInfo(id)
                    }
                    else if(id == null){
                        showInfo(null)
                    }
                    return true
                }
                else{
                    return false
                }
            }
        })
        map.setOnTouchListener(View.OnTouchListener { view, motionEvent -> // OnTouchListner로 터치 이벤트 감지
            gestureDetector.onTouchEvent( // gestureDectector로 터치 이벤트 처리
                motionEvent
            )
        })



        // 화면 비율
        ratio = map.getResources().getDisplayMetrics().density.toFloat() // 화면에 따른 이미지의 해상도 비율


    }

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

    fun mapInit()
    {

        var scid = db.findCrosstoID(startId?.toInt(), nodesCross)
        var ecid = db.findCrosstoID(endId?.toInt(), nodesCross)

        if (startId == endId){
            interaction = true
            return
        }
        if (startId != null && endId != null)
        {
            interaction = false
            map.clearStartPin()
            map.clearEndPin()
            map.addPin(PointF(scid!!.x.toFloat()*ratio, scid!!.y.toFloat()*ratio), 1, R.drawable.pushpin_blue)
            map.addPin(PointF(ecid!!.x.toFloat()*ratio, ecid!!.y.toFloat()*ratio), 1, R.drawable.pushpin_blue)

            dijk = Dijkstra(nodesCross, startId!!.toInt(), endId!!.toInt())
            var root = dijk.findShortestPath(dijk!!.makeGraph())
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



    // QR 촬영 후 데이터 값 받아옴.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val returnedData = data?.getStringExtra("QRdata")
            Toast.makeText(this, returnedData, Toast.LENGTH_SHORT).show()
            id = db.findPlacetoID(returnedData!!, nodesPlace)
            showInfo(id)
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
            map.clearPin()
            map.addPin(PointF(id.x.toFloat()*ratio, id.y.toFloat()*ratio), 1, R.drawable.pushpin_blue)
            return
        }
        else{
            info.visibility = View.GONE
            map.clearPin()
        }
    }
}


