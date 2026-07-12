[← Back to README](../README.md)

# Getting Started

## How to Run

```bash
# Compile all source files into out/
javac -d out model\Node.java model\Edge.java model\Route.java model\PointOfInterest.java graph\Graph.java spatial\QuadTree.java spatial\POIQuadTree.java strategy\RoutingStrategy.java strategy\DijkstraStrategy.java strategy\AStarStrategy.java engine\NavigationSystem.java loader\SeedFileLoader.java Main.java

# Run (must be run from the final/ directory so seeds/ is found)
java -cp out Main

# Run benchmarks
java -cp out BenchmarkRunner
```

At startup, the program will prompt for a seed file path. Press **Enter** to load the default city (`seeds/default_city.txt`), or type a custom path:

```
Enter seed file path (press Enter for default city): seeds/my_city.txt
```

---

## Seed File Format

City map data is loaded from a plain-text seed file at startup. This separates data from code and lets anyone define a custom city without touching Java.

```text
# Lines starting with # are comments. Blank lines are ignored.
# Sections must appear in this order: NODES -> EDGES -> POIS (POIS optional)

# NODES
# ID, Name, Latitude, Longitude
CTR, Central Plaza, 12.9756, 77.6068

# EDGES
# SourceID, DestID, Distance(km), SpeedLimit(km/h)
CTR, GDN, 1.5, 25.0

# POIS
# ID, Name, Category, Rating, Latitude, Longitude
H1, Grand Plaza Hotel, HOTEL, 4.2, 12.9760, 77.6075
```

### Validation

| Type | Behaviour |
|------|-----------|
| Wrong field count | **Hard error** — stops loading, prints line number |
| Duplicate node ID | **Hard error** — stops loading, prints line number |
| Edge references unknown node | **Hard error** — stops loading, prints line number |
| Invalid number format | **Hard error** — stops loading, prints line number |
| Lat/lon out of range | **Warning** — entry still loaded |
| Distance or speed ≤ 0 | **Warning** — edge skipped |
| Rating outside [1.0, 5.0] | **Warning** — POI still loaded |
| Duplicate POI ID | **Warning** — second entry skipped |

On a hard error the program offers to fall back to `seeds/default_city.txt`.
