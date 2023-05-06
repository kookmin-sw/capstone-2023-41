package com.example.capstone_kotlin

import android.content.Context
import android.graphics.*
import android.util.AttributeSet

/*
PinView 클래스는 SubsamplingScaleImageView 클래스를 상속받아서 만들어진 클래스임.
 SubsamplingScaleImageView는 지도 이미지 위에 핀을 그리고 핀 이미지를 변경할 수 있도록 구현된 클래스이다.
 지도 이미지를 확대 및 축소할 수 있도록 지원하며, 핀을 그릴 수 있도록 onDraw() 함수를 오버라이딩할 수 있다.

 해당 PinView 클래스는 맵 액티비티에서 사용된다.
 Map Activity에서는 PinView 객체를 생성하여 이미지를 지정하고, 핀을 그리는 등의 작업을 수행함.
*/

import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView

class PinView @JvmOverloads constructor(context: Context?, attr: AttributeSet? = null) :
    SubsamplingScaleImageView(context, attr) {
    private val paint = Paint()
    private val vPin = PointF()
    private var sPin: PointF? = null
    private var iPin: Bitmap? = null

    // 지도 이미지 위에 여러 개의 핀을 그리기 위해 ArrayList를 사용
    private var pinArray = ArrayList<PointF>()
    private var fixedArray = ArrayList<Int>() // 0 : not fix, 1 : fix
    private var imageArray = ArrayList<Int>()
    private var lineArray = ArrayList<PointF>()
    private var lineColorArray = ArrayList<Int>()
    var w: Float? = null
    var h: Float? = null

    /**
     * 지도(기본 이미지)위에 기본 Pin을 추가하고 나머지는 초기화합니다.
     * @param nPin Pin 이 표시될 좌표값입니다.
     */

    // 핀을 그리는 함수. 이 함수로 지도상에 핀을 그림. 함수 호출시 기존의 핀은 모두 지워지고 새로운 핀이 그려짐.
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

    // addPin() 함수를 사용하여 핀을 추가하면서 핀의 좌표, 핀 이미지, 핀이 지도와 함께 축소/확대되는지를 나타내는 플래그를 설정
    fun addPin(nPin: PointF?, fix: Int?, imageID: Int?)
    {
        pinArray.add(nPin!!)
        fixedArray.add(fix!!)
        imageArray.add(imageID!!)
        invalidate()
    }

    // 두 개의 좌표를 입력하면 두 좌표 사이에 선을 그림. 라인의 색상은 파라미터로 전달된 값에 따라 달라짐.
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

    // init 부분에서 이 부분을 지우고 실행해봤는데 마커가 그려지지 않았음. 추후 해석 필.
    private fun initialize() {
        val density = resources.displayMetrics.densityDpi.toFloat()
        iPin = BitmapFactory.decodeResource(this.resources, R.drawable.duck)
        w = density / 420f * iPin!!.getWidth()
        h = density / 420f * iPin!!.getHeight()
        iPin = Bitmap.createScaledBitmap(iPin!!, w!!.toInt(), h!!.toInt(), true)
    }

    /**
     * 저장된 좌표들을 지도 위에 표시합니다.
     */

    // onDraw() : 저장된 좌표들을 지도위에 표시하는 역할
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 이미지가 준비되기 전에는 핀을 그리지 않아 설정 중에 이동하지 않도록 함.
        if (!isReady) {
            return
        }
        paint.isAntiAlias = true
        if (sPin != null && iPin != null) {
            // 선 그리기 파트

            // Paint 객체를 사용하여 선의 두께, 스타일, 안티 앨리어싱을 설정하고 for문으로
            // 각 좌표에 선을 그림.
            val myPaint = Paint()
            myPaint.strokeWidth = 10f
            myPaint.style = Paint.Style.FILL
            myPaint.isAntiAlias = true
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

            // 마커 그리기 파트

            var s = scale // 스케일 변수 선언
            val density = resources.displayMetrics.densityDpi.toFloat() // 밀도 설정
            for(i in pinArray.indices) // for 문으로 각 좌표에 마커를 그림.
            {
                var pin = pinArray.get(i)
                var fix = fixedArray.get(i)
                var imageId = imageArray.get(i)
                sourceToViewCoord(pin, vPin)
                var image = BitmapFactory.decodeResource(this.resources, imageId)
                w = density / 420f * image!!.getWidth()
                h = density / 420f * image!!.getHeight()

                // if..else : 마커의 크기가 지도의 확대 축소에 따라 변하는지 여부에 따라 이미지 크기를 조절

                if(fix == 1) // 확대 축소에 따라 크기가 변하지 않음
                {
                    image = Bitmap.createScaledBitmap(image!!, (w!!).toInt(), (h!!).toInt(), true)
                }
                else // 확대 축소에 따라 크기가 변함
                {
                    image= Bitmap.createScaledBitmap(image!!, (w!!*s).toInt(), (h!!*s).toInt(), true)
                }

                // 마커 이미지의 중심이 좌표에 위치하도록 이미지의 너비와 높이를 조절한 후 캔버스에 그림.

                val vX = vPin.x - image!!.width / 2 //(/2가 없는 경우 해당 좌표기준 좌측 위로 이미지가 생성됨)
                val vY = vPin.y - image!!.height
                canvas.drawBitmap(image!!, vX, vY, paint)
            }
        }
    }

    // init 블록에서 initialize()함수를 호출하여 초기화 작업 수행.
    init {
        initialize()
    }
}