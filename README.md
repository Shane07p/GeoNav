# GeoNav — Geospatial Routing & Navigation System

A console-based geospatial routing engine built in Java. The system models a city map and supports route planning using **Dijkstra's Algorithm** (shortest distance) and **A\* Search** (fastest time), powered by a **QuadTree** spatial index for efficient nearest-neighbor queries and a **POI system** for finding nearby places by category.

---

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

---

## Features

| # | Feature | Description |
|---|---------|-------------|
| 1 | Show All Locations | Lists every node with coordinates |
| 2 | Find Route | Single source → destination route |
| 3 | Switch Strategy | Toggle between Dijkstra and A\* at runtime |
| 4 | Find Nearest Location | QuadTree-powered nearest-neighbor query |
| 5 | Add Location | Dynamically insert a new node |
| 6 | Add Road | Dynamically insert a bidirectional edge |
| 7 | Map Statistics | Node count, edge count, active strategy |
| 8 | Delete Location | Remove a node and all its connected roads |
| 9 | Multi-Stop Route | Chained waypoint routing (A → B → C → …) |
| 10 | Compare Routes | Side-by-side Dijkstra vs A\* comparison |
| 11 | Find Nearby Places | K-nearest POI search by category (Hotels, Restaurants, Malls, Theatres) with option to navigate |
| 12 | Add a Place | Dynamically add a new POI with category and rating |

---

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

---

## Performance Benchmarks

> Measured using `System.nanoTime()` with 200 JIT warm-up runs (discarded) followed by 1,000 measured iterations. Times in microseconds (µs).

### Algorithm Execution Time (CTR → PRT, n = 1,000 runs)

| Algorithm | Avg | Min | Max | P95 |
|-----------|:---:|:---:|:---:|:---:|
| Dijkstra | 6 µs | 3 µs | 108 µs | 10 µs |
| A\* Search | 6 µs | 4 µs | 114 µs | 10 µs |

Both algorithms complete in under **10 µs** per query. At 14 nodes both are equally fast — the meaningful difference between them is **path quality**.

### Path Divergence — All 91 Source–Destination Pairs

Dijkstra optimises for **shortest distance** (km); A\* optimises for **fastest time** (hours). On roads with varying speed limits (12–100 km/h) they produce different routes.

| Metric | Value |
|--------|-------|
| Unique pairs analysed | 91 |
| Pairs where paths differ | **38 / 91 (42%)** |
| Average time saved by A\* | **6.0 min / trip** |
| Maximum time saved by A\* | **27.9 min** (CTR → PRT) |
| Avg hops — Dijkstra | 3.7 nodes |
| Avg hops — A\* | 4.1 nodes |

> A\* takes *more hops* but *less time* — it routes via highways even when that means a longer distance.

### Multi-Stop Route Scaling

Each additional stop adds one routing leg, confirming the theoretical **O(k · (V+E) log V)** complexity.

| Stops | Avg Time | Ratio vs 2-stop |
|:-----:|:--------:|:---------------:|
| 2 | 6 µs | 1.00× (baseline) |
| 3 | 7 µs | 1.14× |
| 4 | 9 µs | 1.49× |
| 5 | 8 µs | 1.29× |

### POI K-Nearest Search Latency

| Category | k = 1 | k = 3 | k = 5 |
|----------|:-----:|:-----:|:-----:|
| HOTEL | 1 µs | 1 µs | 1 µs |
| RESTAURANT | 1 µs | 1 µs | 1 µs |
| MALL | 1 µs | 1 µs | 1 µs |
| THEATRE | 1 µs | 1 µs | 1 µs |

### QuadTree vs Linear Scan (5,000 random probes)

| Method | Avg per probe | Complexity |
|--------|:-------------:|:----------:|
| Linear scan | 264 ns | O(N) |
| QuadTree | 422 ns | O(log N) |

At N = 14 nodes, the QuadTree's traversal overhead exceeds the cost of scanning 14 items directly — the crossover point is typically 50–100+ nodes. The structure is designed for city-scale datasets where O(log N) compounds significantly over O(N).

---

## Time Complexity of Operations

Let **V** = number of nodes (vertices), **E** = number of edges (roads).

### Graph Operations

| Operation | Time Complexity | Explanation |
|-----------|:---------------:|-------------|
| Add Node | **O(1)** | HashMap insertion |
| Add Edge | **O(1)** | Appends to two adjacency lists |
| Remove Node | **O(V + E)** | Removes from map, then scans all adjacency lists to purge edges pointing to the deleted node |
| Get Neighbors | **O(1)** | Direct HashMap lookup |
| Get Node by ID | **O(1)** | Direct HashMap lookup |
| Edge Count | **O(1)** | Counter maintained on `addEdge` / `removeNode` |

### QuadTree Operations

| Operation | Average Case | Worst Case | Explanation |
|-----------|:------------:|:----------:|-------------|
| Insert | **O(log V)** | **O(V)** | Recursively subdivides; degrades if all points are co-located |
| Nearest Neighbor | **O(log V)** | **O(V)** | Branch-and-bound pruning skips irrelevant quadrants |
| K-Nearest (POI) | **O(K log N)** | **O(N)** | Max-heap of size K with branch-and-bound pruning; each eviction O(log K) — heap entry carries the POI directly |

