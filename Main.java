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
    private static final RoutingStrategy A_STAR = new AStarStrategy();

    private static final String CYAN    = "[36m";
    private static final String GREEN   = "[32m";
    private static final String YELLOW  = "[33m";
    private static final String RED     = "[31m";
    private static final String MAGENTA = "[35m";
    private static final String BOLD    = "[1m";
    private static final String DIM     = "[2m";
    private static final String RESET   = "[0m";

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Build an empty NavigationSystem — seed file fills it with data
        Graph graph = new Graph();
        double[] bounds = { 12.84, 13.21, 77.56, 77.80 };
        QuadTree quadTree = new QuadTree(bounds[0], bounds[1], bounds[2], bounds[3]);
        NavigationSystem navSystem = new NavigationSystem(graph, quadTree, DIJKSTRA,
                bounds[0], bounds[1], bounds[2], bounds[3]);

        // Prompt for seed file
        System.out.print("\n  Enter seed file path (press Enter for default city): ");
        String path = sc.nextLine().trim();
        if (path.isEmpty()) path = "seeds/default_city.txt";

        try {
            SeedFileLoader.load(navSystem, path);
        } catch (SeedFileLoader.SeedFileException e) {
            System.out.println("  " + RED + "[ERROR] " + e.getMessage() + RESET);
            if (!loadFallback(navSystem, sc)) { sc.close(); return; }
        } catch (FileNotFoundException e) {
            System.out.println("  " + RED + "[ERROR] File not found: " + path + RESET);
            if (!loadFallback(navSystem, sc)) { sc.close(); return; }
        } catch (IOException e) {
            System.out.println("  " + RED + "[ERROR] Could not read file: " + e.getMessage() + RESET);
            if (!loadFallback(navSystem, sc)) { sc.close(); return; }
        }

        printBanner();

        boolean running = true;
        while (running) {
            printMenu(navSystem);
            System.out.print(CYAN + "  > " + RESET);
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
                    System.out.println(DIM + "  Goodbye! Thank you for using GeoNav." + RESET + "\n");
                    break;
                default:
                    System.out.println(RED + "  Invalid choice. Enter 0-12." + RESET + "\n");
            }
        }

        sc.close();
    }

    private static boolean loadFallback(NavigationSystem navSystem, Scanner sc) {
        System.out.print("  Load default city instead? (y/n): ");
        if (!sc.nextLine().trim().equalsIgnoreCase("y")) return false;
        try {
            SeedFileLoader.load(navSystem, "seeds/default_city.txt");
            return true;
        } catch (Exception e) {
            System.out.println("  " + RED + "[ERROR] Could not load default city: " + e.getMessage() + RESET);
            return false;
        }
    }

    // ══════════════════════════════════════════════════════════
    // MENU ACTIONS (1-12)
    // ══════════════════════════════════════════════════════════

    private static void showAllLocations(NavigationSystem nav) {
        System.out.println(BOLD + "  LOCATIONS ON MAP" + RESET);
        System.out.println(DIM + "  ────────────────────────────────────────────" + RESET);
        System.out.printf("  " + DIM + "%-5s" + RESET + "  %-16s  %10s  %10s\n",
                "ID", "Name", "Latitude", "Longitude");
        System.out.println(DIM + "  ────────────────────────────────────────────" + RESET);

        for (Node node : nav.getGraph().getAllNodes()) {
            System.out.printf("  " + CYAN + "%-5s" + RESET + "  %-16s  %10.4f  %10.4f\n",
                    node.getId(), node.getName(), node.getLatitude(), node.getLongitude());
        }
        System.out.println(DIM + "  ────────────────────────────────────────────" + RESET);
        System.out.printf("  " + DIM + "Total: %d locations" + RESET + "\n\n", nav.getGraph().getNodeCount());
    }

    private static void findRoute(NavigationSystem nav, Scanner sc) {
        System.out.println(BOLD + "  FIND ROUTE" + RESET);
        System.out.println(DIM + "  Strategy: " + nav.getStrategy().getStrategyName() + RESET);
        System.out.println();

        System.out.print("  " + YELLOW + "Source ID      : " + RESET);
        String srcId = sc.nextLine().trim().toUpperCase();
        System.out.print("  " + YELLOW + "Destination ID : " + RESET);
        String destId = sc.nextLine().trim().toUpperCase();

        if (srcId.equals(destId)) {
            System.out.println("\n  " + YELLOW + "You're already there!" + RESET + "\n");
            return;
        }

        long t0 = System.nanoTime();
        Route route = nav.findRoute(srcId, destId);
        long elapsed = System.nanoTime() - t0;

        if (route == null) {
            System.out.println("\n  " + RED + "No route found. Check that IDs are correct." + RESET + "\n");
        } else {
            printRouteBox(route, elapsed, GREEN);
        }
    }

    private static void switchStrategy(NavigationSystem nav) {
        if (nav.getStrategy() instanceof DijkstraStrategy) {
            nav.setStrategy(A_STAR);
        } else {
            nav.setStrategy(DIJKSTRA);
        }
        System.out.println("  " + GREEN + "Switched to: " + BOLD
                + nav.getStrategy().getStrategyName() + RESET + "\n");
    }

    private static void findNearest(NavigationSystem nav, Scanner sc) {
        System.out.println(BOLD + "  FIND NEAREST LOCATION" + RESET);
        System.out.println();

        double lat = readDouble(sc, "Latitude  : ");
        if (Double.isNaN(lat)) return;
        double lon = readDouble(sc, "Longitude : ");
        if (Double.isNaN(lon)) return;

        Node nearest = nav.findNearestNode(lat, lon);
        if (nearest != null) {
            System.out.println("\n  " + GREEN + "Nearest: " + BOLD + nearest.getName()
                    + RESET + GREEN + " (" + nearest.getId() + ")"
                    + " [" + nearest.getLatitude() + ", " + nearest.getLongitude() + "]" + RESET + "\n");
        } else {
            System.out.println("\n  " + RED + "No locations found." + RESET + "\n");
        }
    }

    private static void addLocation(NavigationSystem nav, Scanner sc) {
        System.out.println(BOLD + "  ADD NEW LOCATION" + RESET);
        System.out.println();

        System.out.print("  " + YELLOW + "Location ID   : " + RESET);
        String id = sc.nextLine().trim().toUpperCase();
        if (nav.getGraph().getNode(id) != null) {
            System.out.println("\n  " + RED + "ID '" + id + "' already exists." + RESET + "\n");
            return;
        }

        System.out.print("  " + YELLOW + "Location Name : " + RESET);
        String name = sc.nextLine().trim();

        double lat = readDouble(sc, "Latitude      : ");
        if (Double.isNaN(lat)) return;
        double lon = readDouble(sc, "Longitude     : ");
        if (Double.isNaN(lon)) return;

        nav.addLocation(new Node(id, name, lat, lon));
        System.out.println("\n  " + GREEN + "Added: " + BOLD + name + RESET + GREEN
                + " (" + id + ")" + RESET + "\n");
    }

    private static void deleteLocation(NavigationSystem nav, Scanner sc) {
        System.out.println(BOLD + "  DELETE LOCATION" + RESET);
        System.out.println(DIM + "  (removes the location and all connected roads)" + RESET);
        System.out.println();

        System.out.print("  " + YELLOW + "Location ID : " + RESET);
        String id = sc.nextLine().trim().toUpperCase();

        if (nav.getGraph().getNode(id) == null) {
            System.out.println("\n  " + RED + "No location with ID '" + id + "' found." + RESET + "\n");
            return;
        }

        String name = nav.getGraph().getNode(id).getName();
        boolean removed = nav.removeLocation(id);
        if (removed) {
            System.out.println("\n  " + GREEN + "Deleted: " + BOLD + name + RESET + GREEN
                    + " (" + id + ") and all connected roads." + RESET + "\n");
        } else {
            System.out.println("\n  " + RED + "Failed to delete location." + RESET + "\n");
        }
    }

    private static void addRoad(NavigationSystem nav, Scanner sc) {
        System.out.println(BOLD + "  ADD NEW ROAD" + RESET);
        System.out.println(DIM + "  (creates a bidirectional connection)" + RESET);
        System.out.println();

        System.out.print("  " + YELLOW + "From ID           : " + RESET);
        String srcId = sc.nextLine().trim().toUpperCase();
        System.out.print("  " + YELLOW + "To ID             : " + RESET);
        String destId = sc.nextLine().trim().toUpperCase();

        if (nav.getGraph().getNode(srcId) == null || nav.getGraph().getNode(destId) == null) {
            System.out.println("\n  " + RED + "Invalid ID(s). Add locations first." + RESET + "\n");
            return;
        }

        double dist = readDouble(sc, "Distance (km)     : ");
        if (Double.isNaN(dist)) return;
        double speed = readDouble(sc, "Speed limit (km/h): ");
        if (Double.isNaN(speed)) return;

        nav.addRoad(srcId, destId, dist, speed);
        System.out.println("\n  " + GREEN + "Road added: " + BOLD + srcId + " <-> " + destId
                + RESET + GREEN + " (" + dist + " km, " + speed + " km/h)" + RESET + "\n");
    }

    private static void showMapStats(NavigationSystem nav) {
        System.out.println(BOLD + "  MAP STATISTICS" + RESET);
        System.out.println(DIM + "  ────────────────────────────────────────────" + RESET);
        System.out.printf("  Locations  : " + CYAN + "%d" + RESET + "\n", nav.getGraph().getNodeCount());
        System.out.printf("  Roads      : " + CYAN + "%d" + RESET + "\n", nav.getGraph().getEdgeCount());
        System.out.printf("  Strategy   : " + CYAN + "%s" + RESET + "\n", nav.getStrategy().getStrategyName());
        System.out.println(DIM + "  ────────────────────────────────────────────" + RESET + "\n");
    }

    private static void multiStopRoute(NavigationSystem nav, Scanner sc) {
        System.out.println(BOLD + "  MULTI-STOP ROUTE" + RESET);
        System.out.println(DIM + "  Enter stop IDs separated by commas (min 2)" + RESET);
        System.out.println(DIM + "  Example: CTR, MKT, LKS, PRT" + RESET);
        System.out.println();

        System.out.print("  " + YELLOW + "Stops: " + RESET);
        String input = sc.nextLine().trim().toUpperCase();
        String[] parts = input.split("\\s*,\\s*");

        if (parts.length < 2) {
            System.out.println("\n  " + RED + "Need at least 2 stops." + RESET + "\n");
            return;
        }

        List<String> stops = new ArrayList<>();
        for (String p : parts) {
            if (!p.isEmpty()) stops.add(p);
        }

        long t0 = System.nanoTime();
        Route route = nav.findMultiStopRoute(stops);
        long elapsed = System.nanoTime() - t0;

        if (route == null) {
            System.out.println("\n  " + RED + "No route found. Check IDs and connectivity." + RESET + "\n");
        } else {
            System.out.println();
            System.out.println(MAGENTA + "  ╔══════════════════════════════════════════════════╗" + RESET);
            System.out.println(MAGENTA + "  ║  MULTI-STOP ROUTE                                ║" + RESET);
            System.out.println(MAGENTA + "  ╠══════════════════════════════════════════════════╣" + RESET);
            System.out.println(MAGENTA + "  ║  " + RESET + "Stops: " + BOLD + String.join(" -> ", stops) + RESET);

            StringBuilder pathStr = new StringBuilder();
            for (int i = 0; i < route.getNodes().size(); i++) {
                if (i > 0) pathStr.append(" -> ");
                pathStr.append(route.getNodes().get(i).getName());
            }
            System.out.println(MAGENTA + "  ║  " + RESET + "Path : " + DIM + pathStr + RESET);
            System.out.printf(MAGENTA + "  ║  " + RESET + "Total Distance : " + CYAN + "%.2f km" + RESET + "\n",
                    route.getTotalDistance());
            System.out.printf(MAGENTA + "  ║  " + RESET + "Total Time     : " + CYAN + "%.1f minutes" + RESET + "\n",
                    route.getTotalTime() * 60);
            System.out.printf(MAGENTA + "  ║  " + RESET + DIM + "Computed in %.3f ms" + RESET + "\n",
                    elapsed / 1_000_000.0);
            System.out.println(MAGENTA + "  ╚══════════════════════════════════════════════════╝" + RESET + "\n");
        }
    }

    private static void compareRoutes(NavigationSystem nav, Scanner sc) {
        System.out.println(BOLD + "  ROUTE COMPARISON — Dijkstra vs A*" + RESET);
        System.out.println();
        System.out.print("  " + YELLOW + "Source ID      : " + RESET);
        String srcId = sc.nextLine().trim().toUpperCase();
        System.out.print("  " + YELLOW + "Destination ID : " + RESET);
        String destId = sc.nextLine().trim().toUpperCase();

        NavigationSystem.ComparisonResult cmp = nav.compareRoutes(srcId, destId);
        if (cmp == null) {
            System.out.println("\n  " + RED + "Invalid IDs." + RESET + "\n");
            return;
        }

        System.out.println();
        System.out.println(BOLD + "  ┌─────────────────────────┬─────────────────────────┐" + RESET);
        System.out.printf(BOLD + "  │ %-24s" + RESET + BOLD + "│ %-24s│" + RESET + "\n",
                CYAN + " Dijkstra (Distance)" + RESET, MAGENTA + " A* Search (Time)" + RESET);
        System.out.println(BOLD + "  ├─────────────────────────┼─────────────────────────┤" + RESET);

        String dPath = cmp.dijkstraRoute != null ? formatPathShort(cmp.dijkstraRoute) : "No route";
        String aPath = cmp.aStarRoute    != null ? formatPathShort(cmp.aStarRoute)    : "No route";
        System.out.printf("  │ %-24s│ %-24s│\n", " " + dPath, " " + aPath);

        String dDist = cmp.dijkstraRoute != null ? String.format("%.2f km", cmp.dijkstraRoute.getTotalDistance()) : "-";
        String aDist = cmp.aStarRoute    != null ? String.format("%.2f km", cmp.aStarRoute.getTotalDistance())    : "-";
        System.out.printf("  │ Dist: %-18s│ Dist: %-18s│\n", dDist, aDist);

        String dTime = cmp.dijkstraRoute != null ? String.format("%.1f min", cmp.dijkstraRoute.getTotalTime() * 60) : "-";
        String aTime = cmp.aStarRoute    != null ? String.format("%.1f min", cmp.aStarRoute.getTotalTime() * 60)    : "-";
        System.out.printf("  │ Time: %-18s│ Time: %-18s│\n", dTime, aTime);

        System.out.printf("  │ Comp: %-18s│ Comp: %-18s│\n",
                String.format("%.3f ms", cmp.dijkstraTimeNs / 1_000_000.0),
                String.format("%.3f ms", cmp.aStarTimeNs    / 1_000_000.0));

        System.out.println("  └─────────────────────────┴─────────────────────────┘");
        System.out.println();
    }

    // ══════════════════════════════════════════════════════════
    // POI FEATURES (11-12)
    // ══════════════════════════════════════════════════════════

    private static void findNearbyPlaces(NavigationSystem nav, Scanner sc) {
        System.out.println(BOLD + "  FIND NEARBY PLACES" + RESET);
        System.out.println();

        Set<String> categories = nav.getCategories();
        if (categories.isEmpty()) {
            System.out.println("  " + RED + "No places loaded." + RESET + "\n");
            return;
        }

        System.out.println("  " + DIM + "Categories:" + RESET);
        String[] catArray = categories.toArray(new String[0]);
        for (int i = 0; i < catArray.length; i++) {
            int count = nav.getPOIsByCategory(catArray[i]).size();
            System.out.printf("  " + CYAN + BOLD + "  %d." + RESET + " %s " + DIM + "(%d places)" + RESET + "\n",
                    i + 1, catArray[i], count);
        }
        System.out.println();
        int catChoice = readInt(sc, "Choose category (1-" + catArray.length + "): ");
        if (catChoice == -1) return;
        if (catChoice < 1 || catChoice > catArray.length) {
            System.out.println("\n  " + RED + "Invalid choice." + RESET + "\n");
            return;
        }
        String selectedCat = catArray[catChoice - 1];

        System.out.print("  " + YELLOW + "Your location (Node ID): " + RESET);
        String nodeId = sc.nextLine().trim().toUpperCase();
        Node userNode = nav.getGraph().getNode(nodeId);
        if (userNode == null) {
            System.out.println("\n  " + RED + "Unknown location ID." + RESET + "\n");
            return;
        }

        List<PointOfInterest> nearest = nav.findNearestPOIs(selectedCat,
                userNode.getLatitude(), userNode.getLongitude(), 3);

        if (nearest.isEmpty()) {
            System.out.println("\n  " + RED + "No " + selectedCat + " places found." + RESET + "\n");
            return;
        }

        System.out.println();
        System.out.println(BOLD + "  TOP " + nearest.size() + " " + selectedCat + "S NEAR " + userNode.getName() + RESET);
        System.out.println(DIM + "  ────────────────────────────────────────────────────────" + RESET);
        System.out.printf("  " + DIM + "%-3s %-22s %-14s %s" + RESET + "\n", "#", "Name", "Rating", "Distance");
        System.out.println(DIM + "  ────────────────────────────────────────────────────────" + RESET);

        for (int i = 0; i < nearest.size(); i++) {
            PointOfInterest poi = nearest.get(i);
            double dist = haversineDist(userNode.getLatitude(), userNode.getLongitude(),
                    poi.getLatitude(), poi.getLongitude());
            System.out.printf("  " + CYAN + BOLD + "%-3d" + RESET + " %-22s "
                    + YELLOW + "%-14s" + RESET + " " + GREEN + "%.1f km" + RESET + "\n",
                    i + 1, poi.getName(), poi.getStars() + " (" + poi.getRating() + ")", dist);
        }
        System.out.println(DIM + "  ────────────────────────────────────────────────────────" + RESET);

        System.out.println();
        System.out.print("  " + YELLOW + "Navigate to (1-" + nearest.size() + ", or 0 to skip): " + RESET);
        int navChoice;
        try {
            navChoice = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            navChoice = 0;
        }

        if (navChoice < 1 || navChoice > nearest.size()) {
            System.out.println(DIM + "  Returning to menu." + RESET + "\n");
            return;
        }

        PointOfInterest chosen = nearest.get(navChoice - 1);
        System.out.println();
        System.out.println(BOLD + "  NAVIGATING TO: " + RESET + CYAN + chosen.getName() + RESET);

        Node chosenNode = nav.findNearestNode(chosen.getLatitude(), chosen.getLongitude());
        if (chosenNode == null) {
            System.out.println("  " + RED + "No graph node found near " + chosen.getName() + "." + RESET + "\n");
            return;
        }

        long t0 = System.nanoTime();
        Route route = nav.findRoute(nodeId, chosenNode.getId());
        long elapsed = System.nanoTime() - t0;

        if (route == null) {
            System.out.println("  " + RED + "No route found to " + chosen.getName() + "." + RESET + "\n");
            return;
        }

        printRouteBox(route, elapsed, GREEN);
    }

    private static void addPlace(NavigationSystem nav, Scanner sc) {
        System.out.println(BOLD + "  ADD A PLACE (POI)" + RESET);
        System.out.println();

        System.out.print("  " + YELLOW + "Place ID       : " + RESET);
        String id = sc.nextLine().trim().toUpperCase();
        System.out.print("  " + YELLOW + "Place Name     : " + RESET);
        String name = sc.nextLine().trim();
        System.out.print("  " + YELLOW + "Category       : " + RESET);
        String cat = sc.nextLine().trim().toUpperCase();

        double rating = readDouble(sc, "Rating (1-5)   : ");
        if (Double.isNaN(rating)) return;
        double lat = readDouble(sc, "Latitude       : ");
        if (Double.isNaN(lat)) return;
        double lon = readDouble(sc, "Longitude      : ");
        if (Double.isNaN(lon)) return;

        Node nearest = nav.findNearestNode(lat, lon);
        if (nearest == null) {
            System.out.println("\n  " + RED + "No graph nodes exist yet. Add locations first." + RESET + "\n");
            return;
        }

        nav.addPOI(new PointOfInterest(id, name, cat, rating, lat, lon));
        System.out.println("\n  " + GREEN + "Place added: " + BOLD + name + RESET + GREEN
                + " [" + cat + "] " + rating + " stars"
                + DIM + "  (nearest node: " + nearest.getName() + ")" + RESET + "\n");
    }

    // Approximate distance in km (haversine formula)
    private static double haversineDist(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    // ══════════════════════════════════════════════════════════
    // UI HELPERS
    // ══════════════════════════════════════════════════════════

    private static void printRouteBox(Route route, long elapsedNs, String color) {
        System.out.println();
        System.out.println(color + "  ╔══════════════════════════════════════════╗" + RESET);
        System.out.println(color + "  ║  ROUTE FOUND                             ║" + RESET);
        System.out.println(color + "  ╠══════════════════════════════════════════╣" + RESET);

        StringBuilder pathStr = new StringBuilder();
        for (int i = 0; i < route.getNodes().size(); i++) {
            if (i > 0) pathStr.append(" -> ");
            pathStr.append(route.getNodes().get(i).getName());
        }
        System.out.println(color + "  ║  " + RESET + "Path : " + BOLD + pathStr + RESET);
        System.out.printf(color + "  ║  " + RESET + "Distance : " + CYAN + "%.2f km" + RESET + "\n",
                route.getTotalDistance());
        System.out.printf(color + "  ║  " + RESET + "Time     : " + CYAN + "%.1f minutes" + RESET + "\n",
                route.getTotalTime() * 60);
        System.out.printf(color + "  ║  " + RESET + DIM + "Computed in %.3f ms" + RESET + "\n",
                elapsedNs / 1_000_000.0);
        System.out.println(color + "  ╚══════════════════════════════════════════╝" + RESET + "\n");
    }

    private static String formatPathShort(Route route) {
        List<Node> nodes = route.getNodes();
        if (nodes.size() <= 3) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < nodes.size(); i++) {
                if (i > 0) sb.append("->");
                sb.append(nodes.get(i).getId());
            }
            return sb.toString();
        }
        return nodes.get(0).getId() + "->...->" + nodes.get(nodes.size() - 1).getId()
                + " (" + nodes.size() + " stops)";
    }

    private static double readDouble(Scanner sc, String prompt) {
        System.out.print("  " + YELLOW + prompt + RESET);
        try {
            return Double.parseDouble(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("\n  " + RED + "Invalid number. Please enter a numeric value." + RESET + "\n");
            return Double.NaN;
        }
    }

    private static int readInt(Scanner sc, String prompt) {
        System.out.print("  " + YELLOW + prompt + RESET);
        try {
            return Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("\n  " + RED + "Invalid choice." + RESET + "\n");
            return -1;
        }
    }

    private static void printBanner() {
        System.out.println();
        System.out.println(CYAN + BOLD + "   ____            _   _             " + RESET);
        System.out.println(CYAN + BOLD + "  / ___| ___  ___ | \\ | | __ ___   __" + RESET);
        System.out.println(CYAN + BOLD + " | |  _ / _ \\/ _ \\|  \\| |/ _` \\ \\ / /" + RESET);
        System.out.println(CYAN + BOLD + " | |_| |  __/ (_) | |\\  | (_| |\\ V / " + RESET);
        System.out.println(CYAN + BOLD + "  \\____|\\___|\\___/|_| \\_|\\__,_| \\_/  " + RESET);
        System.out.println();
        System.out.println(DIM + "  Geospatial Routing & Navigation Engine" + RESET);
        System.out.println();
    }

    private static void printMenu(NavigationSystem nav) {
        String strat = (nav.getStrategy() instanceof DijkstraStrategy) ? "Dijkstra" : "A*";

        System.out.println(DIM + "  ────────────────────────────────────────────" + RESET);
        System.out.println("  " + BOLD + " 1" + RESET + "  Show All Locations");
        System.out.println("  " + BOLD + " 2" + RESET + "  Find Route");
        System.out.println("  " + BOLD + " 3" + RESET + "  Switch Strategy " + DIM + "[" + strat + "]" + RESET);
        System.out.println("  " + BOLD + " 4" + RESET + "  Find Nearest Location");
        System.out.println("  " + MAGENTA + BOLD + " 5" + RESET + "  Add Location");
        System.out.println("  " + MAGENTA + BOLD + " 6" + RESET + "  Add Road");
        System.out.println("  " + BOLD + " 7" + RESET + "  Map Statistics");
        System.out.println("  " + RED + BOLD + " 8" + RESET + "  Delete Location");
        System.out.println("  " + CYAN + BOLD + " 9" + RESET + "  Multi-Stop Route");
        System.out.println("  " + CYAN + BOLD + "10" + RESET + "  Compare Routes " + DIM + "(Dijkstra vs A*)" + RESET);
        System.out.println("  " + CYAN + BOLD + "11" + RESET + "  Find Nearby Places " + DIM + "(Hotels, Restaurants...)" + RESET);
        System.out.println("  " + MAGENTA + BOLD + "12" + RESET + "  Add a Place");
        System.out.println("  " + DIM + " 0" + RESET + "  Exit");
        System.out.println(DIM + "  ────────────────────────────────────────────" + RESET);
    }
}
