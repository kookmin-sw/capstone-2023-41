package com.example.dijkstra

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.example.sqlite.DataBaseHelper

class MainActivity : AppCompatActivity() {
    private lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val db = DataBaseHelper(this)
        val nodesCross = db.getNodesCross()
        val dijk = Dijkstra(nodesCross, nodesCross[1].id, nodesCross[5].id)
        var text = "${dijk.findShortestPath(dijk.makeGraph())}"

        textView = findViewById(R.id.tv_result)
        textView.text = text
    }
}