### Routing Algorithms

| Algorithm | Time Complexity | Space Complexity | Explanation |
|-----------|:---------------:|:----------------:|-------------|
| Dijkstra | **O((V + E) log V)** | **O(V)** | Min-heap priority queue; each node extracted once, each edge relaxed once; path reconstruction O(V) via cached edge map |
| A\* Search | **O((V + E) log V)** | **O(V)** | Same worst case as Dijkstra, but the heuristic prunes the search space — in practice explores fewer nodes; reconstruction O(V) via cached edge map |
| Multi-Stop Route | **O(k · (V + E) log V)** | **O(V)** | Chains *k − 1* individual route queries for *k* stops |
| Route Comparison | **O((V + E) log V)** | **O(V)** | Runs both algorithms sequentially |

### Dynamic Map Editing

| Operation | Time Complexity | Explanation |
|-----------|:---------------:|-------------|
| Add Location | **O(V log V)** | Inserts node, then rebuilds QuadTree |
| Remove Location | **O(V + E)** | Removes node and edges, then rebuilds QuadTree |
| Find Nearest | **O(log V)** | Delegates to QuadTree |
| Add POI | **O(N log N)** | Inserts POI, then rebuilds that category's POIQuadTree |
| Find K-Nearest POI | **O(K log N)** | Max-heap pruned search within one category tree |

---

## Algorithm Flowcharts

Dijkstra and A\* share the same priority-queue skeleton. The difference is the **edge weight definition** and the **heuristic term** added to the priority.

**Dijkstra — Shortest Distance**

```mermaid
flowchart LR
    D1([Source\ndist = 0]):::term --> D2[Push to\nmin-heap]:::action
    D2 --> D3{Heap\nempty?}:::dec
    D3 -->|Yes| D9([No path]):::bad
    D3 -->|No| D4[Pop u\nmin dist]:::action
    D4 --> D5{"Stale entry?\ne.priority > dist.get(u)"}:::dec
    D5 -->|Yes| D3
    D5 -->|No| D6{u = dest?}:::dec
    D6 -->|Yes| D8([Return Route]):::term
    D6 -->|No| D7["Relax neighbors\nnewDist = dist_u + km\nif better → update + push"]:::action
    D7 --> D3

    classDef term   fill:#d1e7dd,stroke:#198754,color:#000
    classDef action fill:#cfe2ff,stroke:#0d6efd,color:#000
    classDef dec    fill:#fff3cd,stroke:#ffc107,color:#000
    classDef bad    fill:#f8d7da,stroke:#dc3545,color:#000
```

**A\* — Fastest Time**

```mermaid
flowchart LR
    A1([Source\ng=0, f=h]):::term --> A2[Push to\nmin-heap]:::action
    A2 --> A3{Heap\nempty?}:::dec
    A3 -->|Yes| A9([No path]):::bad
    A3 -->|No| A4[Pop u\nmin f-score]:::action
    A4 --> A5{"Stale entry?\ne.priority > gScore.get(u)+h"}:::dec
    A5 -->|Yes| A3
    A5 -->|No| A6{u = dest?}:::dec
    A6 -->|Yes| A8([Return Route]):::term
    A6 -->|No| A7["g_new = g_u + km/speed\nh = haversine(v,dest) / 60\nf = g_new + h\nif better → update + push"]:::action
    A7 --> A3

    classDef term   fill:#d1e7dd,stroke:#198754,color:#000
    classDef action fill:#cfe2ff,stroke:#0d6efd,color:#000
    classDef dec    fill:#fff3cd,stroke:#ffc107,color:#000
    classDef bad    fill:#f8d7da,stroke:#dc3545,color:#000
```

---

## Mathematics Behind the A\* Heuristic

### Core Formula

A\* evaluates each node using:

```
f(n) = g(n) + h(n)
```

| Symbol | Meaning |
|--------|---------|
| `g(n)` | Actual travel time from the source to node *n* |
| `h(n)` | Estimated remaining travel time from *n* to the destination |
| `f(n)` | Estimated total cost of the cheapest path through *n* |

The priority queue always expands the node with the lowest `f(n)`, steering the search towards the goal.

### Heuristic Function

```
h(n) = haversine_distance(n, dest) / max_speed
```

- **haversine_distance** = great-circle (straight-line on Earth) distance between *n* and the destination
- **max_speed** = maximum speed limit on any road (60 km/h in our map)

This gives the *minimum possible travel time* by assuming a straight-line path at the fastest speed.

### The Haversine Formula

Calculates the great-circle distance between two points on Earth given their latitudes (φ) and longitudes (λ):

```
a = sin²(Δφ/2) + cos(φ₁) · cos(φ₂) · sin²(Δλ/2)
c = 2 · atan2(√a, √(1 − a))
d = R · c
```

