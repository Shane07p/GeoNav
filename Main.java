import engine.NavigationSystem;
import graph.Graph;
import loader.SeedFileLoader;
import model.Node;
import model.PointOfInterest;
import model.Route;
import spatial.QuadTree;
import strategy.AStarStrategy;
import strategy.DijkstraStrategy;
import strategy.RoutingStrategy;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

public class Main {

    private static final RoutingStrategy DIJKSTRA = new DijkstraStrategy();
    private static final RoutingStrategy A_STAR   = new AStarStrategy();

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        Graph graph = new Graph();
        double[] bounds = { 12.84, 13.21, 77.56, 77.80 };
        QuadTree quadTree = new QuadTree(bounds[0], bounds[1], bounds[2], bounds[3]);
        NavigationSystem navSystem = new NavigationSystem(graph, quadTree, DIJKSTRA,
                bounds[0], bounds[1], bounds[2], bounds[3]);

        System.out.print("\nEnter seed file path (press Enter for default city): ");
        String path = sc.nextLine().trim();
        if (path.isEmpty()) path = "seeds/default_city.txt";

        try {
            SeedFileLoader.load(navSystem, path);
        } catch (SeedFileLoader.SeedFileException e) {
            System.out.println("[ERROR] " + e.getMessage());
            if (!loadFallback(navSystem, sc)) { sc.close(); return; }
        } catch (FileNotFoundException e) {
            System.out.println("[ERROR] File not found: " + path);
            if (!loadFallback(navSystem, sc)) { sc.close(); return; }
        } catch (IOException e) {
            System.out.println("[ERROR] Could not read file: " + e.getMessage());
            if (!loadFallback(navSystem, sc)) { sc.close(); return; }
        }

        System.out.println("\nGeoNav — Geospatial Routing & Navigation Engine\n");

        boolean running = true;
        while (running) {
            printMenu(navSystem);
            System.out.print("> ");
            String choice = sc.nextLine().trim();
            System.out.println();

            switch (choice) {
                case "1":  showAllLocations(navSystem);      break;
                case "2":  findRoute(navSystem, sc);         break;
                case "3":  switchStrategy(navSystem);        break;
                case "4":  findNearest(navSystem, sc);       break;
                case "5":  addLocation(navSystem, sc);       break;
                case "6":  addRoad(navSystem, sc);           break;
                case "7":  showMapStats(navSystem);          break;
                case "8":  deleteLocation(navSystem, sc);    break;
                case "9":  multiStopRoute(navSystem, sc);    break;
                case "10": compareRoutes(navSystem, sc);     break;
                case "11": findNearbyPlaces(navSystem, sc);  break;
                case "12": addPlace(navSystem, sc);          break;
                case "0":
                    running = false;
                    System.out.println("Goodbye!\n");
                    break;
                default:
                    System.out.println("Invalid choice. Enter 0-12.\n");
            }
        }

