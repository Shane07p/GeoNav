package model;

import java.util.Objects;

/**
 * Represents a Point of Interest (hotel, restaurant, mall, theatre, etc.)
 * Each POI has a category, rating, GPS position, and is linked to the nearest map node.
 */

public class PointOfInterest {
    private final String id;
    private final String name;
    private final String category;     // HOTEL, RESTAURANT, MALL, THEATRE
    private final double rating;       // 1.0 - 5.0
    private final double latitude;
    private final double longitude;

    public PointOfInterest(String id, String name, String category, double rating, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.category = category.toUpperCase();
        this.rating = rating;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters
    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getRating() { return rating; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PointOfInterest poi = (PointOfInterest) o;
        return Objects.equals(id, poi.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("%s (%s) [%s] %.1f stars", name, id, category, rating);
    }
}
