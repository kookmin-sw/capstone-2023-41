package com.example.capstone_kotlin

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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

    data class IndoorFloor(val placeid: Int, val floorid: Int, val mapname: String)

    data class PlaceNode(val placeid: Int, val id: Int, val name: String, val checkplace: Int,
                         val x: Int, val y: Int, val access: Int, val img1: Bitmap?, val img2: Bitmap?)
    data class CrossNode(val placeid: Int, val id: Int, val x: Int, val y: Int, val nodes: List<Triple<Int, Int, String>>,
                         val imgEast: Bitmap?, val imgWest: Bitmap?, val imgSouth: Bitmap?, val imgNorth: Bitmap?)

    val floorList = mutableListOf<IndoorFloor>()
    val placeList = mutableListOf<PlaceNode>()
    val crossList = mutableListOf<CrossNode>()

    fun getFloorsIndoor(): List<IndoorFloor> {
        val db = readableDatabase
        val floorsIndoorCursor = db.rawQuery("SELECT * FROM indoor_floors", null)

        floorsIndoorCursor?.let {
            while (it.moveToNext()) {
                val placeid = it.getInt(0)
                val floorid = it.getInt(1)
                val mapname = it.getString(2)

                floorList.add(IndoorFloor(placeid, floorid, mapname))
            }

            it.close()
        }

        return floorList
    }

    fun getNodesPlace(): List<PlaceNode> {
        val db = readableDatabase
        val nodesPlaceCursor = db.rawQuery("SELECT * FROM nodes_place", null)

        nodesPlaceCursor?.let {
            while (it.moveToNext()) {
                val placeid = it.getInt(0)
                val id = it.getInt(1)
                val name = it.getString(2)
                val checkplace = it.getInt(3)
                val x = it.getInt(4)
                val y = it.getInt(5)
                val access = it.getInt(6)

                val bytes1: ByteArray = it.getBlob(7)
                val bytes2: ByteArray = it.getBlob(8)

                val img1: Bitmap? = BitmapFactory.decodeByteArray(bytes1, 0, bytes1.size)
                val img2: Bitmap? = BitmapFactory.decodeByteArray(bytes2, 0, bytes2.size)

                placeList.add(PlaceNode(placeid, id, name, checkplace, x, y, access, img1, img2))
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
                val placeid = it.getInt(0)
                val id = it.getInt(1)
                val x = it.getInt(2)
                val y = it.getInt(3)
                val nodeList = it.getString(4)

                val bytesEast: ByteArray? = it.getBlob(5)
                val bytesWest: ByteArray? = it.getBlob(6)
                val bytesSouth: ByteArray? = it.getBlob(7)
                val bytesNorth: ByteArray? = it.getBlob(8)

                val nodes = nodeList.split(",")
                    .map { tripleString ->
                        val (id, distance, direction) = tripleString.split("_")
                        Triple(id.toInt(), distance.toInt(), direction)
                    }

                var imgEast: Bitmap? = null
                var imgWest: Bitmap? = null
                var imgSouth: Bitmap? = null
                var imgNorth: Bitmap? = null

                if (bytesEast != null) {
                    imgEast = BitmapFactory.decodeByteArray(bytesEast, 0, bytesEast.size)
                }

                if (bytesWest != null) {
                    imgWest = BitmapFactory.decodeByteArray(bytesWest, 0, bytesWest.size)
                }

                if (bytesSouth != null) {
                    imgSouth = BitmapFactory.decodeByteArray(bytesSouth, 0, bytesSouth.size)
                }

                if (bytesNorth != null) {
                    imgNorth = BitmapFactory.decodeByteArray(bytesNorth, 0, bytesNorth.size)
                }

                crossList.add(CrossNode(placeid, id, x, y, nodes, imgEast, imgWest, imgSouth, imgNorth))
            }

            it.close()
        }

        return crossList
    }

    fun findPlacetoXY(x: Int, y: Int, list: List<PlaceNode>): PlaceNode? {
        for (i in list) {
            if ((i.x - 25 <= x && x <= i.x + 25) && (i.y - 25 <= y && y <= i.y + 25)) {
                return i
            }
        }

        return null
    }

    fun findPlacetoID(id: Int?, list: List<PlaceNode>): PlaceNode? {
        for (i in list) {
            if (i.id == id) {
                return i
            }
        }

        return null
    }

    fun findCrosstoID(id: Int?, list: List<CrossNode>): CrossNode? {
        for (i in list) {
            if (i.id == id) {
                return i
            }
        }

        return null
    }

    fun findMaptoFloor(floorid: Int, list: List<IndoorFloor>): String? {
        for (i in list) {
            if (i.floorid == floorid) {
                return i.mapname
            }
        }

        return null
    }

    override fun onCreate(db: SQLiteDatabase) {}

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
}