        sc.close();
    }

    private static boolean loadFallback(NavigationSystem navSystem, Scanner sc) {
        System.out.print("Load default city instead? (y/n): ");
        if (!sc.nextLine().trim().equalsIgnoreCase("y")) return false;
        try {
            SeedFileLoader.load(navSystem, "seeds/default_city.txt");
            return true;
        } catch (Exception e) {
            System.out.println("[ERROR] Could not load default city: " + e.getMessage());
            return false;
        }
    }

    // ── Menu Actions ──────────────────────────────────────────────────────────

    private static void showAllLocations(NavigationSystem nav) {
        System.out.println("LOCATIONS");
        System.out.println("------------------------------------------");
        System.out.printf("%-6s  %-16s  %10s  %10s\n", "ID", "Name", "Latitude", "Longitude");
        System.out.println("------------------------------------------");
        for (Node node : nav.getGraph().getAllNodes()) {
            System.out.printf("%-6s  %-16s  %10.4f  %10.4f\n",
                    node.getId(), node.getName(), node.getLatitude(), node.getLongitude());
        }
        System.out.println("------------------------------------------");
        System.out.printf("Total: %d locations\n\n", nav.getGraph().getNodeCount());
    }

    private static void findRoute(NavigationSystem nav, Scanner sc) {
        System.out.println("FIND ROUTE  [" + nav.getStrategy().getStrategyName() + "]");
        System.out.print("Source ID      : ");
        String srcId = sc.nextLine().trim().toUpperCase();
        System.out.print("Destination ID : ");
        String destId = sc.nextLine().trim().toUpperCase();

        if (srcId.equals(destId)) {
            System.out.println("You're already there!\n");
            return;
        }

        long t0 = System.nanoTime();
        Route route = nav.findRoute(srcId, destId);
        long elapsed = System.nanoTime() - t0;

        if (route == null) {
            System.out.println("No route found. Check that IDs are correct.\n");
        } else {
            printRoute(route, elapsed);
        }
    }

    private static void switchStrategy(NavigationSystem nav) {
        if (nav.getStrategy() instanceof DijkstraStrategy) {
            nav.setStrategy(A_STAR);
        } else {
            nav.setStrategy(DIJKSTRA);
        }
        System.out.println("Switched to: " + nav.getStrategy().getStrategyName() + "\n");
    }

    private static void findNearest(NavigationSystem nav, Scanner sc) {
        System.out.println("FIND NEAREST LOCATION");
        double lat = readDouble(sc, "Latitude  : ");
        if (Double.isNaN(lat)) return;
        double lon = readDouble(sc, "Longitude : ");
        if (Double.isNaN(lon)) return;

        Node nearest = nav.findNearestNode(lat, lon);
        if (nearest != null) {
            System.out.println("Nearest: " + nearest.getName() + " (" + nearest.getId() + ")"
                    + " [" + nearest.getLatitude() + ", " + nearest.getLongitude() + "]\n");
        } else {
            System.out.println("No locations found.\n");
        }
    }

    private static void addLocation(NavigationSystem nav, Scanner sc) {
        System.out.println("ADD LOCATION");
        System.out.print("Location ID   : ");
        String id = sc.nextLine().trim().toUpperCase();
        if (nav.getGraph().getNode(id) != null) {
            System.out.println("ID '" + id + "' already exists.\n");
            return;
        }
        System.out.print("Location Name : ");
        String name = sc.nextLine().trim();
        double lat = readDouble(sc, "Latitude      : ");
        if (Double.isNaN(lat)) return;
        double lon = readDouble(sc, "Longitude     : ");
        if (Double.isNaN(lon)) return;

        nav.addLocation(new Node(id, name, lat, lon));
        System.out.println("Added: " + name + " (" + id + ")\n");
    }

    private static void deleteLocation(NavigationSystem nav, Scanner sc) {
        System.out.println("DELETE LOCATION  (removes all connected roads)");
        System.out.print("Location ID : ");
        String id = sc.nextLine().trim().toUpperCase();

        if (nav.getGraph().getNode(id) == null) {
            System.out.println("No location with ID '" + id + "' found.\n");
            return;
        }

        String name = nav.getGraph().getNode(id).getName();
        if (nav.removeLocation(id)) {
            System.out.println("Deleted: " + name + " (" + id + ") and all connected roads.\n");
        } else {
            System.out.println("Failed to delete location.\n");
        }
    }

    private static void addRoad(NavigationSystem nav, Scanner sc) {
        System.out.println("ADD ROAD  (bidirectional)");
        System.out.print("From ID           : ");
        String srcId = sc.nextLine().trim().toUpperCase();
        System.out.print("To ID             : ");
        String destId = sc.nextLine().trim().toUpperCase();

        if (nav.getGraph().getNode(srcId) == null || nav.getGraph().getNode(destId) == null) {
            System.out.println("Invalid ID(s). Add locations first.\n");
            return;
        }

        double dist  = readDouble(sc, "Distance (km)     : ");
        if (Double.isNaN(dist)) return;
        double speed = readDouble(sc, "Speed limit (km/h): ");
        if (Double.isNaN(speed)) return;

        nav.addRoad(srcId, destId, dist, speed);
        System.out.println("Road added: " + srcId + " <-> " + destId
                + " (" + dist + " km, " + speed + " km/h)\n");
    }

    private static void showMapStats(NavigationSystem nav) {
        System.out.println("MAP STATISTICS");
        System.out.println("------------------------------------------");
        System.out.println("Locations : " + nav.getGraph().getNodeCount());
        System.out.println("Roads     : " + nav.getGraph().getEdgeCount());
        System.out.println("Strategy  : " + nav.getStrategy().getStrategyName());
        System.out.println("------------------------------------------\n");
    }

    private static void multiStopRoute(NavigationSystem nav, Scanner sc) {
        System.out.println("MULTI-STOP ROUTE");
        System.out.println("Enter stop IDs separated by commas (min 2), e.g. CTR,MKT,LKS");
        System.out.print("Stops: ");
        String input = sc.nextLine().trim().toUpperCase();
        String[] parts = input.split("\\s*,\\s*");

        if (parts.length < 2) {
            System.out.println("Need at least 2 stops.\n");
            return;
        }

        List<String> stops = new ArrayList<>();
        for (String p : parts) if (!p.isEmpty()) stops.add(p);

        long t0 = System.nanoTime();
        Route route = nav.findMultiStopRoute(stops);
        long elapsed = System.nanoTime() - t0;

        if (route == null) {
            System.out.println("No route found. Check IDs and connectivity.\n");
        } else {
            System.out.println("Stops: " + String.join(" -> ", stops));
            printRoute(route, elapsed);
        }
    }

    private static void compareRoutes(NavigationSystem nav, Scanner sc) {
        System.out.println("COMPARE ROUTES — Dijkstra vs A*");
        System.out.print("Source ID      : ");
        String srcId = sc.nextLine().trim().toUpperCase();
        System.out.print("Destination ID : ");
        String destId = sc.nextLine().trim().toUpperCase();

        NavigationSystem.ComparisonResult cmp = nav.compareRoutes(srcId, destId);
        if (cmp == null) {
            System.out.println("Invalid IDs.\n");
            return;
        }

        System.out.println("------------------------------------------");
        System.out.println("Dijkstra (shortest distance):");
        if (cmp.dijkstraRoute != null) {
            System.out.println("  Path     : " + formatPath(cmp.dijkstraRoute));
            System.out.printf("  Distance : %.2f km%n", cmp.dijkstraRoute.getTotalDistance());
            System.out.printf("  Time     : %.1f min%n", cmp.dijkstraRoute.getTotalTime() * 60);
            System.out.printf("  Computed : %.3f ms%n", cmp.dijkstraTimeNs / 1_000_000.0);
        } else {
            System.out.println("  No route found.");
        }
        System.out.println("A* (fastest time):");
        if (cmp.aStarRoute != null) {
            System.out.println("  Path     : " + formatPath(cmp.aStarRoute));
            System.out.printf("  Distance : %.2f km%n", cmp.aStarRoute.getTotalDistance());
            System.out.printf("  Time     : %.1f min%n", cmp.aStarRoute.getTotalTime() * 60);
            System.out.printf("  Computed : %.3f ms%n", cmp.aStarTimeNs / 1_000_000.0);
        } else {
            System.out.println("  No route found.");
        }
        System.out.println("------------------------------------------\n");
    }

    private static void findNearbyPlaces(NavigationSystem nav, Scanner sc) {
        System.out.println("FIND NEARBY PLACES");

        Set<String> categories = nav.getCategories();
        if (categories.isEmpty()) {
            System.out.println("No places loaded.\n");
            return;
        }

        String[] catArray = categories.toArray(new String[0]);
        for (int i = 0; i < catArray.length; i++) {
            System.out.printf("  %d. %s (%d places)%n",
                    i + 1, catArray[i], nav.getPOIsByCategory(catArray[i]).size());
        }

        int catChoice = readInt(sc, "Choose category (1-" + catArray.length + "): ");
        if (catChoice == -1) return;
        if (catChoice < 1 || catChoice > catArray.length) {
            System.out.println("Invalid choice.\n");
            return;
        }
        String selectedCat = catArray[catChoice - 1];

        System.out.print("Your location (Node ID): ");
        String nodeId = sc.nextLine().trim().toUpperCase();
        Node userNode = nav.getGraph().getNode(nodeId);
        if (userNode == null) {
            System.out.println("Unknown location ID.\n");
            return;
        }

        List<PointOfInterest> nearest = nav.findNearestPOIs(selectedCat,
                userNode.getLatitude(), userNode.getLongitude(), 3);

        if (nearest.isEmpty()) {
            System.out.println("No " + selectedCat + " places found.\n");
            return;
        }

        System.out.println("\nTop " + nearest.size() + " " + selectedCat + "s near " + userNode.getName() + ":");
        System.out.println("------------------------------------------");
        for (int i = 0; i < nearest.size(); i++) {
            PointOfInterest poi = nearest.get(i);
            double dist = haversineDist(userNode.getLatitude(), userNode.getLongitude(),
                    poi.getLatitude(), poi.getLongitude());
            System.out.printf("  %d. %-22s  %.1f stars  %.1f km%n",
                    i + 1, poi.getName(), poi.getRating(), dist);
        }
        System.out.println("------------------------------------------");

        System.out.print("Navigate to (1-" + nearest.size() + ", or 0 to skip): ");
        int navChoice;
        try {
            navChoice = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            navChoice = 0;
        }

        if (navChoice < 1 || navChoice > nearest.size()) {
            System.out.println("Returning to menu.\n");
            return;
        }

        PointOfInterest chosen = nearest.get(navChoice - 1);
        System.out.println("Navigating to: " + chosen.getName());

        Node chosenNode = nav.findNearestNode(chosen.getLatitude(), chosen.getLongitude());
        if (chosenNode == null) {
            System.out.println("No graph node found near " + chosen.getName() + ".\n");
            return;
        }

        long t0 = System.nanoTime();
        Route route = nav.findRoute(nodeId, chosenNode.getId());
        long elapsed = System.nanoTime() - t0;

        if (route == null) {
            System.out.println("No route found to " + chosen.getName() + ".\n");
        } else {
            printRoute(route, elapsed);
        }
    }

    private static void addPlace(NavigationSystem nav, Scanner sc) {
        System.out.println("ADD A PLACE (POI)");
        System.out.print("Place ID       : ");
        String id = sc.nextLine().trim().toUpperCase();
        System.out.print("Place Name     : ");
        String name = sc.nextLine().trim();
        System.out.print("Category       : ");
        String cat = sc.nextLine().trim().toUpperCase();

        double rating = readDouble(sc, "Rating (1-5)   : ");
        if (Double.isNaN(rating)) return;
        double lat    = readDouble(sc, "Latitude       : ");
        if (Double.isNaN(lat)) return;
        double lon    = readDouble(sc, "Longitude      : ");
        if (Double.isNaN(lon)) return;

        Node nearest = nav.findNearestNode(lat, lon);
        if (nearest == null) {
            System.out.println("No graph nodes exist yet. Add locations first.\n");
            return;
        }

        nav.addPOI(new PointOfInterest(id, name, cat, rating, lat, lon));
        System.out.println("Place added: " + name + " [" + cat + "] " + rating + " stars"
                + "  (nearest node: " + nearest.getName() + ")\n");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static void printRoute(Route route, long elapsedNs) {
        StringBuilder path = new StringBuilder();
        for (int i = 0; i < route.getNodes().size(); i++) {
            if (i > 0) path.append(" -> ");
            path.append(route.getNodes().get(i).getName());
        }
        System.out.println("------------------------------------------");
        System.out.println("Path     : " + path);
        System.out.printf("Distance : %.2f km%n", route.getTotalDistance());
        System.out.printf("Time     : %.1f minutes%n", route.getTotalTime() * 60);
        System.out.printf("Computed : %.3f ms%n", elapsedNs / 1_000_000.0);
        System.out.println("------------------------------------------\n");
    }

    private static String formatPath(Route route) {
        List<Node> nodes = route.getNodes();
        if (nodes.size() <= 3) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < nodes.size(); i++) {
                if (i > 0) sb.append(" -> ");
                sb.append(nodes.get(i).getId());
            }
            return sb.toString();
        }
        return nodes.get(0).getId() + " -> ... -> " + nodes.get(nodes.size() - 1).getId()
                + " (" + nodes.size() + " stops)";
    }

    private static double readDouble(Scanner sc, String prompt) {
        System.out.print(prompt);
        try {
            return Double.parseDouble(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid number.\n");
            return Double.NaN;
        }
    }

    private static int readInt(Scanner sc, String prompt) {
        System.out.print(prompt);
        try {
            return Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid choice.\n");
            return -1;
        }
    }

    private static void printMenu(NavigationSystem nav) {
        String strat = (nav.getStrategy() instanceof DijkstraStrategy) ? "Dijkstra" : "A*";
        System.out.println("------------------------------------------");
        System.out.println("  1  Show All Locations");
        System.out.println("  2  Find Route");
        System.out.println("  3  Switch Strategy  [" + strat + "]");
        System.out.println("  4  Find Nearest Location");
        System.out.println("  5  Add Location");
        System.out.println("  6  Add Road");
        System.out.println("  7  Map Statistics");
        System.out.println("  8  Delete Location");
        System.out.println("  9  Multi-Stop Route");
        System.out.println(" 10  Compare Routes (Dijkstra vs A*)");
        System.out.println(" 11  Find Nearby Places");
        System.out.println(" 12  Add a Place");
        System.out.println("  0  Exit");
        System.out.println("------------------------------------------");
    }

    private static double haversineDist(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
