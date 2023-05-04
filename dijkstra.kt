import java.util.*

// now : 현재 노드, near : 인접 노드, weight : 거리
class Edge(val now: Int, val near: Int, val weight: Int)

// graph : 전체 노드(각 노드 별로 인접 노드와 거리 가짐)
// start : 시작 노드
// end : 도착 노드
fun dijkstra(graph: Array<ArrayList<Edge>>, start: Int, end: Int): Pair<Int, List<Int>> {

    val graph2 = Array(1000) { arrayListOf<Edge>() }
    for (i in graph.indices){
        graph2[graph[i][0].now] = graph[i]
    }

    // dist : 최단 거리 담을 배열. 초기엔 max_value 초기화
    val dist = IntArray(1000) { Int.MAX_VALUE }
    dist[start] = 0 // 시작 노드는 거리를 0으로.

    // 가장 짧은 노드 선택하기 위해 우선순위 큐
    val pq = PriorityQueue<Pair<Int, Int>>(compareBy { it.second })
    pq.add(start to 0) // 출발노드 
    
    // prev : 최단 경로상에서 현재 노드의 직전 노드 기록
    val path = IntArray(1000) { -1 }

    while (pq.isNotEmpty()) {
        // poll : 우선순위 큐에서 헤드(= 가장 짧은 거리의 노드) 추출
        val (cur, curDist) = pq.poll()

        // 도착 노드에 도달하면 현 최단거리 리턴하고 종료
        if (cur == end) {
            val path2 = ArrayList<Int>()

            path2.add(end)
            while(true){
                path2.add(path[path2.last()])
                if(path2.last() == start)
                    break
            }
            path2.reverse()
            return curDist to path2
        }

        // 현재 노드를 기준으로 인접노드들 불러옴.
        for (edge in graph2[cur]) {
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
        arrayListOf(Edge(400, 402, 10)),
        arrayListOf(Edge(401, 403,13)),
        arrayListOf(Edge(402, 400,10), Edge(402,403,5), Edge(402,404,11)),
        arrayListOf(Edge(403,401,13),Edge(403,402,5), Edge(403,405,11)),
        arrayListOf(Edge(404,402,11),Edge(404,405,6)),
        arrayListOf(Edge(405,403,11),Edge(405,404,6),Edge(405,406,10)),
        arrayListOf(Edge(406,405,10))
    )
    val start = 400
    val end = 406
    val (dist, path) = dijkstra(graph, start, end)
    if (dist == -1) {
        println("error")
    } else {
        println("$start to $end -> $dist")
        println("Path: ${path.joinToString(" -> ")}")

    }
}
