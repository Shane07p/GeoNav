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

    // Display

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("╔══════════════════════════════════════════════╗\n");
        sb.append("║            ROUTE FOUND                      ║\n");
        sb.append("╠══════════════════════════════════════════════╣\n");

        sb.append("║  Path: ");
        for (int i = 0; i < nodes.size(); i++) {
            if (i > 0)
                sb.append(" → ");
            sb.append(nodes.get(i).getName());
        }
        sb.append("\n");

        sb.append(String.format("║  Total Distance : %.2f km\n", totalDistance));
        sb.append(String.format("║  Estimated Time : %.1f minutes\n", totalTime * 60));
        sb.append("╚══════════════════════════════════════════════╝");

        return sb.toString();
    }
}
