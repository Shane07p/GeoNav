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
 * Uses a min-priority-pq keyed on f(n) = g(n) + h(n), where:
 * - g(n) = actual travel time from src to n
 * - h(n) = heuristic estimate of remaining time (haversine distance / max speed)
 * The haversine heuristic is admissible (never overestimates), guaranteeing optimality.
 */

public class AStarStrategy implements RoutingStrategy {

    @Override
    public String getStrategyName() {
        return "A* Search (Fastest Time)";
    }

    @Override
    public Route findRoute(Graph graph, Node src, Node dest) {
        Map<String, Double> gScore = new HashMap<>();
        Map<String, Edge>   usedEdge = new HashMap<>();
        PriorityQueue<DijkstraStrategy.NodeEntry> pq = new PriorityQueue<>();

        // The heuristic divides by the fastest road speed so it never
        // overestimates. Deriving it from the actual graph (rather than a
        // hardcoded constant) keeps it admissible for any seed — a hardcoded
        // cap below a real road speed would break optimality.
        double maxSpeed = maxEdgeSpeed(graph);

        for (Node node : graph.getAllNodes()) {
            gScore.put(node.getId(), Double.MAX_VALUE);
        }

        gScore.put(src.getId(), 0.0);
        pq.add(new DijkstraStrategy.NodeEntry(src.getId(), heuristic(src, dest, maxSpeed)));

        while (!pq.isEmpty()) {
            DijkstraStrategy.NodeEntry e = pq.poll();
            String u = e.nodeId;

            // stale entry: a shorter path to this node was already processed
            if (e.priority > gScore.get(u) + heuristic(graph.getNode(u), dest, maxSpeed))
                continue;

            if (u.equals(dest.getId()))
                break;

            for (Edge edge : graph.getNeighbors(u)) {
                String v  = edge.getDestination().getId();
                double ng = gScore.get(u) + edge.getTravelTime();
                if (ng < gScore.get(v)) {
                    usedEdge.put(v, edge);
                    gScore.put(v, ng);
                    pq.add(new DijkstraStrategy.NodeEntry(v, ng + heuristic(edge.getDestination(), dest, maxSpeed)));
                }
            }
        }

        return reconstructRoute(graph, src, dest, gScore, usedEdge);
    }

    private double heuristic(Node a, Node b, double maxSpeed) {
        double d = haversineDistance(a.getLatitude(), a.getLongitude(),
                b.getLatitude(), b.getLongitude());
        return d / maxSpeed;
    }

    /**
     * Fastest road speed in the graph. Used as the heuristic's speed divisor
     * so that estimated time never exceeds true time (admissibility). Falls
     * back to 1.0 for an edgeless graph to avoid division by zero — such a
     * graph has no routes anyway.
     */
    private double maxEdgeSpeed(Graph graph) {
        double max = 0.0;
        for (Node n : graph.getAllNodes()) {
            for (Edge e : graph.getNeighbors(n.getId())) {
                if (e.getSpeedLimit() > max)
                    max = e.getSpeedLimit();
            }
        }
        return max > 0 ? max : 1.0;
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

    private Route reconstructRoute(Graph graph, Node src, Node dest,
            Map<String, Double> gScore, Map<String, Edge> usedEdge) {
        if (gScore.get(dest.getId()) == Double.MAX_VALUE) {
            return null;
        }

        // Walk back from dest via the edge used to reach each node; that edge's
        // source is the predecessor, so no separate prev map is needed.
        List<Node> path = new ArrayList<>();
        double totalDist = 0;
        String u = dest.getId();
        while (usedEdge.containsKey(u)) {
            Edge e = usedEdge.get(u);
            path.add(graph.getNode(u));
            totalDist += e.getDistance();
            u = e.getSource().getId();
        }
        path.add(src);
        Collections.reverse(path);

        return new Route(path, totalDist, gScore.get(dest.getId()));
    }

}
