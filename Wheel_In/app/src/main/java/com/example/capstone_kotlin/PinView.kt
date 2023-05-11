package com.example.capstone_kotlin

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView

class PinView @JvmOverloads constructor(context: Context?, attr: AttributeSet? = null) :
    SubsamplingScaleImageView(context, attr) {
    private val paint = Paint()
    private val vPin = PointF()
    private var sPin: PointF? = null
    private var iPin: Bitmap? = null
    private var pinArray = ArrayList<PointF>()
    private var fixedArray = ArrayList<Int>() // 0 : not fix, 1 : fix
    private var imageArray = ArrayList<Int>()
    private var lineArray = ArrayList<PointF>()
    private var lineColorArray = ArrayList<Int>()
    var w: Float? = null
    var h: Float? = null

    private var startPinArray = ArrayList<PointF>()
    private var startFixedArray = ArrayList<Int>() // 0 : not fix, 1 : fix
    private var startImageArray = ArrayList<Int>()

    private var endPinArray = ArrayList<PointF>()
    private var endFixedArray = ArrayList<Int>() // 0 : not fix, 1 : fix
    private var endImageArray = ArrayList<Int>()

    /**
     * 지도(기본 이미지)위에 기본 Pin을 추가하고 나머지는 초기화합니다.
     * @param nPin Pin 이 표시될 좌표값입니다.
     */
    fun setPin(sPin: PointF?) {
        pinArray = arrayListOf()
        fixedArray = arrayListOf()
        imageArray = arrayListOf()
        pinArray.add(sPin!!)
        fixedArray.add(1)
        imageArray.add(R.drawable.pushpin_blue)
        this.sPin = sPin
        //initialise()
        invalidate()
    }

    /**
     * 지도(기본 이미지)위에 표시할 Pin을 추가합니다.
     * @param nPin Pin 이 표시될 좌표값입니다.
     * @param fix Pin 이미지가 지도와 함께 축소, 확대될지 결정합니다. 0:적용 1:적용안함
     * @param imageID Pin 이미지의 ID 값입니다. (ex: R.drawable.image_name)
     */
    fun addPin(nPin: PointF?, fix: Int, imageID: Int?)
    {
        pinArray.add(nPin!!)
        fixedArray.add(fix!!)
        imageArray.add(imageID!!)
        invalidate()
    }

    fun addStartPin(nPin: PointF?, fix: Int, imageID: Int?)
    {
        startPinArray.add(nPin!!)
        startFixedArray.add(fix!!)
        startImageArray.add(imageID!!)
        invalidate()
    }

    fun addEndPin(nPin: PointF?, fix: Int, imageID: Int?)
    {
        endPinArray.add(nPin!!)
        endFixedArray.add(fix!!)
        endImageArray.add(imageID!!)
        invalidate()
    }

    /**
     * 지도(기본 이미지)위에 표시할 Point를 추가합니다. 이 Point들은 2개 이상일 경우 연결되어 선을 표시합니다.
     * @param point PointF 형식의 좌표입니다.
     * @param color 표현될 선의 색깔입니다. 1번 좌표, 2번 좌표 를 추가했을때 1번 좌표와 함께 입력된 색깔이 선의 색깔이 됩니다.
     */
    fun addLine(point: PointF?, color: Int?)
    {
        lineArray.add(point!!)
        lineColorArray.add(color!!)
        invalidate()
    }

    /**
     * 지도(기본 이미지)위에 표시된 Pin을 모두 제거합니다.
     */
    fun clearPin()
    {
        pinArray = arrayListOf()
        fixedArray = arrayListOf()
        imageArray = arrayListOf()
        lineArray = arrayListOf()
        lineColorArray = arrayListOf()
        invalidate()
    }
    fun clearStartPin()
    {
        startPinArray.clear()
        startFixedArray.clear()
        startImageArray.clear()
        lineArray.clear()
        lineColorArray.clear()
        invalidate()
    }
    fun clearEndPin()
    {
        endPinArray.clear()
        endFixedArray.clear()
        endImageArray.clear()
        lineArray.clear()
        lineColorArray.clear()
        invalidate()
    }

    private fun initialise() {
        invalidate();
        this.setScaleAndCenter(1f, PointF(0F,0F));
        invalidate()
    }

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
            myPaint.color = lineColorArray.get(i)
            var pointTmp1 = PointF()
            var pointTmp2 = PointF()
            sourceToViewCoord(lineArray.get(i), pointTmp1)
            sourceToViewCoord(lineArray.get(i+1),pointTmp2)
            canvas.drawLine(pointTmp1.x,pointTmp1.y,pointTmp2.x, pointTmp2.y, myPaint)
        }

        // Marker 파트
        var s = scale
        val density = resources.displayMetrics.densityDpi.toFloat()
        for(i in pinArray.indices)
        {
            var pin = pinArray.get(i)
            var fix = fixedArray.get(i)
            var imageId = imageArray.get(i)
            sourceToViewCoord(pin, vPin)
            var image = BitmapFactory.decodeResource(this.resources, imageId)
            w = density / 420f * image!!.getWidth()
            h = density / 420f * image!!.getHeight()

            if(fix == 1) // 확대 축소에 따라 크기가 변하지 않음
            {
                image = Bitmap.createScaledBitmap(image!!, (w!!).toInt(), (h!!).toInt(), true)
            }
            else // 확대 축소에 따라 크기가 변함
            {
                image= Bitmap.createScaledBitmap(image!!, (w!!*s).toInt(), (h!!*s).toInt(), true)
            }
            val vX = vPin.x - image!!.width / 2 //(/2가 없는 경우 해당 좌표기준 좌측 위로 이미지가 생성됨)
            val vY = vPin.y - image!!.height
            canvas.drawBitmap(image!!, vX, vY, paint)
        }

        for (i in startPinArray.indices){
            var pin = startPinArray.get(i)
            var fix = startFixedArray.get(i)
            var imageId = startImageArray.get(i)
            sourceToViewCoord(pin, vPin)
            var image = BitmapFactory.decodeResource(this.resources, imageId)
            w = density / 420f * image!!.getWidth()
            h = density / 420f * image!!.getHeight()

            if(fix == 1) // 확대 축소에 따라 크기가 변하지 않음
            {
                image = Bitmap.createScaledBitmap(image!!, (w!!).toInt(), (h!!).toInt(), true)
            }
            else // 확대 축소에 따라 크기가 변함
            {
                image= Bitmap.createScaledBitmap(image!!, (w!!*s).toInt(), (h!!*s).toInt(), true)
            }
            val vX = vPin.x - image!!.width / 2 //(/2가 없는 경우 해당 좌표기준 좌측 위로 이미지가 생성됨)
            val vY = vPin.y - image!!.height
            canvas.drawBitmap(image!!, vX, vY, paint)
        }

        for (i in endPinArray.indices){
            var pin = endPinArray.get(i)
            var fix = endFixedArray.get(i)
            var imageId = endImageArray.get(i)
            sourceToViewCoord(pin, vPin)
            var image = BitmapFactory.decodeResource(this.resources, imageId)
            w = density / 420f * image!!.getWidth()
            h = density / 420f * image!!.getHeight()

            if(fix == 1) // 확대 축소에 따라 크기가 변하지 않음
            {
                image = Bitmap.createScaledBitmap(image!!, (w!!).toInt(), (h!!).toInt(), true)
            }
            else // 확대 축소에 따라 크기가 변함
            {
                image= Bitmap.createScaledBitmap(image!!, (w!!*s).toInt(), (h!!*s).toInt(), true)
            }
            val vX = vPin.x - image!!.width / 2 //(/2가 없는 경우 해당 좌표기준 좌측 위로 이미지가 생성됨)
            val vY = vPin.y - image!!.height
            canvas.drawBitmap(image!!, vX, vY, paint)
        }
    }

    init {

        initialise()
    }
}