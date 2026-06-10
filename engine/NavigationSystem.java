package engine;

import graph.Graph;
import model.Node;
import model.PointOfInterest;
import model.Route;
import spatial.POIQuadTree;
import spatial.QuadTree;
import strategy.AStarStrategy;
import strategy.DijkstraStrategy;
import strategy.RoutingStrategy;

import java.util.*;

/**
 * NavigationSystem
 * 
 * Features:
 * - Single-pair routing (Dijkstra or A*)
 * - Multi-stop routing (waypoints)
 * - Route comparison (both algorithms side-by-side)
 * - POI search by category (K-nearest hotels, restaurants, etc.)
 * - Dynamic map editing (add locations/roads)
 */

public class NavigationSystem {

    private final Graph graph;
    private QuadTree quadTree;
    private RoutingStrategy strategy;

    // Bounding box for QuadTree rebuilds
    private double minLat, maxLat, minLon, maxLon;

    private static final RoutingStrategy DIJKSTRA = new DijkstraStrategy();
    private static final RoutingStrategy A_STAR    = new AStarStrategy();
    private static final double BOUNDS_BUFFER      = 0.01;

    // POI storage: grouped by category
    private final Map<String, List<PointOfInterest>> poiByCategory = new LinkedHashMap<>();
    private final Map<String, POIQuadTree> poiTrees = new HashMap<>();

    public NavigationSystem(Graph graph, QuadTree quadTree, RoutingStrategy strategy, double minLat, double maxLat,
            double minLon, double maxLon) {
        this.graph = graph;
        this.quadTree = quadTree;
        this.strategy = strategy;
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
    }

    // Setters and Getters
    public void setStrategy(RoutingStrategy strategy) {
        this.strategy = strategy;
    }

    public RoutingStrategy getStrategy() {
        return strategy;
    }

    // Single-pair routing
    public Route findRoute(String srcId, String destId) {
        Node src = graph.getNode(srcId);
        Node dest = graph.getNode(destId);
        if (src == null || dest == null)
            return null;
        return strategy.findRoute(graph, src, dest);
    }

    /**
     * Multi-pair routing
     * Find a route through multiple stops in order.
     * Chains individual routes: stop1->stop2, stop2->stop3, etc.
     *
     * @param stopIds ordered list of node IDs (minimum 2)
     * @return combined Route, or null if any leg has no path
     */

    public Route findMultiStopRoute(List<String> stopIds) {
        if (stopIds.size() < 2)
            return null;

        List<Node> combinedPath = new ArrayList<>();
        double totalDist = 0, totalTime = 0;

        for (int i = 0; i < stopIds.size() - 1; i++) {
            Route leg = findRoute(stopIds.get(i), stopIds.get(i + 1));
            if (leg == null)
                return null;

            // If i > 0 this means that our current src was previously dest so we must skip
            // that node
            // the list is unmodifiable so head cannot be removed
            List<Node> legNodes = leg.getNodes();
            if (i > 0 && !combinedPath.isEmpty()) {
                legNodes = legNodes.subList(1, legNodes.size());
            }
            combinedPath.addAll(legNodes);
            totalDist += leg.getTotalDistance();
            totalTime += leg.getTotalTime();
        }

        return new Route(combinedPath, totalDist, totalTime);
    }

    // Strategy Comparison
    public static class ComparisonResult {
        public final Route dijkstraRoute;
        public final Route aStarRoute;
        public final long dijkstraTimeNs;
        public final long aStarTimeNs;

        public ComparisonResult(Route dijkstra, Route aStar, long dTimeNs, long aTimeNs) {
            this.dijkstraRoute = dijkstra;
            this.aStarRoute = aStar;
            this.dijkstraTimeNs = dTimeNs;
            this.aStarTimeNs = aTimeNs;
        }
    }

    public ComparisonResult compareRoutes(String srcId, String destId) {
        Node src = graph.getNode(srcId);
        Node dest = graph.getNode(destId);
        if (src == null || dest == null)
            return null;

        long t0 = System.nanoTime();
        Route dijkstra = DIJKSTRA.findRoute(graph, src, dest);
        long t1 = System.nanoTime();
        Route aStar = A_STAR.findRoute(graph, src, dest);
        long t2 = System.nanoTime();

        return new ComparisonResult(dijkstra, aStar, t1 - t0, t2 - t1);
    }

    // POI Functions
    public void addPOI(PointOfInterest poi) {
        poiByCategory.computeIfAbsent(poi.getCategory(), k -> new ArrayList<>()).add(poi);
        rebuildPOITree(poi.getCategory());
    }

    public List<PointOfInterest> findNearestPOIs(String category, double lat, double lon, int k) {
        POIQuadTree tree = poiTrees.get(category.toUpperCase());
        if (tree == null)
            return Collections.emptyList();
        return tree.findKNearest(lat, lon, k);
    }

    public Set<String> getCategories() {
        return poiByCategory.keySet();
    }

    public List<PointOfInterest> getPOIsByCategory(String category) {
        return poiByCategory.getOrDefault(category.toUpperCase(), Collections.emptyList());
    }

    private void rebuildPOITree(String category) {
        POIQuadTree tree = new POIQuadTree(minLat - BOUNDS_BUFFER, maxLat + BOUNDS_BUFFER, minLon - BOUNDS_BUFFER, maxLon + BOUNDS_BUFFER);
        for (PointOfInterest p : poiByCategory.getOrDefault(category, Collections.emptyList())) {
            tree.insert(p);
        }
        poiTrees.put(category, tree);
    }

    // Dynamic Map Editing
    public void addLocation(Node node) {
        graph.addNode(node);
        minLat = Math.min(minLat, node.getLatitude() - BOUNDS_BUFFER);
        maxLat = Math.max(maxLat, node.getLatitude() + BOUNDS_BUFFER);
        minLon = Math.min(minLon, node.getLongitude() - BOUNDS_BUFFER);
        maxLon = Math.max(maxLon, node.getLongitude() + BOUNDS_BUFFER);
        rebuildQuadTree();
    }

    /**
     * Removes a location and all roads connected to it.
     * @return true if the location existed and was removed
     */
    public boolean removeLocation(String id) {
        boolean removed = graph.removeNode(id);
        if (removed) {
            rebuildQuadTree();
        }
        return removed;
    }

    public void addRoad(String srcId, String destId, double distance, double speedLimit) {
        Node src = graph.getNode(srcId);
        Node dest = graph.getNode(destId);
        if (src != null && dest != null) {
            graph.addEdge(src, dest, distance, speedLimit);
        }
    }

    private void rebuildQuadTree() {
        quadTree = new QuadTree(minLat, maxLat, minLon, maxLon);
        for (Node n : graph.getAllNodes())
            quadTree.insert(n);
    }

    public Node findNearestNode(double latitude, double longitude) {
        return quadTree.findNearest(latitude, longitude);
    }

    public Graph getGraph() {
        return graph;
    }
}
