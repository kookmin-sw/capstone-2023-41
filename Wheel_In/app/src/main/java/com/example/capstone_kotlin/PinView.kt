package com.example.capstone_kotlin

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView

class PinView @JvmOverloads constructor(context: Context?, attr: AttributeSet? = null) :
    SubsamplingScaleImageView(context, attr) {

    data class Pin(
        var id: String,
        var point: PointF,
        var fixed: Int,
        var imageId: Int,
        var width: Float,
        var height: Float,
        var text: String
    )

    data class Line(var id: String, var point: PointF, var color: Int)

    private val paint = Paint()
    private val vPin = PointF()
    private var sPin: PointF? = null
    private var iPin: Bitmap? = null
    private var pinArray = ArrayList<Pin>()
    private var lineArray = ArrayList<Line>()
    var w: Float? = null
    var h: Float? = null

    fun addStartPin(nPin: PointF?, fix: Int, imageID: Int?) {
        addPin(nPin!!, fix, imageID!!, id = "start")
        invalidate()
    }

    fun addEndPin(nPin: PointF?, fix: Int, imageID: Int?) {
        addPin(nPin!!, fix, imageID!!, id = "end")
        invalidate()
    }

    fun clearStartPin() {
        clearPin("start")
        lineArray = arrayListOf()
        invalidate()
    }

    fun clearEndPin() {
        clearPin("end")
        lineArray = arrayListOf()
        invalidate()
    }

    /**
     * 지도(기본 이미지)위에 표시할 Pin을 추가합니다.
     * @param point Pin 이 표시될 좌표값입니다.
     * @param fix Pin 이미지가 지도와 함께 축소, 확대될지 결정합니다. 0:적용 1:적용안함
     * @param imageID Pin 이미지의 ID 값입니다. (ex: R.drawable.image_name)
     */
    fun addPin(
        point: PointF,
        fix: Int = 0,
        imageID: Int,
        id: String = "0",
        width: Float = 2.0f,
        height: Float = 1.0f,
        text: String = ""
    ) {
        pinArray.add(Pin(id, point, fix, imageID, width, height, text))
        invalidate()
    }

    // 설정된 자료형 기준 순서 위는 기존 설계 + 제작하면서 추가된 순서 - 기존 코드 변경을 막기 위함.
    fun addPin(
        id: String = "0",
        nPin: PointF,
        fix: Int = 0,
        imageID: Int,
        width: Float = 2.0f,
        height: Float = 1.0f,
        text: String = ""
    ) {
        pinArray.add(Pin(id, nPin, fix, imageID, width, height, text))
        invalidate()
    }

    // 설정된 자료형 기준 순서 + width , height 의 자료형이 Int
    fun addPin(
        id: String = "0",
        nPin: PointF,
        fix: Int = 0,
        imageID: Int = R.drawable.bluepin,
        width: Int = 2,
        height: Int = 1,
        text: String = ""
    ) {
        pinArray.add(Pin(id, nPin, fix, imageID, width.toFloat(), height.toFloat(), text))
        invalidate()
    }

    /**
     * 지도(기본 이미지)위에 표시할 Point를 추가합니다. 이 Point들은 2개 이상일 경우 연결되어 선을 표시합니다.
     * @param point PointF 형식의 좌표입니다.
     * @param color 표현될 선의 색깔입니다. 1번 좌표, 2번 좌표 를 추가했을때 1번 좌표와 함께 입력된 색깔이 선의 색깔이 됩니다.
     */
    fun addLine(point: PointF, color: Int, id: String = "0") {
        lineArray.add(Line(id, point, color))
        invalidate()
    }

    /**
     * 지도(기본 이미지)위에 표시된 id == 0인 Pin을 모두 제거합니다.
     * id = 0 은 이 PinView에서 사용되는 기본값 입니다.
     */
    fun clearPin()
    {
        clearPin("0")
        lineArray = arrayListOf()
        invalidate()
    }

    /**
     * @param id 조건으로 사용되는 id 입니다.
     * 지도 위에 표시된 id == clearPin.id 인 Pin을 제외하고 모두 제거합니다.
     * 해당 id의 핀을 모두 제거하려면 removePin을 사용하세요.
     */
    fun clearPin(id:String)
    {
        var removed = pinArray.filterIndexed { index, value -> value.id != id }
        pinArray = removed as ArrayList<Pin>
        lineArray = arrayListOf()
        invalidate()
    }


    /**
     * @param id 조건으로 사용되는 id 입니다.
     * 지도 위에 표시된 id == clearPin.id 인 Pin을 모두 제거합니다.
     */
    fun cleanOtherPin(id:String)
    {
        var removed = pinArray.filterIndexed { index, value -> value.id == id }
        pinArray = removed as ArrayList<Pin>
    }

    private fun initialise() {invalidate()}

    /**
     * 저장된 좌표, 라인들을 지도 위에 표시합니다.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Don't draw pin before image is ready so it doesn't move around during setup.
        if (!isReady) {
            return
        }
        paint.isAntiAlias = true

        // Line 파트
        // Line 관련 정보 설정
        val myPaint = Paint()
        myPaint.strokeWidth = 10f
        myPaint.style = Paint.Style.FILL
        myPaint.isAntiAlias = true
        myPaint.strokeCap = Paint.Cap.ROUND
        for (i in 0..lineArray.size)
        {
            if (i >= lineArray.size-1)
            {
                break
            }
            var line = lineArray.get(i)
            myPaint.color = line.color
            var pointTmp1 = PointF()
            var pointTmp2 = PointF()
            sourceToViewCoord(line.point, pointTmp1)
            sourceToViewCoord(lineArray.get(i+1).point, pointTmp2)
            canvas.drawLine(pointTmp1.x, pointTmp1.y,pointTmp2.x, pointTmp2.y, myPaint)
        }

        // Marker 파트
        var s = scale
        val density = resources.displayMetrics.densityDpi.toFloat()
        myPaint.textAlign = Paint.Align.CENTER
        val options = BitmapFactory.Options() // Bitmap 옵션 객체 생성
//        options.inScaled = false // 이미지 스케일링 비활성화

        for(i in pinArray)
        {
            var pin = i.point
            var fix = i.fixed
            var imageId = i.imageId

            sourceToViewCoord(pin, vPin)
            var image = BitmapFactory.decodeResource(this.resources, imageId, options)
            w = density / 420f * image!!.getWidth()
            h = density / 420f * image!!.getHeight()

            if(fix == 1) // 확대 축소에 따라 크기가 변하지 않음
            {
                image = Bitmap.createScaledBitmap(image!!, (w!!).toInt(), (h!!).toInt(), true)
                myPaint.textSize = 50.0f
            }
            else // 확대 축소에 따라 크기가 변함
            {
                image= Bitmap.createScaledBitmap(image!!, (w!!*s).toInt(), (h!!*s).toInt(), true)
                myPaint.textSize = 50.0f * s
            }
            val vX = vPin.x - image!!.width / i.width //(/2가 없는 경우 해당 좌표기준 좌측 위로 이미지가 생성됨)
            val vY = vPin.y - image!!.height / i.height
            canvas.drawBitmap(image!!, vX, vY, paint)
            myPaint.setColor(Color.BLUE)
            canvas.drawText(i.text, vPin.x, vPin.y+myPaint.textSize+20, myPaint)
        }
    }

    init {initialise()}
}