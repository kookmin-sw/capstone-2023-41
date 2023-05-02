import java.util.*

// near : 인접 노드, weight : 거리
class Edge(val near: Int, val weight: Int)

// graph : 전체 노드(각 노드 별로 인접 노드와 거리 가짐)
// start : 시작 노드
// end : 도착 노드
fun dijkstra(graph: Array<ArrayList<Edge>>, start: Int, end: Int): Pair<Int, List<Int>> {

    // dist : 최단 거리 담을 배열. 초기엔 max_value 초기화
    val dist = IntArray(graph.size) { Int.MAX_VALUE }
    dist[start] = 0 // 시작 노드는 거리를 0으로.

    // 가장 짧은 노드 선택하기 위해 우선순위 큐
    val pq = PriorityQueue<Pair<Int, Int>>(compareBy { it.second })
    pq.add(start to 0) // 출발노드 
    
    // prev : 최단 경로상에서 현재 노드의 직전 노드 기록
    val path = IntArray(graph.size) { -1 }

    while (pq.isNotEmpty()) {
        // poll : 우선순위 큐에서 헤드(= 가장 짧은 거리의 노드) 추출
        val (cur, curDist) = pq.poll()

        // 도착 노드에 도달하면 현 최단거리 리턴하고 종료
        if (cur == end) {
            val path2 = path.distinct().toMutableList()
            for (i in path2.indices.reversed()){
                if (path2[i] == -1){
                    path2.removeAt(i)
                }
            }
            return curDist to path2
        }

        // 현재 노드를 기준으로 인접노드들 불러옴.
        for (edge in graph[cur]) {
            // 시작노드 ~ 현재노드 거리 + 현재노드 ~ 인접노드 거리
            // 거리 비교 후 더 짧은 걸로 갱신
            val newDist = curDist + edge.weight
            if (newDist < dist[edge.near]) {
                dist[edge.near] = newDist
                pq.add(edge.near to newDist)
                path[edge.near] = cur // 최단 경로 상의 바로 직전 노드 기록
                
            }
        }
    }
    return -1 to ArrayList() // 도착 노드에 도달하지 못한 경우 -1 반환
}


fun main() {
    val graph = arrayOf(
        arrayListOf(Edge(1, 5)),
        arrayListOf(Edge(0,5), Edge(2,11), Edge(4,5)),
        arrayListOf(Edge(1, 11),Edge(5, 6)),
        arrayListOf(Edge(4,13)),
        arrayListOf(Edge(1, 5), Edge(3, 13), Edge(5, 11)),
        arrayListOf(Edge(2,6), Edge(4, 11), Edge(6,10)),
        arrayListOf(Edge(5,10))
    )
    val start = 0
    val end = 6
    val (dist, path) = dijkstra(graph, start, end)
    if (dist == -1) {
        println("error")
    } else {
        println("$start to $end -> $dist")
        println("Path: ${path.joinToString(" -> ")}")

    }
}
