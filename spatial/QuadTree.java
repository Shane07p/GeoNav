package spatial;

import model.Node;

import java.util.ArrayList;
import java.util.List;

/**
 * QuadTree — a spatial data structure for efficient 2D point queries.
 *
 * Recursively subdivides a 2D region into four quadrants (NW, NE, SW, SE).
 * Used to efficiently answer "find nearest location" queries without scanning
 * every node in the graph.
 *
 * Time Complexity:
 * - Insert: O(log n) average
 * - Nearest: O(log n) average (instead of O(n) brute force)
 */

public class QuadTree {

    private static final int CAPACITY = 4; // max points per leaf before split
    private final double minLat, maxLat, minLon, maxLon;

    private final List<Node> points;
    private boolean divided;

    private QuadTree northwest;
    private QuadTree northeast;
    private QuadTree southwest;
    private QuadTree southeast;

    public QuadTree(double minLat, double maxLat, double minLon, double maxLon) {
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
        this.points = new ArrayList<>();
        this.divided = false;
    }

    public boolean insert(Node node) {
        if (!contains(node.getLatitude(), node.getLongitude())) {
            return false;
        }

        // If there's room in this leaf, add it
        if (points.size() < CAPACITY && !divided) {
            points.add(node);
            return true;
        }

        // Otherwise, subdivide and delegate
        if (!divided) {
            subdivide();
        }

        if (northwest.insert(node))
            return true;
        if (northeast.insert(node))
            return true;
        if (southwest.insert(node))
            return true;
        if (southeast.insert(node))
            return true;

        return false; // should not happen
    }

    // Nearest Neighbor Query

    /**
     * Find the nearest node to the given coordinates.
     * Uses branch-and-bound pruning to avoid scanning unnecessary quadrants.
     *
     * @param lat query latitude
     * @param lon query longitude
     * @return the nearest Node, or null if the tree is empty
     */
    
    public Node findNearest(double lat, double lon) {
        NearestResult result = new NearestResult();
        result.bestDist = Double.MAX_VALUE;
        result.bestNode = null;
        findNearestHelper(lat, lon, result);
        return result.bestNode;
    }

    private void findNearestHelper(double lat, double lon, NearestResult result) {
        if (distToQuad(lat, lon) >= result.bestDist) {
            return;
        }

        for (Node p : points) {
            double d = euclideanDist(lat, lon, p.getLatitude(), p.getLongitude());
            if (d < result.bestDist) {
                result.bestDist = d;
                result.bestNode = p;
            }
        }

        if (divided) {
            northwest.findNearestHelper(lat, lon, result);
            northeast.findNearestHelper(lat, lon, result);
            southwest.findNearestHelper(lat, lon, result);
            southeast.findNearestHelper(lat, lon, result);
        }
    }

    private boolean contains(double lat, double lon) {
        return lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon;
    }

    private void subdivide() {
        double midLat = (minLat + maxLat) / 2;
        double midLon = (minLon + maxLon) / 2;

        northwest = new QuadTree(midLat, maxLat, minLon, midLon);
        northeast = new QuadTree(midLat, maxLat, midLon, maxLon);
        southwest = new QuadTree(minLat, midLat, minLon, midLon);
        southeast = new QuadTree(minLat, midLat, midLon, maxLon);

        for (Node p : points) {
            northwest.insert(p);
            northeast.insert(p);
            southwest.insert(p);
            southeast.insert(p);
        }
        points.clear();
        divided = true;
    }

    private double distToQuad(double lat, double lon) {
        double closestLat = Math.max(minLat, Math.min(lat, maxLat));
        double closestLon = Math.max(minLon, Math.min(lon, maxLon));
        return euclideanDist(lat, lon, closestLat, closestLon);
    }

    private double euclideanDist(double lat1, double lon1, double lat2, double lon2) {
        double dLat = lat1 - lat2;
        double dLon = lon1 - lon2;
        return Math.sqrt(dLat * dLat + dLon * dLon);
    }

    private static class NearestResult {
        Node bestNode;
        double bestDist;
    }
}
