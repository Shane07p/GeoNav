package strategy;

import graph.Graph;
import model.Edge;
import model.Node;
import model.Route;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

/**
 * Dijkstra's Algorithm — finds the SHORTEST DISTANCE path.
 *
 * Uses a min-priority-pq keyed on cumulative distance.
 * Guarantees the optimal shortest-distance path in a non-negative weighted
 * graph.
 */

public class DijkstraStrategy implements RoutingStrategy {

    @Override
    public String getStrategyName() {
        return "Dijkstra's Algorithm (Shortest Distance)";
    }

    @Override
    public Route findRoute(Graph graph, Node src, Node dest) {

        Map<String, Double> dist = new HashMap<>();
        Map<String, Edge>   usedEdge = new HashMap<>();
        PriorityQueue<NodeEntry> pq = new PriorityQueue<>();

        for (Node node : graph.getAllNodes()) {
            dist.put(node.getId(), Double.MAX_VALUE);
        }
        dist.put(src.getId(), 0.0);
        pq.add(new NodeEntry(src.getId(), 0.0));

        while (!pq.isEmpty()) {
            NodeEntry e = pq.poll();
            String u = e.nodeId;

            if (e.priority > dist.get(u))
                continue;

            if (u.equals(dest.getId()))
                break;

            for (Edge edge : graph.getNeighbors(u)) {
                String v  = edge.getDestination().getId();
                double nd = dist.get(u) + edge.getDistance();
                if (nd < dist.get(v)) {
                    dist.put(v, nd);
                    usedEdge.put(v, edge);
                    pq.add(new NodeEntry(v, nd));
                }
            }
        }

        return reconstructRoute(graph, src, dest, dist, usedEdge);
    }

    protected Route reconstructRoute(Graph graph, Node src, Node dest,
            Map<String, Double> dist, Map<String, Edge> usedEdge) {
        if (dist.get(dest.getId()) == Double.MAX_VALUE) {
            return null;
        }

        // Walk back from dest via the edge used to reach each node; that edge's
        // source is the predecessor, so no separate prev map is needed.
        List<Node> path = new ArrayList<>();
        double totalTime = 0;
        String u = dest.getId();
        while (usedEdge.containsKey(u)) {
            Edge e = usedEdge.get(u);
            path.add(graph.getNode(u));
            totalTime += e.getTravelTime();
            u = e.getSource().getId();
        }
        path.add(src);
        Collections.reverse(path);

        return new Route(path, dist.get(dest.getId()), totalTime);
    }

    // NodeEntry class is independent of its outer class so we have made it static
    // it implements comparable for compareTo function in priority pq
    protected static class NodeEntry implements Comparable<NodeEntry> {
        String nodeId;
        double priority;

        NodeEntry(String nodeId, double priority) {
            this.nodeId = nodeId;
            this.priority = priority;
        }

        @Override
        public int compareTo(NodeEntry other) {
            return Double.compare(this.priority, other.priority);
        }
    }
}
