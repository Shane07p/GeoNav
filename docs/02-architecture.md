[← Back to README](../README.md)

# Architecture

## System Architecture

```mermaid
flowchart LR
    subgraph ENTRY["Entry Point"]
        direction TB
        MAIN["Main.java\nConsole UI"]:::entry
        LOADER["SeedFileLoader\nload · validate · warn"]:::entry
    end

    subgraph ENGINE["Engine"]
        NAV["NavigationSystem\nfindRoute · compareRoutes\naddLocation · addRoad\nfindNearestNode · findNearestPOIs"]:::engine
    end

    subgraph ALGORITHMS["Algorithms  strategy/"]
        direction TB
        IFACE["RoutingStrategy\ninterface"]:::iface
        DIJK["DijkstraStrategy\nShortest Distance\nO((V+E) log V)"]:::algo
        ASTAR["AStarStrategy\nFastest Time\nO((V+E) log V)"]:::algo
        IFACE --> DIJK
        IFACE --> ASTAR
    end

    subgraph DATASTRUCT["Data Structures"]
        direction TB
        G["Graph\nO(1) add · O(V+E) remove\nedgeCount O(1)"]:::struct
        QT["QuadTree\nNearest Node · O(log N)"]:::struct
        POIQT["POIQuadTree\nK-Nearest POI · O(K log N)"]:::struct
    end

    subgraph MODEL["Model  model/"]
        direction TB
        NODE["Node\nid · name · lat · lon"]:::model
        EDGE["Edge\ndist · speed · travelTime"]:::model
        ROUTE["Route\npath · distance · time"]:::model
        POI["PointOfInterest\nname · category · rating"]:::model
    end

    MAIN   --> LOADER
    LOADER --> NAV
    MAIN   --> NAV
    NAV    --> IFACE
    NAV    --> G
    NAV    --> QT
    NAV    --> POIQT
    DIJK   --> G
    ASTAR  --> G
    G      --> NODE
    G      --> EDGE
    NAV    --> ROUTE
    NAV    --> POI

    classDef entry  fill:#dbeafe,stroke:#3b82f6,color:#000
    classDef engine fill:#d1fae5,stroke:#10b981,color:#000
    classDef iface  fill:#f3f4f6,stroke:#6b7280,color:#000
    classDef algo   fill:#fef3c7,stroke:#f59e0b,color:#000
    classDef struct fill:#ede9fe,stroke:#8b5cf6,color:#000
    classDef model  fill:#fce7f3,stroke:#ec4899,color:#000

    style ENTRY      fill:#eff6ff,stroke:#3b82f6
    style ENGINE     fill:#ecfdf5,stroke:#10b981
    style ALGORITHMS fill:#fffbeb,stroke:#f59e0b
    style DATASTRUCT fill:#f5f3ff,stroke:#8b5cf6
    style MODEL      fill:#fdf2f8,stroke:#ec4899
```

---

## Project Structure

```
final/
├── Main.java                        # Interactive console UI
├── BenchmarkRunner.java             # Standalone performance benchmarking tool
├── model/
│   ├── Node.java                    # Geographic location (id, name, lat, lon)
│   ├── Edge.java                    # Road segment (distance, speed limit)
│   ├── Route.java                   # Query result (path, distance, time)
│   └── PointOfInterest.java         # POI (name, category, rating, coords)
├── graph/
│   └── Graph.java                   # Adjacency-list weighted graph
├── spatial/
│   ├── QuadTree.java                # Spatial index for nearest-neighbor queries
│   └── POIQuadTree.java             # K-nearest POI search (max-heap pruning)
├── strategy/
│   ├── RoutingStrategy.java         # Routing algorithm interface
│   ├── DijkstraStrategy.java        # Shortest-distance pathfinding
│   └── AStarStrategy.java           # Fastest-time pathfinding (heuristic)
├── engine/
│   └── NavigationSystem.java        # Core engine coordinating graph, QuadTree & routing
├── loader/
│   └── SeedFileLoader.java          # Parses seed files; validates entries with warnings/errors
├── seeds/
│   └── default_city.txt             # Default city map (14 nodes, 22 roads, 33 POIs)
└── out/                             # Compiled .class files (generated, not committed)
```
