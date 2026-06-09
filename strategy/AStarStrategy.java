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
 * A* Search Algorithm — finds the FASTEST TIME path.
 *
 * Uses a min-priority-queue keyed on f(n) = g(n) + h(n), where:
 * - g(n) = actual travel time from source to n
 * - h(n) = heuristic estimate of remaining time (haversine distance / max speed)
 * The haversine heuristic is admissible (never overestimates), guaranteeing optimality.
 */

public class AStarStrategy implements RoutingStrategy {

    // Assumed maximum speed for heuristic (km/h)
    private static final double MAX_SPEED_KMH = 60.0;

    @Override
    public String getStrategyName() {
        return "A* Search (Fastest Time)";
    }

    @Override
    public Route findRoute(Graph graph, Node source, Node destination) {
        Map<String, Double> gScore = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        Map<String, Edge>   usedEdge = new HashMap<>();
        PriorityQueue<NodeEntry> queue = new PriorityQueue<>();

        for (Node node : graph.getAllNodes()) {
            gScore.put(node.getId(), Double.MAX_VALUE);
        }

        gScore.put(source.getId(), 0.0);
        queue.add(new NodeEntry(source.getId(), heuristic(source, destination)));

        while (!queue.isEmpty()) {
            NodeEntry e = queue.poll();
            String u = e.nodeId;

            // stale entry: a shorter path to this node was already processed
            if (e.priority > gScore.get(u) + heuristic(graph.getNode(u), destination))
                continue;

            if (u.equals(destination.getId()))
                break;

            for (Edge edge : graph.getNeighbors(u)) {
                String v  = edge.getDestination().getId();
                double ng = gScore.get(u) + edge.getTravelTime();
                if (ng < gScore.get(v)) {
                    prev.put(v, u);
                    usedEdge.put(v, edge);
                    gScore.put(v, ng);
                    queue.add(new NodeEntry(v, ng + heuristic(edge.getDestination(), destination)));
                }
            }
        }

        return reconstructRoute(graph, source, destination, gScore, prev, usedEdge);
    }

    private double heuristic(Node a, Node b) {
        double d = haversineDistance(a.getLatitude(), a.getLongitude(),
                b.getLatitude(), b.getLongitude());
        return d / MAX_SPEED_KMH;
    }

    /**
     * Haversine formula — calculates the great-circle distance between two
     * geographic points given their latitude and longitude in degrees.
     *
     * @return dist in km
     */
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius in km

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private Route reconstructRoute(Graph graph, Node source, Node destination,
            Map<String, Double> gScore, Map<String, String> prev, Map<String, Edge> usedEdge) {
        if (gScore.get(destination.getId()) == Double.MAX_VALUE) {
            return null;
        }

        List<Node> path = new ArrayList<>();
        String u = destination.getId();
        while (u != null) {
            path.add(graph.getNode(u));
            u = prev.get(u);
        }
        Collections.reverse(path);

        double totalDist = 0;
        u = destination.getId();
        while (prev.containsKey(u)) {
            totalDist += usedEdge.get(u).getDistance();
            u = prev.get(u);
        }

        return new Route(path, totalDist, gScore.get(destination.getId()));
    }

    // priority queue will keep the top on the basis of a nodes priority
    private static class NodeEntry implements Comparable<NodeEntry> {
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
