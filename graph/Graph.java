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

    public void addEdge(Node source, Node destination, double distance, double speedLimit) {
        // birdirectional Edge
        Edge forward = new Edge(source, destination, distance, speedLimit);
        Edge reverse = new Edge(destination, source, distance, speedLimit);

        adjacency.get(source.getId()).add(forward);
        adjacency.get(destination.getId()).add(reverse);
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

    public List<Edge> getNeighbors(String nodeId) {
        return adjacency.getOrDefault(nodeId, Collections.emptyList()); // return empty list to not get
                                                                        // NullPointerException
    }

    public Node getNode(String id) {
        return nodes.get(id);
    }

    public Collection<Node> getAllNodes() {
        return nodes.values();
    }

    public int getNodeCount() {
        return nodes.size();
    }

    public int getEdgeCount() {
        return edgeCount;
    }
}
