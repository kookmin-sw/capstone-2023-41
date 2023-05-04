// QR코드 스캔을 담당하는 액티비티

package com.example.capstone_kotlin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator

class ScanActivity : AppCompatActivity(), View.OnClickListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // activity_scan.xml 레이아웃을 이 액티비티에 표시
        setContentView(R.layout.activity_scan)

        // 스캔 버튼 초기화
        val scanBtn = findViewById<Button>(R.id.scanBtn) as Button
        // 클릭 리스너 설정
        scanBtn.setOnClickListener(this)
    }

    override fun onClick(p0: View?) {
        scanCode() // 스캔 함수 호출
    }

    // QR코드 스캔 함수
    private fun scanCode() {

        // IntentIntegrator 클래스의 인스턴스 생성
        val integrator = IntentIntegrator(this)

        // 화면 방향 잠금 해제
        integrator.setOrientationLocked(false)

        // 스캔할 바코드 타입 설정 ( 모든 유형의 바코드 스캔 )
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES)

        // 스캔할 때 출력될 메시지 설정
        integrator.setPrompt("출발지 QR코드를 스캔해주세요.")

        // 전면 카메라로 변경 ( 선택 할 수 있음)
        integrator.setCameraId(1)

        // 스캔 시작
        integrator.initiateScan()
    }


    // QR코드 스캔 결과 처리 함수
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {

        // 스캔 결과를 가져오기 위해 IntentIntegrator 클래스의 parseActivityResult 함수 호출
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)

        // 스캔 결과가 null이 아닐 경우
        if (result != null) {
            // 스캔 결과가 비어있지 않은 경우
            if (result.contents != null) {

                // MainActivity로 결과 전송을 위한 Intent 객체 생성
                val intent : Intent = Intent(this@ScanActivity, MainActivity::class.java);
                intent.putExtra("QRdata", result.contents)

                //ResultActivity 내에서 ...
                //Intent intent = getIntent();
                //String myData = intent.getStringExtra("QRdata");

                // 스캔 결과 출력을 위한 AlertDialog.Builder 객체 생성
                val builder = AlertDialog.Builder(this)
                builder.setMessage(result.contents)
                builder.setTitle("Scanning Result")

                // 스캔 다시 실행 버튼
                builder.setPositiveButton(
                    "Scan Again"
                ) { dialogInterface, i -> scanCode() }.setNegativeButton(
                    "finish"
                ) { dialogInterface, i -> finish() }

                // AlertDialog 생성 및 출력
                val dialog = builder.create()
                dialog.show()
            } else {
                Toast.makeText(this, "QR코드에 아무것도 담겨있지 않아요.", Toast.LENGTH_LONG).show()
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}