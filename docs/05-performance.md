[← Back to README](../README.md)

# Performance

## Benchmarks

> The interactive app ships with a 14-node demo city. To validate the asymptotic complexity claims at scale, `BenchmarkRunner` generates a synthetic **100,000-node / ~400,000-edge** connected city (avg degree ~8) plus 10,000 POIs, and measures with `System.nanoTime()` after JIT warm-up. Numbers below are from one such run.

### Routing Algorithm Execution (n0 → n99999, 20 runs)

| Algorithm | Avg |
|-----------|:---:|
| Dijkstra | 145.8 ms |
| A\* Search | 46.0 ms |

On a 100k-node graph A\* runs ~**3× faster** than Dijkstra — its admissible Haversine heuristic prunes the search frontier, so it expands far fewer nodes to reach the goal.

### Path Divergence — 2,000 Source–Destination Pairs

Dijkstra optimises for **shortest distance** (km); A\* optimises for **fastest time** (hours). On roads with varying speed limits they produce different routes.

| Metric | Value |
|--------|-------|
| Pairs analysed | 2,000 |
| Pairs where paths differ | **56.9%** |
| Average time saved by A\* | **9.28 min / trip** |
| Maximum time saved by A\* | **67.38 min** |

> A\* trades a longer distance for less time — it routes via faster roads even when that means more kilometres.

### QuadTree vs Linear Scan (2,000 random probes)

| Method | Avg per probe | Complexity |
|--------|:-------------:|:----------:|
| Linear scan | 794,803 ns | O(N) |
| QuadTree | 66,557 ns | O(log N) |

At N = 100,000 nodes the QuadTree is **11.9× faster** than a linear scan. This is the payoff that's invisible at 14 nodes (where tree-traversal overhead exceeds scanning 14 items) but dominates at city scale.

### POI K-Nearest Search (k = 3, 66,668 queries)

| Metric | Value |
|--------|-------|
| Avg latency | 12.2 µs / query |

The per-category spatial index stays fast even across 10,000 indexed POIs.

### Multi-Stop Route Scaling

Each additional stop adds one routing leg, confirming the theoretical **O(k · (V+E) log V)** complexity.

| Stops | Avg Time | Ratio vs 2-stop |
|:-----:|:--------:|:---------------:|
| 2 | 146.4 ms | 1.00× (baseline) |
| 3 | 258.3 ms | 1.76× |
| 4 | 422.4 ms | 2.88× |
| 5 | 580.6 ms | 3.97× |

Time grows roughly linearly with the number of stops, as expected.

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
