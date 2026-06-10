package model;

/**
 * Represents a road/path between two Nodes on the map.
 * Stores both distance and speed limit so we can optimize for
 * either shortest distance OR fastest time.
 */

public class Edge {
    private final Node source;
    private final Node destination;
    private final double distance; // in km
    private final double speedLimit; // in km/h

    public Edge(Node source, Node destination, double distance, double speedLimit) {
        this.source = source;
        this.destination = destination;
        this.distance = distance;
        this.speedLimit = speedLimit;
    }

    // Getters
    public Node getSource()        { return source; }
    public Node getDestination()   { return destination; }
    public double getDistance()    { return distance; }
    public double getSpeedLimit()  { return speedLimit; }
    public double getTravelTime()  { return distance / speedLimit; }

    @Override
    public String toString() {
        return String.format("%s -> %s [%.2f km, %.0f km/h, %.2f min]",
                source.getName(), destination.getName(),
                distance, speedLimit, getTravelTime() * 60);
    }
}
