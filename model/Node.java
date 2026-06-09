package model;

import java.util.Objects;

/**
 * Represents a geographic location / intersection on the map.
 * Each node has a unique ID, name, and lat/lon coordinates.
 */

public class Node {
    private final String id;
    private final String name;
    private final double latitude;
    private final double longitude;

    public Node(String id, String name, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Node node = (Node) o;
        return Objects.equals(id, node.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }

    @Override
    public String toString() {
        return String.format("%-12s (%s) [%.4f, %.4f]", name, id, latitude, longitude);
    }
}
