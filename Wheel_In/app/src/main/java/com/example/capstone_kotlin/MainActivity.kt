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

    private lateinit var searchView1: SearchView

    // 두번째 서치뷰 관련
    private lateinit var searchView2: SearchView
    private lateinit var searchView_layout: LinearLayout
    private lateinit var cancel: Button
    private lateinit var svAndCancel: LinearLayout

    // 정보창 및 표시될 사진, 지명, 접근성
    private lateinit var info: FrameLayout
    private lateinit var infoPic1: ImageView
    private lateinit var infoPic2: ImageView
    private lateinit var infoText1: TextView
    private lateinit var infoText2: TextView

    // QR 촬영으로 값 받아올 변수
    private var id: DataBaseHelper.PlaceNode? = null

    // MapActivity 선언부
    private lateinit var map: PinView

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

    override fun onCreate(savedInstanceState: Bundle?) { // onCreate 함수를 오버라이드. 이 함수는 액티비티가 생성될 때 호출됨.
        super.onCreate(savedInstanceState) // 부모 클래스의 onCreate 함수를 호출
        setContentView(R.layout.activity_main)

        svAndCancel = findViewById(R.id.svAndCancel)
        //        테스트용
        cancel = findViewById(R.id.cancel)
        cancel.setOnClickListener{
            searchView2.setQuery("", true)
            searchView1.setQuery("", true)

            map.clearPin()
            map.clearStartPin()
            map.clearEndPin()

            startId = null
            endId = null

            interaction = true
        }

        map = findViewById(R.id.map);
        map.setImage(ImageSource.resource(R.drawable.mirae_4f))
        // 지도 크기 제한
        map.maxScale = 1f



        // 출발지, 목적지 입력 서치뷰
        searchView1 = findViewById(R.id.searchView1)
        searchView2 = findViewById(R.id.searchView2)

        // 두번째 searchView와 취소버튼이 나타남에 따라 레이아웃 비율 조절.
        searchView_layout = findViewById(R.id.searchView_layout)
        var layoutParams = searchView_layout.layoutParams as LinearLayout.LayoutParams

        // 출발지와 목적지 입력 서치뷰 활성화.
        // 두번째 searchView 와 취소 버튼을 같이 나타나고 사라지게 조절.
        searchView1.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                if (newText.isEmpty() && searchView2.query.isEmpty()) {
                    svAndCancel.visibility = View.GONE
                    layoutParams.weight = 1.3f
                    searchView_layout.layoutParams = layoutParams
                }
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                if (query.isEmpty() && searchView2.query.isEmpty()) {
                    svAndCancel.visibility = View.GONE
                    layoutParams.weight = 1.3f
                    searchView_layout.layoutParams = layoutParams
                } else {
                    layoutParams.weight = 3f
                    searchView_layout.layoutParams = layoutParams
                    svAndCancel.visibility = View.VISIBLE
                }
                return true
            }
        })

        searchView2.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextChange(newText: String): Boolean {
                // searchView2의 입력 상태에 따라 처리
                if (newText.isEmpty() && searchView1.query.isEmpty()) {
                    svAndCancel.visibility = View.GONE
                    layoutParams.weight = 1.3f
                    searchView_layout.layoutParams = layoutParams
                } else {
                    svAndCancel.visibility = View.VISIBLE
                    layoutParams.weight = 3f
                    searchView_layout.layoutParams = layoutParams
                }
                return true
            }

            override fun onQueryTextSubmit(query: String): Boolean {
                return true
            }
        })



        // QR 촬영 버튼
        var qrButton: Button = findViewById(R.id.qrButton)
        // QR 촬영 버튼 활성화.
        qrButton.setOnClickListener{
            val intent = Intent(this, ScanActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE)
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



        // 정보창
        info = findViewById(R.id.info)

        // 정보창에 띄울 정보들
        infoText1 = findViewById(R.id.text1)
        infoText2 = findViewById(R.id.text2)

        infoPic1 = findViewById(R.id.infoPic1)
        infoPic2 = findViewById(R.id.infoPic2)

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
            searchView2.setQuery(id?.name, true)
            map.clearPin()
            map.clearEndPin()
            map.addEndPin((PointF(id!!.x.toFloat()*ratio!!, id!!.y.toFloat()*ratio!!)),1, R.drawable.pushpin_blue)
            endId = id?.id.toString()
            mapInit()
            info.visibility = View.GONE
        }



        // DB
        db = DataBaseHelper(this)
        nodesPlace = db.getNodesPlace()
        nodesCross = db.getNodesCross()
        floorsIndoor = db.getFloorsIndoor()



        // 비상 연락망 변수 추가
        val btn_emergency: Button = findViewById(R.id.btn_emergency)
        btn_emergency.setOnClickListener {
            showEmergencyPopup()
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



        // 터치 이벤트
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
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


        // MapActivity 연결부
        val drawableName = db.findMaptoFloor(4, floorsIndoor)
        val drawableId = resources.getIdentifier(drawableName, "drawable", packageName)
        map.setImage(ImageSource.resource(drawableId))



        // 화면 비율
        ratio = map.getResources().getDisplayMetrics().density.toFloat() // 화면에 따른 이미지의 해상도 비율



    }

    fun mapInit()
    {

        var scid = db.findCrosstoID(startId?.toInt(), nodesCross)
        var ecid = db.findCrosstoID(endId?.toInt(), nodesCross)

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
            id = db.findPlacetoID(returnedData!!.toInt(), nodesPlace)
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
            map.addPin(PointF(id.x.toFloat()*ratio, id.y.toFloat()*ratio), 1, R.drawable.pushpin_blue)
            return
        }
        else{
            info.visibility = View.GONE
            map.clearPin()
        }
    }
}


