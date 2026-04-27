# Empirical Benchmark Results

Cairo transportation network: |V| = 25, |E| = 28.

All numbers below are *medians* over 1000 runs.

## 1) Single-Source Shortest Paths (source = 3, Downtown Cairo)

| Algorithm | Median time (μs) | Edge relaxations | PQ extractions |
|---|---:|---:|---:|
| Dijkstra (array) | 26.90 | 28 | 19 |
| Dijkstra (binary heap) | 10.60 | 28 | 19 |
| Dijkstra (transport) | 19.20 | 28 | 19 |
| Bellman-Ford | 17.50 | 224 | n/a |

## 2) Point-to-Point: source = 13 (New Admin. Capital), target = 7 (6th October)

| Algorithm | Median time (μs) | Edge relaxations | PQ extractions |
|---|---:|---:|---:|
| Dijkstra (binary heap, full SSSP) | 8.00 | 28 | 19 |
| Dijkstra (transport, early exit) | 8.10 | 25 | 17 |
| A* (haversine heuristic) | 7.80 | 33 | 11 |

## 3) All-Pairs Shortest Paths

| Algorithm | Median time (μs) | Operations |
|---|---:|---:|
| Dijkstra-heap × V | 107.90 | 532 relax, 367 pops |
| Floyd-Warshall | 30.90 | 7450 updates |

## 4) Effect of Weight Function on Optimal Route (3 → 13)

| Weight function | Distance label of target | Hops | Path |
|---|---:|---:|---|
| distance (km) | 61.90 | 4 | 3 → 2 → 4 → 14 → 13 |
| free-flow time (min) | 65.62 | 4 | 3 → 2 → 4 → 14 → 13 |
| BPR @ v/c=0.8 (min) | 69.65 | 4 | 3 → 2 → 4 → 14 → 13 |

