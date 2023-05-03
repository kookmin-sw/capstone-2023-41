package com.example.sqlite

import android.os.Bundle
import android.graphics.BitmapFactory
import android.widget.TextView
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var textView: TextView
    private lateinit var imageView1: ImageView
    private lateinit var imageView2: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textView = findViewById(R.id.tv_result)

        val db = DataBaseHelper(this)

        val nodesPlace = db.getNodesPlace()
        val nodesCross = db.getNodesCross()

        var text = "list1\n\n"

        for (i in nodesPlace) {
            text += "${i.idx}, ${i.id}, ${i.name}, ${i.x}, ${i.y}, ${i.x1}, ${i.y1}, ${i.x2}, ${i.y2}, ${i.access}, ${i.crossid}\n"
        }

        text += "\nlist2\n\n"

        for (i in nodesCross) {
            text += "${i.idx}, ${i.id}, ${i.x}, ${i.y}, ${i.nodes}\n"
        }

        var place1: DataBaseHelper.PlaceNode? = db.findPlacetoXY(920, 440, nodesPlace)
        var place2: DataBaseHelper.PlaceNode? = db.findPlacetoID(464, nodesPlace)

        text += "\nid = ${place1?.id}\n"

        text += "\nx = ${place2?.x}, y = ${place2?.y}, CrossNode = ${db.findCrosstoID(place2?.crossid, nodesCross)?.nodes}"

        textView.text = text
/*
        imageView1 = findViewById(R.id.iv_img1)
        imageView2 = findViewById(R.id.iv_img2)

        imageView1.setImageBitmap(nodesPlace[1].img2)
        imageView2.setImageBitmap(nodesCross[2].imgNorth)*/
    }
}