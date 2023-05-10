package com.example.capstone_kotlin

import com.example.capstone_kotlin.DataBaseHelper
import java.util.*
import kotlin.collections.ArrayList

class Dijkstra(val nodesCross: List<DataBaseHelper.CrossNode>, val startID: Int, val endID: Int) {
    data class Node(val index: Int, val distance: Int)

    fun makeGraph(): List<Triple<Int, Int, Int>> {
        var graph = mutableListOf<Triple<Int, Int, Int>>()

        for (i in nodesCross) {
            for (j in i.nodes) {
                graph.add(Triple(i.id, j.first, j.second))
            }
        }

        return graph
    }

    fun findDist(distances: ArrayList<Pair<Int, Int>>, first: Int): Int {
        for (i in distances) {
            if (i.first == first) {
                return i.second
            }
        }

        return Int.MAX_VALUE
    }

    fun findVisit(visited: ArrayList<Pair<Int, Boolean>>, first: Int): Boolean {
        for (i in visited) {
            if (i.first == first) {
                return i.second
            }
        }

        return false
    }

    fun findPrev(previous: ArrayList<Pair<Int, Int>>, first: Int): Int {
        for (i in previous) {
            if (i.first == first) {
                return i.second
            }
        }

        return -1
    }

    fun findDirect(firstID: Int, secondId: Int): String {
        for (i in nodesCross) {
            for (j in i.nodes) {
                if (i.id == firstID && j.first == secondId) {
                    return j.third
                }
            }
        }

        return ""
    }

    fun findShortestPath(graph: List<Triple<Int, Int, Int>>): List<Pair<Int, String>> {
        val distances = ArrayList<Pair<Int, Int>>()
        val visited = ArrayList<Pair<Int, Boolean>>()
        val previous = ArrayList<Pair<Int, Int>>()

        for (i in nodesCross) {
            if (i.id == startID) {
                distances.add(Pair(i.id, 0))
            }
            else {
                distances.add(Pair(i.id, Int.MAX_VALUE))
            }

            visited.add(Pair(i.id, false))
            previous.add(Pair(i.id, -1))
        }

        val queue = PriorityQueue<Node>(compareBy { it.distance })
        queue.offer(Node(startID, 0))

        while (queue.isNotEmpty()) {
            val currentNode = queue.poll()
            val currentIndex = currentNode.index

            if (findVisit(visited, currentIndex)) {
                continue
            }

            visited.remove(Pair(currentIndex, findVisit(visited, currentIndex)))
            visited.add(Pair(currentIndex, true))

            for (edge in graph) {
                if (edge.first == currentIndex) {
                    val neighborIndex = edge.second
                    val neighborDistance = edge.third

                    val newDistance = findDist(distances, currentIndex) + neighborDistance

                    if (newDistance < findDist(distances, neighborIndex)) {
                        distances.remove(Pair(neighborIndex, findDist(distances, neighborIndex)))
                        distances.add(Pair(neighborIndex, newDistance))

                        previous.remove(Pair(neighborIndex, findPrev(previous, neighborIndex)))
                        previous.add(Pair(neighborIndex, currentIndex))

                        queue.offer(Node(neighborIndex, newDistance))
                    }
                }
            }
        }

        return buildPath(previous)
    }

    fun buildPath(previous: ArrayList<Pair<Int, Int>>): List<Pair<Int, String>> {
        val path = mutableListOf<Int>()
        var currentIndex = endID

        while (currentIndex != -1) {
            path.add(currentIndex)
            currentIndex = findPrev(previous, currentIndex)
        }

        path.reverse()

        return bulidDirect(path)
    }

    fun bulidDirect(path: List<Int>): List<Pair<Int, String>> {
        val result = ArrayList<Pair<Int, String>>()

        result.add(Pair(path[0], "start"))

        for (i in 1..path.size - 2) {
            result.add(Pair(path[i], findDirect(path[i - 1], path[i])))
        }

        result.add(Pair(path[path.size - 1], "end"))

        return result
    }
}