package com.example.sqlite

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.io.FileOutputStream
import java.io.IOException

class DataBaseHelper(private val context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "Nodes.db"
        private const val DATABASE_VERSION = 1
    }

    private val dbPath = context.applicationInfo.dataDir + "/databases/"

    init {
        if (!checkDatabase()) {
            copyDatabase()
        }
    }

    private fun checkDatabase(): Boolean {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        return dbFile.exists()
    }

    private fun copyDatabase() {
        try {
            val folder = context.getDatabasePath(DATABASE_NAME).parentFile
            if (!folder.exists()) {
                folder.mkdir()
            }

            val inputStream = context.assets.open(DATABASE_NAME)
            val outputStream = FileOutputStream(dbPath + DATABASE_NAME)

            val buffer = ByteArray(1024)
            var length: Int

            while (inputStream.read(buffer).also { length = it } > 0) {
                outputStream.write(buffer, 0, length)
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    data class PlaceNode(val idx: Int, val id: Int, val name: String, val x: Int, val y: Int, val x1: Int, val y1: Int, val x2: Int, val y2: Int)
    data class CrossNode(val idx: Int, val id: Int, val x: Int, val y: Int, val x1: Int, val y1: Int, val x2: Int, val y2: Int)

    val placeList = mutableListOf<PlaceNode>()
    val crossList = mutableListOf<CrossNode>()

    fun getNodesPlace(): List<PlaceNode> {
        val db = readableDatabase
        val nodesPlaceCursor = db.rawQuery("SELECT * FROM nodes_place", null)

        nodesPlaceCursor?.let {
            while (it.moveToNext()) {
                val idx = it.getInt(0)
                val id = it.getInt(1)
                val name = it.getString(2)
                val x = it.getInt(3)
                val y = it.getInt(4)

                placeList.add(PlaceNode(idx, id, name, x, y, x - 15, y - 15, x + 15, y + 15))
            }

            it.close()
        }

        return placeList
    }

    fun getNodesCross(): List<CrossNode> {
        val db = readableDatabase
        val nodesCrossCursor = db.rawQuery("SELECT * FROM nodes_cross", null)

        nodesCrossCursor?.let {
            while (it.moveToNext()) {
                val idx = it.getInt(0)
                val id = it.getInt(1)
                val x = it.getInt(2)
                val y = it.getInt(3)

                crossList.add(CrossNode(idx, id, x, y, x - 15, y - 15, x + 15, y + 15))
            }

            it.close()
        }

        return crossList
    }

    fun findID(x: Int, y: Int, list: List<PlaceNode>): Int {
        for (i in list) {
            if ((i.x1 <= x && x <= i.x2) && (i.y1 <= y && y <= i.y2)) {
                return i.id
            }
        }

        return 0
    }

    override fun onCreate(db: SQLiteDatabase) {}

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
}