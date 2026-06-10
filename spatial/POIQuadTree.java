package spatial;

import model.PointOfInterest;

import java.util.*;

/**
 * QuadTree for Point of Interest objects.
 * Supports K-nearest-neighbor queries using a max-heap with branch-and-bound pruning.
 */

public class POIQuadTree {

    private static final int CAPACITY = 4;
    private final double minLat, maxLat, minLon, maxLon;

    private final List<PointOfInterest> points;
    private boolean divided;

    private POIQuadTree northwest, northeast, southwest, southeast;

    public POIQuadTree(double minLat, double maxLat, double minLon, double maxLon) {
        this.minLat = minLat;
        this.maxLat = maxLat;
        this.minLon = minLon;
        this.maxLon = maxLon;
        this.points = new ArrayList<>();
        this.divided = false;
    }

    public boolean insert(PointOfInterest poi) {
        if (!contains(poi.getLatitude(), poi.getLongitude())) {
            return false;
        }

        if (points.size() < CAPACITY && !divided) {
            points.add(poi);
            return true;
        }

        if (!divided) {
            subdivide();
        }

        if (northwest.insert(poi)) return true;
        if (northeast.insert(poi)) return true;
        if (southwest.insert(poi)) return true;
        if (southeast.insert(poi)) return true;

        return false;
    }

    // K-Nearest Neighbor Search

    /**
     * Find the K nearest POIs to a given lat/lon. We will use a maxHeap
     * Branch-and-bound pruning skips quadrants that can't contain closer points.
     */

    public List<PointOfInterest> findKNearest(double lat, double lon, int k) {
        PriorityQueue<Map.Entry<Double, PointOfInterest>> maxHeap = new PriorityQueue<>(
            (a, b) -> Double.compare(b.getKey(), a.getKey())
        );
        findKNearestHelper(lat, lon, k, maxHeap);
        List<PointOfInterest> result = new ArrayList<>();
        while (!maxHeap.isEmpty()) result.add(maxHeap.poll().getValue());
        result.sort((a, b) -> Double.compare(
            euclideanDist(lat, lon, a.getLatitude(), a.getLongitude()),
            euclideanDist(lat, lon, b.getLatitude(), b.getLongitude())
        ));
        return result;
    }

    private void findKNearestHelper(double lat, double lon, int k,
            PriorityQueue<Map.Entry<Double, PointOfInterest>> maxHeap) {
        if (maxHeap.size() >= k && distToQuad(lat, lon) >= maxHeap.peek().getKey()) return;
        for (PointOfInterest p : points) {
            double d = euclideanDist(lat, lon, p.getLatitude(), p.getLongitude());
            if (maxHeap.size() < k) {
                maxHeap.add(Map.entry(d, p));
            } else if (d < maxHeap.peek().getKey()) {
                maxHeap.poll();
                maxHeap.add(Map.entry(d, p));
            }
        }
        if (divided) {
            northwest.findKNearestHelper(lat, lon, k, maxHeap);
            northeast.findKNearestHelper(lat, lon, k, maxHeap);
            southwest.findKNearestHelper(lat, lon, k, maxHeap);
            southeast.findKNearestHelper(lat, lon, k, maxHeap);
        }
    }
    
    private boolean contains(double lat, double lon) {
        return lat >= minLat && lat <= maxLat && lon >= minLon && lon <= maxLon;
    }

    private void subdivide() {
        double midLat = (minLat + maxLat) / 2;
        double midLon = (minLon + maxLon) / 2;

        northwest = new POIQuadTree(midLat, maxLat, minLon, midLon);
        northeast = new POIQuadTree(midLat, maxLat, midLon, maxLon);
        southwest = new POIQuadTree(minLat, midLat, minLon, midLon);
        southeast = new POIQuadTree(minLat, midLat, midLon, maxLon);

        for (PointOfInterest p : points) {
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
}