| Variable | Meaning |
|----------|---------|
| φ₁, φ₂ | Latitudes of the two points (in radians) |
| Δφ | φ₂ − φ₁ (latitude difference) |
| Δλ | λ₂ − λ₁ (longitude difference) |
| R | Earth's mean radius = 6371 km |
| d | Great-circle distance in km |

### Why This Heuristic Is Admissible (Never Overestimates)

A heuristic is **admissible** if it never overestimates the true remaining cost. Ours satisfies this because:

1. **Haversine distance ≤ actual road distance** — the straight-line distance is always shorter than or equal to any path along roads
2. **Dividing by the maximum speed** — assumes the fastest possible travel at every point, so estimated time ≤ actual travel time

Since `h(n) ≤ h*(n)` (true optimal cost), A\* is guaranteed to find the optimal solution.

### Dijkstra vs A\*: When Do They Differ?

| Aspect | Dijkstra | A\* |
|--------|----------|-----|
| **Optimizes for** | Shortest distance | Fastest time |
| **Edge weight used** | `distance` (km) | `distance / speed` (hours) |
| **Heuristic** | None (h = 0) | Haversine / max speed |
| **Nodes explored** | All reachable within optimal distance | Fewer — heuristic prunes unpromising directions |

In our city map, roads have varying speed limits (12–100 km/h). Dijkstra may prefer a short alley (low distance, low speed), while A\* prefers a longer highway (high distance, high speed) because it minimizes *time*. This causes the **42% path divergence** measured in benchmarks above.

---

## POI System — K-Nearest Search

### How It Works

Each Point of Interest belongs to a **category** (HOTEL, RESTAURANT, MALL, THEATRE). POIs are indexed in **per-category POIQuadTrees** for efficient spatial queries.

### K-Nearest Algorithm (Max-Heap)

The search maintains a max-heap of size K (farthest of the K at the top). Each heap entry stores **(distance, POI)** together — no secondary list needed:

```
findKNearest(lat, lon, k):
    maxHeap = PriorityQueue<(dist, POI)>  // farthest-first; entry carries the POI

    for each POI p in this leaf:
        dist = euclidean(lat, lon, p.lat, p.lon)
        if heap.size < k:
            heap.add((dist, p))
        else if dist < heap.peek().dist:
            heap.poll()         // removes farthest POI and its distance atomically — O(log K)
            heap.add((dist, p))

    if divided:
        // PRUNE: skip quadrants whose closest possible point
        // is farther than current K-th best
        recurse into children
```

The pruning step is what makes this `O(K log N)` instead of `O(N)`. Each eviction is `O(log K)` — the POI rides in the heap entry, so `poll()` removes both atomically with no secondary scan.

### Distance Display

POI distances shown to the user use the **Haversine formula** (real km), while QuadTree search uses **Euclidean distance** on lat/lon (faster, same ordering for nearby points).

---

## QuadTree Spatial Partitioning

How the 14-node city is divided into quadrants for nearest-node lookup.

```mermaid
flowchart LR
    ROOT["QuadTree Root\nlat 12.84–13.21 · lon 77.56–77.80\n14 nodes total"]:::root

    subgraph QUADS["Quadrants"]
        direction TB
        NW["NW · high-lat / low-lon\nOLD · NTH · CST · GDN"]:::nw
        NE["NE · high-lat / high-lon\nAIR · HLV · MKT"]:::ne
        SW["SW · low-lat  / low-lon\nUNI · STN · CTR"]:::sw
        SE["SE · low-lat  / high-lon\nLKS · IND · THB · PRT"]:::se
    end

    NOTE["Branch-and-bound pruning\nfindNearest() skips any quadrant\nwhose bounding box is farther\nthan current best candidate"]:::note

    ROOT --> NW
    ROOT --> NE
    ROOT --> SW
    ROOT --> SE
    QUADS -.-> NOTE

    classDef root fill:#dbeafe,stroke:#3b82f6,color:#000
    classDef nw   fill:#d1fae5,stroke:#10b981,color:#000
    classDef ne   fill:#fef3c7,stroke:#f59e0b,color:#000
    classDef sw   fill:#ede9fe,stroke:#8b5cf6,color:#000
    classDef se   fill:#fce7f3,stroke:#ec4899,color:#000
    classDef note fill:#f3f4f6,stroke:#64748b,color:#374151

    style QUADS fill:#f8fafc,stroke:#94a3b8
```

---

## Demo City Map

The default seed file (`seeds/default_city.txt`) loads **14 locations**, **22 bidirectional roads**, and **33 points of interest** modelling a fictional city:

| Road Type | Speed Range | Example |
|-----------|:-----------:|---------|
| Alleys / Lanes | 12–20 km/h | Narrow Alley, Dirt Road |
| City Roads | 25–40 km/h | Boulevard, Ring Road |
| Highways | 50–60 km/h | Coastal Highway |
| Expressways | 70–100 km/h | Airport Expressway |

### Preloaded POIs

| Category | Count | Rating Range |
|----------|:-----:|:------------:|
| Hotels | 12 | 2.8 – 4.9 |
| Restaurants | 10 | 3.0 – 4.7 |
| Malls | 6 | 3.5 – 4.5 |
| Theatres | 5 | 3.6 – 4.7 |
