package graph;

import model.Edge;
import model.Node;

import java.util.*;

/**
 * Adjacency-list based weighted graph.
 * Supports bidirectional edges (roads you can travel both ways).
 */

public class Graph {
    private final Map<String, Node> nodes; // id → Node
    private final Map<String, List<Edge>> adjacency; // nodeId → outgoing edges
    private int edgeCount;

    public Graph() {
        this.nodes = new LinkedHashMap<>();
        this.adjacency = new LinkedHashMap<>();
        this.edgeCount = 0;
    }

    // Mutators

    public void addNode(Node node) {
        nodes.put(node.getId(), node);
        adjacency.putIfAbsent(node.getId(), new ArrayList<>());
    }

    public void addEdge(Node src, Node dest, double distance, double speedLimit) {
        Edge forward = new Edge(src, dest, distance, speedLimit);
        Edge reverse = new Edge(dest, src, distance, speedLimit);

        adjacency.get(src.getId()).add(forward);
        adjacency.get(dest.getId()).add(reverse);
        edgeCount++;
    }

    public boolean removeNode(String id) {
        if (nodes.remove(id) == null) {
            return false;
        }
        List<Edge> removed = adjacency.remove(id);
        if (removed != null) edgeCount -= removed.size();

        // Remove edges in other nodes' lists that point to the deleted node
        for (List<Edge> edges : adjacency.values()) {
            edges.removeIf(e -> e.getDestination().getId().equals(id));
        }
        return true;
    }

    // Getters
    public List<Edge> getNeighbors(String id) { return adjacency.getOrDefault(id, Collections.emptyList()); }
    public Node getNode(String id) { return nodes.get(id); }
    public Collection<Node> getAllNodes() { return nodes.values(); }
    public int getNodeCount() { return nodes.size(); }
    public int getEdgeCount() { return edgeCount; }
}
