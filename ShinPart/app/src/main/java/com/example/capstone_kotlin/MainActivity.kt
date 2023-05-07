package com.example.capstone_kotlin  // 파일이 속한 패키지 정의
import android.Manifest
import android.app.Activity
import android.content.Intent // Intent는 액티비티간 데이터를 전달하는데 사용된다.
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PointF
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

    private var isButtonsVisible = false

    // MapActivity 선언부
    var map: PinView? = null
    var gestureDetector: GestureDetector? = null

    var db : DataBaseHelper? = null
    var nodesPlace: List<DataBaseHelper.PlaceNode>? = null
    var nodesCross: List<DataBaseHelper.CrossNode>? = null

    var dijk : Dijkstra? = null
    var ratio : Float? = null

    // mapvar
    var startId: String? = null;
    var endId: String? = null;

    override fun onCreate(savedInstanceState: Bundle?) { // onCreate 함수를 오버라이드. 이 함수는 액티비티가 생성될 때 호출됨.
        super.onCreate(savedInstanceState) // 부모 클래스의 onCreate 함수를 호출
        setContentView(R.layout.activity_main)

        map = findViewById(R.id.map);

        // 출발지, 목적지 입력 서치뷰
        searchView1 = findViewById<SearchView>(R.id.searchView1)
        val searchView2 = findViewById<SearchView>(R.id.searchView2)

        // QR 촬영 버튼
        var qrButton: Button = findViewById(R.id.qrButton)

        // 층 수 스피너
        val spinner: Spinner = findViewById(R.id.spinner)

        // 정보창
        var info = findViewById<FrameLayout>(R.id.info)

        // 정보창에 띄울 사진들
        var infoPic1 = findViewById<ImageView>(R.id.infoPic1)
        var infoPic2 = findViewById<ImageView>(R.id.infoPic2)

        // 출발, 도착 버튼
        var start = findViewById<Button>(R.id.start)
        var end = findViewById<Button>(R.id.end)

        // 지도 크기 제한
        map!!.maxScale = 0.5f

        // QR 촬영 버튼 활성화.
        qrButton.setOnClickListener{
            val intent = Intent(this, ScanActivity::class.java)
            startActivityForResult(intent, REQUEST_CODE)
        }

        // 비상 연락망 변수 추가
        val btn_emergency: Button = findViewById(R.id.btn_emergency)
        btn_emergency.bringToFront()
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
                    if(query == "123"){
                        info.visibility = View.VISIBLE
                        isButtonsVisible = true
                    }
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
            searchView1.setQuery("429", true)
            startId = "429"
            Toast.makeText(applicationContext, startId, Toast.LENGTH_SHORT).show()
            mapInit()
        }

        // 도착 버튼 누르면 searchView2 채우기
        end.setOnClickListener{
            searchView2.setQuery("464", true)
            endId = "464"
            Toast.makeText(applicationContext, endId, Toast.LENGTH_SHORT).show()
            mapInit()
        }

        var touchTime = 0L



        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {

                var pointt = map?.viewToSourceCoord(e.x, e.y);
                if(check_area(pointt!!.x, pointt!!.y))
                {
                    if (info.visibility == View.VISIBLE) {
                        info.visibility = View.GONE
                    } else {
                        info.visibility = View.VISIBLE
                        infoPic1.setImageResource(R.drawable.pushpin_blue)
                        infoPic2.setImageResource(R.drawable.pushpin_blue)
                    }
                }
                else if (info.visibility == View.VISIBLE) {
                    info.visibility = View.GONE
                }
                /*
                map.setOnTouchListener { view, event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            val x = event.x
                            val y = event.y
                            val outPoint = IntArray(2)
                            map.getLocationOnScreen(outPoint)
                            val imageX = x - outPoint[0]
                            val imageY = y - outPoint[1]
                            // 클릭한 좌표값 사용하기
//                    Toast.makeText(this, "x: $imageX, y: $imageY", Toast.LENGTH_SHORT).show()
                            if (imageX < 500f && imageY < 200f){
                                if (isButtonsVisible) {
                                    info.visibility = View.GONE
                                } else {
                                    info.visibility = View.VISIBLE
                                }
                                isButtonsVisible = !isButtonsVisible
                                infoPic1.setImageResource(R.drawable.pushpin_blue)
                                infoPic2.setImageResource(R.drawable.pushpin_blue)
                            }
                            else if(imageX < 1000f && imageY < 500f){
                                if (isButtonsVisible) {
                                    info.visibility = View.GONE
                                } else {
                                    info.visibility = View.VISIBLE
                                }
                                isButtonsVisible = !isButtonsVisible
                                infoPic1.setImageResource(R.drawable.duck)
                                infoPic2.setImageResource(R.drawable.duck)
                            }
                        }
                    }
                    false
                }*/

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


        db = DataBaseHelper(this)
        nodesPlace = db!!.getNodesPlace()
        nodesCross = db!!.getNodesCross()

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

        var intent : Intent = getIntent()
        var qrData : String? = intent!!.getStringExtra("QRdata")
        if (qrData != null)
        {
            scid = db!!.findCrosstoID(startId!!.toInt(), nodesCross!!)
            intent.removeExtra("QRdata")
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
                var pointt = db!!.findCrosstoID(i.first.toInt(), nodesCross!!)
                map?.addLine(PointF(pointt!!.x.toFloat()*ratio!!, pointt!!.y.toFloat()*ratio!!), Color.GREEN)
            }
        }
    }



    // QR 촬영 후 데이터 값 받아옴.
    // 2023-05-27 15시 기준 현재 받아온 데이터 값을 searchView1 에 넣음.
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val returnedData = data?.getStringExtra("QRdata")
            Toast.makeText(this, returnedData, Toast.LENGTH_SHORT).show()
            searchView1.setQuery(returnedData, false)
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
    private fun check_area(x: Float, y: Float) : Boolean
    {
        var testX = x/ratio!!
        var testY = y/ratio!!

        var msg2 = testX.toString() + ":" + testY.toString()
        var id = db!!.findPlacetoXY(testX.toInt(), testY.toInt(), nodesPlace!!)
        if (id != null)
        {
            //map?.clearPin();
            map?.addPin(PointF(id!!.x.toFloat()*ratio!!, id!!.y.toFloat()*ratio!!), 1, R.drawable.pushpin_blue)
            return true
        }
        return false
    }


}


