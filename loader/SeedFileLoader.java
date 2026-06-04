package loader;

import engine.NavigationSystem;
import model.Node;
import model.PointOfInterest;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Loads a city map from a plain-text seed file into a NavigationSystem.
 *
 * File format:
 *   # NODES          <- section header (case-insensitive)
 *   ID, Name, Latitude, Longitude
 *
 *   # EDGES
 *   SourceID, DestID, Distance(km), SpeedLimit(km/h)
 *
 *   # POIS
 *   ID, Name, Category, NearestNodeID, Rating, Latitude, Longitude
 *
 * Lines starting with '#' and blank lines are ignored.
 * Sections must appear in order: NODES -> EDGES -> POIS (POIS is optional).
 */
public class SeedFileLoader {

    private enum Section { NONE, NODES, EDGES, POIS }

    public static void load(NavigationSystem system, String filePath) throws IOException, SeedFileException {
        Set<String> nodeIds = new HashSet<>();
        Set<String> poiIds  = new HashSet<>();
        int nodeCount = 0, edgeCount = 0, poiCount = 0;
        Section section = Section.NONE;
        int lineNum = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String raw;
            while ((raw = reader.readLine()) != null) {
                lineNum++;
                String line = raw.trim();
                if (line.isEmpty()) continue;

                // Detect section headers (lines that start with "# NODES" etc.)
                String upper = line.toUpperCase();
                if (upper.startsWith("# NODES")) { section = Section.NODES; continue; }
                if (upper.startsWith("# EDGES")) { section = Section.EDGES; continue; }
                if (upper.startsWith("# POIS"))  { section = Section.POIS;  continue; }
                if (line.startsWith("#"))         continue; // other comment

                if (section == Section.NONE)
                    throw new SeedFileException("Line " + lineNum + ": data found before any section header");

                String[] f = line.split(",");
                for (int i = 0; i < f.length; i++) f[i] = f[i].trim();

                switch (section) {
                    case NODES:
                        parseNode(f, lineNum, nodeIds, system);
                        nodeCount++;
                        break;
                    case EDGES:
                        if (parseEdge(f, lineNum, nodeIds, system)) edgeCount++;
                        break;
                    case POIS:
                        if (parsePOI(f, lineNum, nodeIds, poiIds, system)) poiCount++;
                        break;
                    default:
                        break;
                }
            }
        }

        System.out.printf("  Loaded %d locations, %d roads, %d POIs.%n", nodeCount, edgeCount, poiCount);
    }

    // ── Parsers ─────────────────────────────────────────────────────────────

    private static void parseNode(String[] f, int line, Set<String> nodeIds, NavigationSystem system)
            throws SeedFileException {
        if (f.length != 4)
            throw new SeedFileException("Line " + line + ": node needs 4 fields (ID, Name, Lat, Lon), got " + f.length);
        String id = f[0].toUpperCase();
        if (nodeIds.contains(id))
            throw new SeedFileException("Line " + line + ": duplicate node ID '" + id + "'");
        double lat = parseDouble(f[2], line, "latitude");
        double lon = parseDouble(f[3], line, "longitude");
        if (lat < -90 || lat > 90)   warn(line, "latitude "  + lat + " is outside [-90, 90]");
        if (lon < -180 || lon > 180) warn(line, "longitude " + lon + " is outside [-180, 180]");
        nodeIds.add(id);
        system.addLocation(new Node(id, f[1], lat, lon));
    }

    private static boolean parseEdge(String[] f, int line, Set<String> nodeIds, NavigationSystem system)
            throws SeedFileException {
        if (f.length != 4)
            throw new SeedFileException("Line " + line + ": edge needs 4 fields (Src, Dest, Dist, Speed), got " + f.length);
        String src  = f[0].toUpperCase();
        String dest = f[1].toUpperCase();
        if (!nodeIds.contains(src))
            throw new SeedFileException("Line " + line + ": edge references unknown node '" + src + "'");
        if (!nodeIds.contains(dest))
            throw new SeedFileException("Line " + line + ": edge references unknown node '" + dest + "'");
        double dist  = parseDouble(f[2], line, "distance");
        double speed = parseDouble(f[3], line, "speed limit");
        if (dist <= 0)  { warn(line, "distance "    + dist  + " must be > 0, skipping edge"); return false; }
        if (speed <= 0) { warn(line, "speed limit " + speed + " must be > 0, skipping edge"); return false; }
        system.addRoad(src, dest, dist, speed);
        return true;
    }

    private static boolean parsePOI(String[] f, int line, Set<String> nodeIds, Set<String> poiIds,
                                    NavigationSystem system) throws SeedFileException {
        if (f.length != 7)
            throw new SeedFileException("Line " + line
                    + ": POI needs 7 fields (ID, Name, Category, NearestNodeID, Rating, Lat, Lon), got " + f.length);
        String id          = f[0].toUpperCase();
        String nearestNode = f[3].toUpperCase();
        double rating = parseDouble(f[4], line, "rating");
        double lat    = parseDouble(f[5], line, "latitude");
        double lon    = parseDouble(f[6], line, "longitude");

        if (poiIds.contains(id)) {
            warn(line, "duplicate POI ID '" + id + "', skipping");
            return false;
        }
        if (!nodeIds.contains(nearestNode)) {
            warn(line, "POI nearest node '" + nearestNode + "' not found in graph, skipping");
            return false;
        }
        if (rating < 1.0 || rating > 5.0)
            warn(line, "rating " + rating + " is outside [1.0, 5.0]");

        poiIds.add(id);
        system.addPOI(new PointOfInterest(id, f[1], f[2].toUpperCase(), rating, lat, lon, nearestNode));
        return true;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private static double parseDouble(String s, int line, String field) throws SeedFileException {
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            throw new SeedFileException("Line " + line + ": invalid " + field + " value '" + s + "'");
        }
    }

    private static void warn(int line, String msg) {
        System.out.println("  [WARNING] Line " + line + ": " + msg);
    }

    // ── Exception ────────────────────────────────────────────────────────────

    public static class SeedFileException extends Exception {
        public SeedFileException(String message) {
            super(message);
        }
    }
}
