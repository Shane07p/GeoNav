package model;

import java.util.Collections;
import java.util.List;

/**
 * Represents the result of a routing query — an ordered list of nodes
 * forming a path, along with aggregate distance and time metrics.
 */

public class Route {
    private final List<Node> nodes;
    private final double totalDistance; // km
    private final double totalTime; // hrs

    public Route(List<Node> nodes, double totalDistance, double totalTime) {
        this.nodes = Collections.unmodifiableList(nodes);
        this.totalDistance = totalDistance;
        this.totalTime = totalTime;
    }

    // Getters
    public List<Node> getNodes()        { return nodes; }
    public double getTotalDistance()    { return totalDistance; }
    public double getTotalTime()        { return totalTime; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Path: ");
        for (int i = 0; i < nodes.size(); i++) {
            if (i > 0) sb.append(" -> ");
            sb.append(nodes.get(i).getName());
        }
        sb.append(String.format("%nDistance: %.2f km", totalDistance));
        sb.append(String.format("%nTime: %.1f minutes", totalTime * 60));
        return sb.toString();
    }
}
