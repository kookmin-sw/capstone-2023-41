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

    data class IndoorFloor(val idx: Int, val placeid: Int, val floorid: Int, val name: String, val mapname: String)

    data class PlaceNode(val placeid: Int, val id: Double, val name: String, val nickname: String, val checkplace: Int,
                         val x: Int, val y: Int, val access: Int, val img1: Bitmap?, val img2: Bitmap?)
    data class CrossNode(val placeid: Int, val id: Double, val x: Int, val y: Int, val name: String?, val nodes: List<Triple<Double, Int, String>>,
                         val imgEast: Bitmap?, val imgWest: Bitmap?, val imgSouth: Bitmap?, val imgNorth: Bitmap?)

    val floorList = mutableListOf<IndoorFloor>()
    val placeList = mutableListOf<PlaceNode>()
    val crossList = mutableListOf<CrossNode>()

    fun getFloorsIndoor(): List<IndoorFloor> {
        val db = readableDatabase
        val floorsIndoorCursor = db.rawQuery("SELECT * FROM indoor_floors", null)

        floorsIndoorCursor?.let {
            while (it.moveToNext()) {
                val idx = it.getInt(0)
                val placeid = it.getInt(1)
                val floorid = it.getInt(2)
                val name = it.getString(3)
                val mapname = it.getString(4)

                floorList.add(IndoorFloor(idx, placeid, floorid, name, mapname))
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
                val id = it.getDouble(1)
                val name = it.getString(2)
                val nickname = it.getString(3)
                val checkplace = it.getInt(4)
                val x = it.getInt(5)
                val y = it.getInt(6)
                val access = it.getInt(7)

                val bytes1: ByteArray? = it.getBlob(8)
                val bytes2: ByteArray? = it.getBlob(9)

                var img1: Bitmap? = null
                var img2: Bitmap? = null

                if (bytes1 != null) {
                    img1 = BitmapFactory.decodeByteArray(bytes1, 0, bytes1.size)
                }

                if (bytes2 != null) {
                    img2 = BitmapFactory.decodeByteArray(bytes2, 0, bytes2.size)
                }

                placeList.add(PlaceNode(placeid, id, name, nickname, checkplace, x, y, access, img1, img2))
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
                val id = it.getDouble(1)
                val x = it.getInt(2)
                val y = it.getInt(3)
                val name = it.getString(4)
                val nodeList = it.getString(5)

                val bytesEast: ByteArray? = it.getBlob(6)
                val bytesWest: ByteArray? = it.getBlob(7)
                val bytesSouth: ByteArray? = it.getBlob(8)
                val bytesNorth: ByteArray? = it.getBlob(9)

                val nodes = nodeList.split(",")
                    .map { tripleString ->
                        val (id, distance, direction) = tripleString.split("_")
                        Triple(id.toDouble(), distance.toInt(), direction)
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

                crossList.add(CrossNode(placeid, id, x, y, name, nodes, imgEast, imgWest, imgSouth, imgNorth))
            }

            it.close()
        }

        return crossList
    }

    fun findIdxtoFloor(floorid: Int, list: List<IndoorFloor>): Int {
        for (i in list) {
            if (floorid == i.floorid) {
                return i.idx
            }
        }

        return 1
    }

    fun findPlacetoXY(x: Int, y: Int, list: List<PlaceNode>, floorid: Int): PlaceNode? {
        for (i in list) {
            if (i.id.toInt() / 100 == floorid) {
                if ((i.x - 50 <= x && x <= i.x + 50) && (i.y - 50 <= y && y <= i.y + 50)) {
                    return i
                }
            }
        }

        return null
    }

    fun findPlacetoID(id: Double, list: List<PlaceNode>): PlaceNode? {
        for (i in list) {
            if (i.id == id){
                return i
            }
        }

        return null
    }

    fun searchPlace(text: String, list: List<PlaceNode>): PlaceNode? {
        for (i in list) {
            if (i.name == text || i.nickname == text) {
                return i
            }
        }

        return null
    }

    fun findCrosstoXY(x: Int, y: Int, list: List<CrossNode>, floorid: Int): CrossNode? {
        for (i in list) {
            if (i.id.toInt() / 100 == floorid) {
                if ((i.x - 50 <= x && x <= i.x + 50) && (i.y - 50 <= y && y <= i.y + 50)) {
                    return i
                }
            }
        }

        return null
    }

    fun findCrosstoID(id: Double?, list: List<CrossNode>): CrossNode? {
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