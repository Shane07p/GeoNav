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
        // we will track the g(n), f(n) and previous optimal node
        Map<String, Double> gScore = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        Map<String, Edge>   usedEdge = new HashMap<>();

        // Priority queue: ordered by f-score
        PriorityQueue<NodeEntry> queue = new PriorityQueue<>();

        for (Node node : graph.getAllNodes()) {
            gScore.put(node.getId(), Double.MAX_VALUE);
        }

        gScore.put(source.getId(), 0.0);
        queue.add(new NodeEntry(source.getId(), heuristic(source, destination)));

        while (!queue.isEmpty()) {
            NodeEntry current = queue.poll();
            String currentId = current.nodeId;

            // stale entry: a shorter path to this node was already processed
            if (current.priority > gScore.get(currentId) + heuristic(graph.getNode(currentId), destination))
                continue;

            if (currentId.equals(destination.getId()))
                break;

            for (Edge edge : graph.getNeighbors(currentId)) {
                double tentativeG = gScore.get(currentId) + edge.getTravelTime();
                String neighborId = edge.getDestination().getId();

                if (tentativeG < gScore.get(neighborId)) {
                    prev.put(neighborId, currentId);
                    usedEdge.put(neighborId, edge);
                    gScore.put(neighborId, tentativeG);
                    double h = heuristic(edge.getDestination(), destination);
                    queue.add(new NodeEntry(neighborId, tentativeG + h));
                }
            }
        }

        return reconstructRoute(graph, source, destination, gScore, prev, usedEdge);
    }

    // ── Haversine Heuristic ──────────────────────────────────

    /**
     * Estimates remaining travel time using the haversine (great-circle) distance
     * divided by the assumed maximum speed.
     * This is an admissible heuristic: it never overestimates the true travel time.
     */
    private double heuristic(Node a, Node b) {
        double distKm = haversineDistance(a.getLatitude(), a.getLongitude(),
                b.getLatitude(), b.getLongitude());
        return distKm / MAX_SPEED_KMH;
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
        String current = destination.getId();
        while (current != null) {
            path.add(graph.getNode(current));
            current = prev.get(current);
        }
        Collections.reverse(path);

        double totalDistance = 0;
        String cur = destination.getId();
        while (prev.containsKey(cur)) {
            totalDistance += usedEdge.get(cur).getDistance();
            cur = prev.get(cur);
        }

        return new Route(path, totalDistance, gScore.get(destination.getId()));
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
