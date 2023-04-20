package com.example.asj_test

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
    var w: Float? = null
    var h: Float? = null
    var canvas: Canvas? = null // canvas 변수 추가

    /**
     * 지도(기본 이미지)위에 기본 Pin을 추가하고 나머지는 초기화합니다.
     * @param nPin Pin 이 표시될 좌표값입니다.
     */
    fun setPin(sPin: PointF?) {
        pinArray = arrayListOf()
        fixedArray = arrayListOf()
        imageArray = arrayListOf()
        pinArray.add(sPin!!)
        fixedArray.add(0)
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
    fun addPin(nPin: PointF?, fix: Int?, imageID: Int?)
    {
        pinArray.add(nPin!!)
        fixedArray.add(fix!!)
        imageArray.add(imageID!!)
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
        invalidate()
    }

    // init 부분에서 이 부분을 지우고 실행해봤는데 마커가 그려지지 않았음. 추후 해석 필.
    private fun initialise() {
        val density = resources.displayMetrics.densityDpi.toFloat()
        iPin = BitmapFactory.decodeResource(this.resources, R.drawable.pushpin_green)
        w = density / 420f * iPin!!.getWidth()
        h = density / 420f * iPin!!.getHeight()
        iPin = Bitmap.createScaledBitmap(iPin!!, w!!.toInt(), h!!.toInt(), true)
    }

    /**
     * 저장된 좌표들을 지도 위에 표시합니다.
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Don't draw pin before image is ready so it doesn't move around during setup.
        if (!isReady) {
            return
        }
        paint.isAntiAlias = true
        if (sPin != null && iPin != null) {
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
                val vX = vPin.x - image!!.width / 2 //(해당 좌표기준 좌측 위로 이미지가 생성됨)
                val vY = vPin.y - image!!.height
                canvas.drawBitmap(image!!, vX, vY, paint)
            }

        }
    }

    init {
        initialise()
    }
}