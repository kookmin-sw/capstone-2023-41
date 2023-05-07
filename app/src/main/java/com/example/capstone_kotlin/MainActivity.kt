package com.example.capstone_kotlin  // 파일이 속한 패키지 정의
import android.Manifest
import android.content.Intent // Intent는 액티비티간 데이터를 전달하는데 사용된다.
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity // AppCompatActivity 클래스를 임포트. AppCompatActivity는 안드로이드 앱에서 사용되는 기본 클래스
import android.os.Bundle // Bundle은 액티비티가 시스템에서 재생성될 때 데이터를 저장하고 다시 가져오는 데 사용
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

// Activity 는 사용자와 상호작용 하기 위한 하나의 단위


class MainActivity : AppCompatActivity() {  // MainActivity정의, AppCompatActivity 클래스를 상속받음
    override fun onCreate(savedInstanceState: Bundle?) { // onCreate 함수를 오버라이드. 이 함수는 액티비티가 생성될 때 호출됨.
        super.onCreate(savedInstanceState) // 부모 클래스의 onCreate 함수를 호출
        setContentView(R.layout.activity_main)


        val searchView = findViewById<SearchView>(R.id.searchView)
        val searchEditTextId = searchView.context.resources.getIdentifier("android:id/search_src_text", null, null)
        val searchEditText = searchView.findViewById<EditText>(searchEditTextId)
        searchEditText.setHint("목적지를 입력하세요.")
        searchEditText.setHintTextColor(Color.DKGRAY)


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

        val btn_emergency: Button = findViewById(R.id.btn_emergency)

        btn_emergency.bringToFront()

        btn_emergency.setOnClickListener {
            showEmergencyPopup()
        }
    }


}