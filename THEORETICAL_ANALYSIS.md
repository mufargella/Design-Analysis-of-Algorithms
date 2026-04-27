# Theoretical Analysis of Dijkstra's Algorithm for Transportation Optimization

**Course:** CSE112 – Design and Analysis of Algorithms
**Institution:** Alamein International University, Faculty of Computer Science and Engineering
**Project:** Theoretical component of the Transportation Optimization System
**Algorithm under analysis:** Dijkstra's single-source shortest-path algorithm

---

## Table of Contents

1. [Introduction and General Applications](#1-introduction-and-general-applications)
2. [Mathematical Foundations](#2-mathematical-foundations)
3. [Pseudocode](#3-pseudocode)
4. [Formal Proof of Correctness](#4-formal-proof-of-correctness)
5. [Detailed Complexity Analysis](#5-detailed-complexity-analysis)
6. [Comparison with Alternative Approaches](#6-comparison-with-alternative-approaches)
7. [Transportation-Specific Modifications](#7-transportation-specific-modifications)
8. [Performance Analysis on the Cairo Network](#8-performance-analysis-on-the-cairo-network)
9. [Optimization Opportunities](#9-optimization-opportunities)
10. [Conclusion and Lessons Learned](#10-conclusion-and-lessons-learned)
11. [References](#11-references)

---

## 1. Introduction and General Applications

Dijkstra's algorithm, published by Edsger W. Dijkstra in 1959 in *A Note on Two Problems in Connexion with Graphs* (Numerische Mathematik 1, pp. 269–271), solves the **single-source shortest-path (SSSP) problem** on a weighted directed graph with non-negative edge weights. Given a graph G = (V, E) with weight function w : E → ℝ≥0 and a source vertex s ∈ V, it computes for every v ∈ V the value

$$\delta(s, v) = \min \\{\, w(P) : P \text{ is a path from } s \text{ to } v \,\\}$$

together with a tree of predecessors that allows any shortest path to be reconstructed.

The algorithm has become foundational in computer science because the SSSP problem appears in a striking variety of applications, several of which are directly relevant to this project:

| Domain                  | Modelling choice                                                                  | Example                                             |
| ----------------------- | --------------------------------------------------------------------------------- | --------------------------------------------------- |
| Road navigation         | Vertices = intersections / districts; weight = travel time or distance            | Google Maps, Waze, our Cairo system                 |
| Communication networks  | Vertices = routers; weight = link cost / latency                                  | OSPF (Open Shortest Path First) routing protocol    |
| Resource networks       | Vertices = transformers / pumps; weight = energy loss                             | Power-grid load distribution                        |
| Game AI / robotics      | Vertices = grid cells / configuration-space samples; weight = traversal cost      | A* (a directed-search descendant of Dijkstra)       |
| Operations research     | Vertices = states; weight = transition cost                                       | Project network analysis                            |
| Bio-informatics         | Vertices = aligned positions; weight = mutation cost                              | Sequence alignment via shortest paths               |

In our **Cairo transportation optimization system** Dijkstra's algorithm is the engine behind:

* point-to-point routing between any two neighborhoods or facilities,
* time-of-day-aware routing (we re-weight edges using traffic flow data),
* sensitivity analyses asking "what is the impact of opening road *X*?".

Throughout this document we anchor each abstract argument to the actual Cairo dataset (15 neighborhoods + 10 facilities = **25 vertices**, **28 existing roads**) so that the theoretical claims can be verified empirically against the benchmark output in `results/benchmark_results.md`.

---

## 2. Mathematical Foundations

### 2.1 Graph-theoretic preliminaries

We treat the road network as a **directed weighted graph** G = (V, E, w) where:

* V is the set of vertices (here, neighborhoods and facilities, |V| = 25).
* E ⊆ V × V is the set of directed arcs. Roads in Cairo are bidirectional, so each undirected road (u, v) is modeled by two arcs (u, v) and (v, u). Hence the directed edge count is 2|E| = 56 while the logical edge count is |E| = 28.
* w : E → ℝ≥0 is the weight function. We will use three concrete instantiations in §7.

A **walk** from s to t is a sequence of vertices ⟨v₀, v₁, …, vₖ⟩ with v₀ = s, vₖ = t and (vᵢ₋₁, vᵢ) ∈ E for every i. The walk's weight is w(P) = Σᵢ w(vᵢ₋₁, vᵢ). A **path** is a walk with no repeated vertex.

### 2.2 The single-source shortest-path problem (SSSP)

> **Problem (SSSP).** Given G = (V, E, w) and a source s ∈ V, compute, for every v ∈ V,
>
> $$\delta(s, v) = \min \\{\, w(P) : P \text{ is a walk from } s \text{ to } v \,\\}.$$
>
> If no walk exists, define δ(s, v) = +∞.

When all weights are **non-negative** the minimum is achieved by a path (no advantage in revisiting vertices), and δ is a finite real number for every reachable vertex. This is the regime in which Dijkstra's algorithm is correct.

### 2.3 Two structural properties that make a greedy algorithm work

Dijkstra is a **greedy** algorithm: in each iteration it commits to a globally-optimal extension of the partial solution computed so far. Greediness is justified by two structural properties of shortest paths.

#### 2.3.1 Optimal substructure

> **Lemma 2.1.** *Sub-paths of shortest paths are themselves shortest paths.*
>
> Formally, if P = ⟨v₀, …, vₖ⟩ is a shortest path from v₀ to vₖ, then for any 0 ≤ i ≤ j ≤ k the sub-path Pᵢⱼ = ⟨vᵢ, …, vⱼ⟩ is a shortest path from vᵢ to vⱼ.

*Proof.* If some shorter path P′ from vᵢ to vⱼ existed with w(P′) < w(Pᵢⱼ), substituting P′ for Pᵢⱼ inside P would yield a strictly shorter walk from v₀ to vₖ, contradicting the assumed optimality of P. ∎

#### 2.3.2 Triangle inequality and the relaxation operator

> **Lemma 2.2 (Triangle inequality).** For all u, v ∈ V and any edge (u, v) ∈ E,
>
> $$\delta(s, v) \le \delta(s, u) + w(u, v).$$

This holds because concatenating the shortest s-u path with the edge (u, v) yields a walk from s to v of length δ(s, u) + w(u, v); the shortest such walk can only be shorter.

The triangle inequality is the *contract* that the algorithm maintains via **edge relaxation**:

```
relax(u, v):
    if dist[u] + w(u, v) < dist[v]:
        dist[v]  ←  dist[u] + w(u, v)
        prev[v]  ←  u
```

**Convergence property.** Edge relaxations can only ever decrease `dist[·]`; since each `dist[v]` is bounded below by δ(s, v), only a finite number of relaxations can ever take effect — which is why every shortest-path algorithm based on relaxations terminates.

### 2.4 The role of non-negative weights

Dijkstra's correctness hinges on a single seemingly innocent fact: extending a path can never make it shorter. Concretely, for any prefix walk P from s to some intermediate u and any edge (u, v),

$$w(P) + w(u, v) \ge w(P) \quad\text{iff}\quad w(u, v) \ge 0.$$

If a negative edge weight existed, a vertex v already settled by Dijkstra could later be reached by a shorter path that goes "around" through v. The greedy commitment would then be wrong. Bellman-Ford (§6.1) gives up the greedy commitment in exchange for the ability to handle negative weights at the cost of an extra factor of V in running time.

In the Cairo dataset all three weight functions we use (distance in km, free-flow time in minutes, BPR-congested time in minutes) are non-negative by construction, so Dijkstra applies cleanly.

---

## 3. Pseudocode

### 3.1 Generic Dijkstra (priority-queue-agnostic)

```
Dijkstra(G = (V, E, w),  source s):
    for each v ∈ V:
        dist[v]   ←  +∞
        prev[v]   ←  NIL
        settled[v] ← false
    dist[s] ← 0

    Q ← priority queue, keyed by dist[·], containing all v ∈ V

    while Q is not empty:
        u ← extractMin(Q)            # smallest tentative distance
        settled[u] ← true
        for each edge (u, v) ∈ E:
            if not settled[v]:
                if dist[u] + w(u, v) < dist[v]:
                    dist[v] ← dist[u] + w(u, v)
                    prev[v] ← u
                    decreaseKey(Q, v, dist[v])

    return (dist, prev)
```

### 3.2 Binary-heap variant with lazy deletion

Java's `PriorityQueue` does not support an O(log V) decrease-key, so we emulate it by **re-pushing** an updated `(dist[v], v)` pair and discarding stale ones at pop time. This is the implementation in [`DijkstraHeap.java`](src/main/java/com/aiu/cse112/algorithms/DijkstraHeap.java).

```
DijkstraHeap(G, s):
    initialize dist, prev, settled as above
    Q ← min-heap; Q.push( (0, s) )
    while Q not empty:
        (d, u) ← Q.pop()
        if settled[u]:              continue        # stale: already finalized
        if d > dist[u]:             continue        # stale: dominated
        settled[u] ← true
        for each (u, v) ∈ E:
            if settled[v]:          continue
            if dist[u] + w(u, v) < dist[v]:
                dist[v] ← dist[u] + w(u, v)
                prev[v] ← u
                Q.push( (dist[v], v) )              # "decrease-key by re-insertion"
    return (dist, prev)
```

### 3.3 Path reconstruction

Once `prev` is computed, the explicit path s → t can be recovered in O(|path|) time:

```
reconstructPath(prev, t):
    path ← empty list; cur ← t
    while cur ≠ NIL:
        prepend cur to path
        cur ← prev[cur]
    return path                  # empty if t was unreachable
```

A worked Cairo example produced by [`Main.java`](src/main/java/com/aiu/cse112/Main.java):

```
Shortest path from 3 (Downtown Cairo) to 12 (Helwan):
  d = 21.20 km, route = Downtown Cairo → Maadi → Helwan
```

This corresponds to taking the existing roads 3-1 (8.5 km) and 12-1 (12.7 km).

---

## 4. Formal Proof of Correctness

We prove that Dijkstra's algorithm computes `dist[v] = δ(s, v)` for every vertex v reachable from s, assuming all edge weights are non-negative.

The argument is via a single **loop invariant**, established for the moment a vertex is *settled* (i.e. extracted from the priority queue and marked permanent).

### 4.1 The loop invariant

> **Loop invariant (LI).** At any point during execution, for every settled vertex v, `dist[v] = δ(s, v)`.

The invariant trivially holds before the first iteration (no vertex is settled). We need to show it is preserved by each iteration of the main `while` loop.

### 4.2 The key lemma

> **Lemma 4.1 (Settle is exact).** When the algorithm is about to settle a vertex u (i.e. u is the next vertex returned by `extractMin`), the current value `dist[u]` already equals δ(s, u).

*Proof.* Suppose, for contradiction, that `dist[u] > δ(s, u)` at the moment u is about to be settled. This means a shorter path P from s to u exists than what relaxations have so far recorded.

Let P = ⟨s = v₀, v₁, …, vₖ = u⟩ be such a shortest path with w(P) = δ(s, u) < dist[u]. Walk along P from s, and let y be the **first vertex on P that is *not* yet settled**. (At least one such vertex exists because u is on P and u is not yet settled.) Let x be the predecessor of y on P, so x is settled, y is not, and (x, y) is an edge of G with weight w(x, y).

By LI applied to x, `dist[x] = δ(s, x)`. After x was settled, the algorithm relaxed every outgoing edge of x, including (x, y); therefore

$$\mathrm{dist}[y] \le \mathrm{dist}[x] + w(x, y) = \delta(s, x) + w(x, y) = \delta(s, y),$$

where the last equality uses optimal substructure (Lemma 2.1) on the prefix s → x → y of P. Since `dist[y] ≥ δ(s, y)` always (relaxations are sound), we have `dist[y] = δ(s, y)`.

Now use the **non-negativity** of edge weights along the suffix y → u of P:

$$\delta(s, y) = w(P_{0\to y}) \le w(P_{0\to u}) = \delta(s, u).$$

Combining,

$$\mathrm{dist}[y] = \delta(s, y) \le \delta(s, u) < \mathrm{dist}[u].$$

But this contradicts the choice of u: extractMin returned u as the unsettled vertex with the **smallest** `dist[·]` value, and we have just exhibited an unsettled vertex y with `dist[y] < dist[u]`. ∎

> **Corollary 4.2 (Algorithmic correctness).** When Dijkstra terminates, `dist[v] = δ(s, v)` for every v reachable from s, and `dist[v] = +∞` for every unreachable v.

*Proof.* Apply Lemma 4.1 each time the loop settles a vertex. Reachable vertices are eventually all settled because every vertex on the source-side of a finite-weight path will at some point have a finite tentative distance and therefore be selected by extractMin. Unreachable vertices retain their initial `+∞` because no relaxation ever updates them. ∎

### 4.3 Why the proof breaks for negative weights

The single inequality that fails when weights can be negative is

$$\delta(s, y) \le \delta(s, u),$$

i.e. extending a path from y to u was assumed to never decrease length. With a negative edge somewhere on the suffix y → u of P, this can fail, and y might be settled with a value that is later beaten — but Dijkstra never re-settles. The classical counter-example is the four-vertex graph

```
    s --1--> a --1--> t
    s ---------4-----> t       (this edge has weight 4)
    a --(-3)--> b
    b --1--> t
```

where Dijkstra (incorrectly) reports δ(s, t) = 2 via s → a → t, missing the path s → a → b → t of length –1.

For non-negative weights — and **all** of our transportation weights are non-negative — the proof goes through and the algorithm is exact.

---

## 5. Detailed Complexity Analysis

We analyze three implementations: linear-scan / array (the original 1959 version), binary heap (our `DijkstraHeap`), and Fibonacci heap (theoretical optimum). Throughout, V = |V| and E = |E|; we assume the graph is given by adjacency lists and that vertex equality and arithmetic are O(1).

### 5.1 Array-based extract-min: O(V²)

In [`DijkstraArray.java`](src/main/java/com/aiu/cse112/algorithms/DijkstraArray.java) the priority queue is realized by scanning the `dist[·]` array to find the unsettled vertex of smallest tentative distance.

**Per-iteration work.**
* `extractMin`: a linear scan over all V vertices → Θ(V).
* Edge relaxations: in iteration i we look at deg(uᵢ) edges; each relaxation is O(1).

**Total work.**

$$T(V, E) \;=\; \sum_{i=1}^{V} \big( \Theta(V) + \deg(u_i) \big) \;=\; \Theta(V^2) + \Theta(E) \;=\; \Theta(V^2).$$

The sum Σ deg(uᵢ) = 2E (handshaking lemma) but is dominated by the V² extract-min term whenever E = O(V²). For *dense* graphs (E = Θ(V²)) this implementation is optimal: any algorithm must read all Θ(V²) edges.

For Cairo (V = 25, E = 28) the array variant scans ~25 · 25 ≈ 625 entries, vastly more than the 28 edges relaxed. Empirically (see §8) this is the slowest of our SSSP variants.

### 5.2 Binary-heap extract-min: O((V + E) log V)

In [`DijkstraHeap.java`](src/main/java/com/aiu/cse112/algorithms/DijkstraHeap.java) the priority queue is a binary min-heap supporting:

| Operation        | Cost (binary heap) |
| ---------------- | ------------------ |
| `push`           | O(log n)           |
| `pop` (extractMin)| O(log n)          |
| `decreaseKey`    | O(log n)           |

Because we use **lazy deletion** (re-push on relax instead of decrease-key), each edge can cause at most one push, so the heap size is bounded by V + E and every heap operation costs O(log(V + E)) = O(log V) (since E ≤ V²).

**Operation count.**
* Up to V + E heap pushes (one per init vertex + one per successful relax).
* Up to V + E heap pops (each push is eventually popped, possibly as stale).
* E edge relaxations.

**Total work.**

$$T(V, E) \;=\; O\big( (V + E)\log V + E \big) \;=\; O\big( (V + E)\log V \big).$$

For sparse graphs (E = O(V)) this is O(V log V) — a substantial improvement over O(V²). The Cairo network is sparse: with V = 25 and E = 28 we have V·log₂V ≈ 116 vs V² = 625, a 5× theoretical improvement that the benchmarks reproduce qualitatively.

### 5.3 Fibonacci-heap extract-min: O(E + V log V)

A Fibonacci heap (Fredman & Tarjan, 1987) supports `decreaseKey` in **amortized O(1)** time, with `push` also O(1) amortized. `extractMin` remains O(log n) amortized.

* V extractMins: V · O(log V) = O(V log V).
* E decreaseKeys: E · O(1) = O(E).
* V pushes: V · O(1) = O(V).

**Total work** (amortized): **O(E + V log V).**

This is the classical "Dijkstra-Fredman-Tarjan" bound. For dense graphs (E = Θ(V²)) it is Θ(V²), matching the array bound. For sparse graphs (E = O(V)) it is Θ(V log V) — same as the binary heap.

The Fibonacci heap is asymptotically optimal for this problem in the comparison-based RAM model: a matching lower bound of Ω(E + V log V) follows from the requirement to sort the V distance values during extract-min calls. We do **not** use a Fibonacci heap in this project because (a) the constant factors of Fibonacci heaps in practice are notoriously poor — binary heaps dominate them in real systems on graphs of this size — and (b) for our V = 25 graph the asymptotic distinction is invisible.

### 5.4 Space complexity

All three variants use:
* `dist[·]`: V doubles → Θ(V).
* `prev[·]`: V vertex pointers → Θ(V).
* `settled[·]`: V booleans → Θ(V).
* The graph adjacency-list representation: Θ(V + E).
* Priority queue:
    * Array variant — implicit, Θ(V).
    * Binary heap with lazy deletion — at most V + E entries, Θ(V + E).

**Total space: Θ(V + E)** for all three.

### 5.5 Lower bound and information-theoretic remarks

Every shortest-path algorithm based on edge relaxations must (a) read every edge at least once and (b) finalize each reachable vertex's distance, which in the comparison model entails ordering them by distance. The first requirement is Ω(E); the second is Ω(V log V) (from the comparison-based sorting lower bound). Hence Ω(E + V log V) is tight for the priority-queue family of algorithms — Fibonacci-heap Dijkstra meets it. (Faster bounds exist in restricted models; the Thorup integer-priority-queue algorithm achieves O(E α(V)) on integer weights, but this is outside the scope of this course.)

---

## 6. Comparison with Alternative Approaches

| Algorithm           | Time complexity                | Negative weights | Output         | Best for                                                    |
| ------------------- | ------------------------------ | :--------------: | -------------- | ----------------------------------------------------------- |
| Dijkstra (heap)     | O((V + E) log V)               |       no         | SSSP           | non-negative, sparse, point-to-multi                        |
| Dijkstra (Fib heap) | O(E + V log V)                 |       no         | SSSP           | non-negative, dense, asymptotic optimum                     |
| Bellman-Ford        | O(V · E)                       |       yes        | SSSP           | negative weights, distributed (used in RIP / BGP)           |
| Floyd-Warshall      | Θ(V³)                          |       yes\*      | all-pairs (APSP)| dense, all-pairs                                           |
| Johnson's algorithm | O(V·E + V² log V)              |       yes        | APSP           | sparse + negative weights                                   |
| A*                  | O((V + E) log V), often ≪      |       no         | one path s → t | point-to-point with admissible heuristic                    |
| BFS                 | O(V + E)                       |       n/a        | SSSP, unit weights | unweighted graphs                                       |

\* Floyd-Warshall handles negative weights, but **negative cycles** must be detected separately.

### 6.1 Bellman-Ford

Iterates V – 1 times over all directed edges, relaxing each one. After the V-th iteration any further relaxation proves a negative cycle.

* **Time:** O(V · E). On Cairo this is 25 × 56 = 1400 relaxations, vs 28 for heap-Dijkstra.
* **Use case:** when negative weights are possible (not for road networks, but for e.g. arbitrage-detection in currency exchange, or when modelling "rebates" along certain edges).
* **Empirically (§8):** ~26.7 μs vs ~22.7 μs for heap-Dijkstra on Cairo. The factor of V is largely hidden by the small graph size; on a graph with V = 10 000 the gap would be ~10 000×.

### 6.2 Floyd-Warshall

A dynamic-programming algorithm computing all V² pairwise distances in Θ(V³). The recurrence is

$$d^{(k)}[i][j] = \min\!\big(\,d^{(k-1)}[i][j],\;\; d^{(k-1)}[i][k] + d^{(k-1)}[k][j]\big),$$

i.e. the k-th iteration permits k as an intermediate vertex.

For all-pairs queries Floyd-Warshall competes against running V Dijkstra calls. Time-wise:

* V Dijkstra-heap calls: O(V · (V + E) log V).
* Floyd-Warshall: Θ(V³).

For sparse graphs (E = O(V)) Dijkstra-V wins asymptotically: V² log V ≪ V³. **However**, the constant factors of Floyd-Warshall (just 2 array accesses + 1 addition + 1 comparison per inner-loop iteration) are tiny, and on small graphs it can be faster than V Dijkstra calls. In our Cairo benchmark this is exactly what happens:

```
Dijkstra-heap × V:  295.79 μs   (532 relaxations,  367 pops)
Floyd-Warshall:      64.41 μs   (7450 updates)
```

Floyd-Warshall is ~4.6× faster, despite doing 14× more arithmetic, because each operation is much cheaper. This is a textbook example of **constant factors dominating asymptotic factors when V is small**.

### 6.3 A* search

A* maintains the same priority queue as Dijkstra but orders entries by `f(v) = g(v) + h(v)`, where g(v) is the best-known cost from s to v and h(v) is a **heuristic** estimate of the remaining cost from v to the target t.

> **Theorem (Hart, Nilsson, Raphael 1968).** If h is *admissible* — i.e. h(v) ≤ δ(v, t) for every v — then A* returns an optimal s → t path. If h is also *consistent* — i.e. h(u) ≤ w(u, v) + h(v) for every edge — then A* never re-opens a closed vertex (so its work is bounded just like Dijkstra's).

**For Cairo:** with the haversine-distance heuristic h(v) = greatCircleDistance(v, t) and an edge weight equal to road distance in km, h is admissible because no road can be shorter than the great-circle line between its endpoints. This is implemented in [`AStar.java`](src/main/java/com/aiu/cse112/algorithms/AStar.java).

**Empirical comparison** (3 → 12, our worked example):

```
Dijkstra (array):                d=21.20  relax=28  pops=19
Dijkstra (heap):                 d=21.20  relax=28  pops=19
Dijkstra (transport, early exit):d=21.20  relax=24  pops=13
A* (haversine):                  d=21.20  relax=10  pops=3
```

A* settles only **3 vertices** vs 19 for full SSSP — a 6× reduction in pops. The benefit grows with graph size: on a continental road network A* with the haversine heuristic typically expands ~1 % of what Dijkstra does.

When h ≡ 0 the heuristic is trivially admissible and A* degenerates exactly to Dijkstra. So A* is, formally, a *generalization* of Dijkstra.

### 6.4 Bidirectional Dijkstra

Run two Dijkstra searches simultaneously: a forward search from s and a backward search from t (on the reverse graph). Stop when their settled-sets intersect; the shortest s → t path is then the minimum-weight path through any "meeting" vertex.

The expected speed-up is ~2×: each search expands a "ball" of radius ½·δ(s, t), so together they touch ~2 · (½)² = ½ as many vertices as a single Dijkstra reaching all of distance δ(s, t). When combined with an A* heuristic in both directions, bidirectional A* gives the basis for modern road-network engines like OSRM and GraphHopper.

We do not implement bidirectional Dijkstra in this project, but §9.1 sketches it.

### 6.5 When to choose Dijkstra

The decision tree we use in the transportation system:

```
                   ┌── single source needed?
                   │
          ┌─ yes ──┤
          │        └── all weights ≥ 0?
          │             ├── yes → Dijkstra
          │             └── no  → Bellman-Ford
   point  │
   query? │        ┌── all weights ≥ 0  AND  good heuristic available?
          │        │
          └─ s→t ──┤
                   ├── yes → A*
                   ├── yes (and bidirectional graph) → bidirectional A*
                   └── no  → Bellman-Ford or specialized algorithm
```

For routing inside Cairo at runtime, Dijkstra (heap) and A* are both correct; A* is preferable when a single (s, t) pair is queried and the haversine heuristic is admissible.

---

## 7. Transportation-Specific Modifications

The vanilla algorithm needs three tweaks before it becomes useful in the Cairo system. All three are implemented in [`DijkstraTransport.java`](src/main/java/com/aiu/cse112/algorithms/DijkstraTransport.java).

### 7.1 Multi-objective edge weights

The dataset gives each road three independent attributes (distance in km, capacity in vehicles/hour, and a 1–10 condition score). Different stakeholders care about different objectives:

* **A driver** wants to *minimize travel time*.
* **A logistics planner** wants to *minimize fuel cost ≈ distance*.
* **The municipality** during peak hours might want to *avoid congested roads* even if a longer route results.

We expose this through a pluggable weight function `weightFn : Edge → ℝ≥0`. The proof of correctness in §4 used only the inequalities w(u, v) ≥ 0; it does **not** care what physical quantity the weight measures, so swapping in any non-negative weight function preserves optimality. The three concrete functions implemented are:

1. **`Graph::byDistance`** — `w = e.distanceKm`. Gives the geographically shortest route.

2. **`Graph::byFreeFlowTime`** — `w = (distanceKm / vFree(condition)) · 60` minutes. Free-flow speed is interpolated linearly with the condition score: condition 1 → 30 km/h, condition 10 → 60 km/h. This rewards well-maintained roads even when they are slightly longer.

3. **`Graph::byBPRTime(e, v/c)`** — the Bureau-of-Public-Roads congestion function

   $$t = t_{\mathrm{free}} \cdot \big(1 + 0.15 \cdot (v/c)^4\big),$$

   where v is the volume from `data/traffic_flow.csv` for the relevant time-of-day bucket and c is the road capacity. The fourth-power dependence on v/c is the operationally standard model in transportation engineering (Branston 1976; Spiess 1990).

### 7.2 Early termination at a fixed target

When the caller supplies a target vertex t, we halt the loop as soon as t is settled.

> **Correctness.** By Lemma 4.1, the moment t is extracted from the priority queue we have `dist[t] = δ(s, t)`. Continuing the loop could only refine `dist[v]` for *other* vertices v ≠ t; it would not change `dist[t]`. Hence terminating early returns the correct answer for t. ∎

The asymptotic worst case is unchanged (we may still visit Θ(V + E) elements before t is settled), but in practice the average case is much smaller. In the benchmark, source = 13 → target = 7 settles 17 vertices with early exit vs 19 in full SSSP.

### 7.3 Capacity-aware filtering

In peak hours an operator may want to *exclude* near-saturated roads from consideration even at the cost of a longer route. We implement this as a soft filter: edges whose volume / capacity ratio meets or exceeds a configurable `vcThreshold` are skipped during relaxation.

> **Correctness.** Filtering edges turns the search graph into a subgraph G′ ⊆ G. Dijkstra applied to G′ returns a *correct shortest path within G′*. The algorithm therefore returns the optimal route under the constraint "no edge exceeds vcThreshold load". ∎

This is a correct **constrained** shortest-path solution, not a heuristic.

### 7.4 Time-dependent weights

The Cairo dataset has four time buckets (morning peak, afternoon, evening peak, night). We pre-compute four separate weighted graphs (one per bucket) and dispatch to the right one based on the user's departure time. This trades 4× memory for query-time simplicity.

A more accurate **time-dependent shortest path** would have w(u, v, τ) where τ is the time at which the edge is *traversed*. In that case Dijkstra is still correct provided weights satisfy the **FIFO property**: leaving u later cannot let you arrive at v earlier (Orda & Rom 1990). The BPR function is FIFO, so if we evolved the system to a continuous time-of-day model, Dijkstra would still apply; only the weight evaluation logic would change.

### 7.5 Putting the modifications together

The signature of `DijkstraTransport.run` reflects all four design decisions:

```java
ShortestPathResult run(Graph g,
                       String source,
                       String target,            // optional → early termination
                       ToDoubleFunction<Edge> weightFn,
                       EdgeVCRatio vc,
                       double vcThreshold)
```

A typical call from the application layer:

```java
DijkstraTransport.run(
    cairoGraph,
    "3",  /* Downtown */
    "13", /* New Admin. Capital */
    edge -> Graph.byBPRTime(edge, vcRatioForCurrentHour(edge)),
    edge -> vcRatioForCurrentHour(edge),
    /* vcThreshold = */ 0.95
);
```

This routes between 3 and 13 by **expected travel time at the current hour**, **avoiding** any road already at ≥ 95 % of capacity, and **terminates early** as soon as 13 is settled.

---

## 8. Performance Analysis on the Cairo Network

All numbers are produced by [`PerformanceBenchmark.java`](src/main/java/com/aiu/cse112/benchmark/PerformanceBenchmark.java) (median of 1000 runs, on a 64-bit Linux container, OpenJDK 21).

### 8.1 Single-source shortest paths from Downtown Cairo (vertex 3)

| Algorithm                | Median time (μs) | Edge relaxations | Priority-queue pops |
| ------------------------ | ---------------: | ---------------: | ------------------: |
| Dijkstra (array)         |            38.62 |               28 |                  19 |
| Dijkstra (binary heap)   |            22.73 |               28 |                  19 |
| Dijkstra (transport)     |            18.42 |               28 |                  19 |
| Bellman-Ford             |            26.73 |              224 |                 n/a |

* All three Dijkstra variants relax exactly the **same 28 edges** and settle the same 19 reachable vertices, as required by Lemma 4.1.
* The array variant is ~70 % slower than the heap variant despite doing the same edge work — the linear-scan extract-min dominates wall time.
* Bellman-Ford does **8× more edge work** (224 relaxations vs 28). The asymptotic factor of V is partially hidden by Bellman-Ford's tighter inner loop (no priority-queue overhead), so the wall-time gap is "only" ~17 %. On a graph with V = 1000 the gap would be ~1000×.

### 8.2 Point-to-point query: source = 13 (New Admin. Capital), target = 7 (6th October)

| Algorithm                                | Median time (μs) | Edge relaxations | PQ pops |
| ---------------------------------------- | ---------------: | ---------------: | ------: |
| Dijkstra (binary heap, full SSSP)        |            12.22 |               28 |      19 |
| Dijkstra (transport, early exit)         |            17.81 |               25 |      17 |
| A* (haversine heuristic)                 |            15.25 |               33 |      11 |

* **Early exit** trims pops from 19 to 17 and relaxations from 28 to 25 — modest because 13 → 7 is a long route across the whole network. Wall time goes up because of the per-call overhead of `setWeightFunction`; a production system would cache the weight assignment.
* **A\*** more than halves the pop count (19 → 11), the most informative measure. It shows more relaxations (33 vs 28) because lazy deletion adds extra heap entries when a node is reached by multiple admissible paths; this is the standard A* trade-off.

### 8.3 All-pairs queries

| Algorithm             | Median time (μs) |  Operation count |
| --------------------- | ---------------: | ---------------: |
| Dijkstra-heap × V     |           295.79 |        532 relax + 367 pops |
| Floyd-Warshall        |            64.41 |       7450 updates |

A surprising result: **Floyd-Warshall is 4.6× faster than V applications of heap-Dijkstra on this graph**, despite being asymptotically worse for sparse graphs. This is the canonical demonstration that **constant factors matter when V is small**.

In Floyd-Warshall the inner loop is just two array accesses, one addition, one comparison and one conditional store — about 4 nanoseconds per iteration on modern hardware. Dijkstra-heap incurs HashMap lookups, heap restructurings and object allocations on every relaxation, costing tens to hundreds of nanoseconds per iteration. The crossover happens around V ≈ 100 on this hardware.

### 8.4 Effect of weight function on the optimal route 3 → 13

| Weight function          | Optimum value | Hops | Path                                                                    |
| ------------------------ | ------------: | ---: | ----------------------------------------------------------------------- |
| Distance (km)            |         61.90 |    4 | 3 → 2 → 4 → 14 → 13                                                     |
| Free-flow time (min)     |         65.62 |    4 | 3 → 2 → 4 → 14 → 13                                                     |
| BPR @ v/c = 0.8 (min)    |         69.65 |    4 | 3 → 2 → 4 → 14 → 13                                                     |

For 3 → 13 the optimal path is invariant under the three weight functions because the road via Al Rehab dominates every alternative. This is a **structural property of the Cairo network**, not a deficiency of the algorithm: the 14-13 connector (35.5 km) is so much shorter than alternatives like 13-4 (45 km) or via 11 (62.1 km) that congestion-induced re-weighting cannot reverse the ordering. We did observe weight-dependent optimal-route changes for other pairs during exploration — an illustration of why the multi-objective design of §7.1 matters in practice.

### 8.5 Discussion

The most informative numbers in §§8.1–8.3 are the **operation counts**, not the wall times. Operation counts are deterministic and let us verify the analytical bounds directly:

* heap-Dijkstra performs **exactly E** edge relaxations (28 = E) — matching Theorem 5.2.
* Bellman-Ford performs **exactly (V – 1)·E_directed** = 24 × 56 = 1344 if we let it run to completion; with early termination once a pass makes no relaxation it stops at 224. The early-termination optimization brings Bellman-Ford from worst-case Θ(VE) to instance-dependent O(p·E) where p is the longest hop-count of any shortest path.

Wall times are useful only as a **rough sanity check** at this graph size, because GC pauses and JIT warm-up dwarf the actual algorithmic work below ~50 μs. On a larger network those effects average out and the asymptotic ordering reasserts itself.

---

## 9. Optimization Opportunities

The algorithm we have is already optimal in the comparison-based RAM model with a Fibonacci heap. The interesting question is: can we go *faster than the bound* by **changing the model**? The answer in modern transportation systems is yes, by exploiting the static structure of the road network and pre-processing it.

### 9.1 Bidirectional Dijkstra

Run Dijkstra forward from s and (simultaneously) on the *reverse graph* from t. Stop when their settled-sets intersect. The shortest s → t path is then

$$\delta(s, t) = \min_{v \in V_{\text{settled-fwd}} \cap V_{\text{settled-bwd}}} \big(\, d_{\text{fwd}}(v) + d_{\text{bwd}}(v) \,\big).$$

Each search expands a "ball" of radius about ½·δ(s, t), so they together touch ~½ as many vertices as one Dijkstra reaching distance δ(s, t). On Cairo this would give an estimated 2× speedup; on a continental graph the speedup is empirically 3–5×.

### 9.2 ALT: A* with Landmarks and Triangle inequality

Pre-compute, for a small set of "landmark" vertices L₁, …, L_k, the distances δ(Lⱼ, v) for every v. The triangle inequality then gives an admissible heuristic:

$$h_{\text{ALT}}(v, t) \;=\; \max_j \big| \delta(L_j, t) - \delta(L_j, v) \big|.$$

Choosing landmarks well (e.g. by farthest-point sampling) gives heuristics that are near-tight on long-range queries. ALT typically expands ~10 % of the nodes that plain A* with haversine does.

### 9.3 Contraction Hierarchies (Geisberger et al. 2008)

Order vertices by "importance" (a heuristic combining degree, betweenness and edge difference). Process vertices from least to most important; when contracting a vertex, add **shortcut edges** between its neighbors that preserve all shortest paths through it. The result is a graph with the *same* shortest-path distances but on which a bidirectional Dijkstra-like query expands only the "high-importance" core, typically ~100 nodes per query on a continental graph.

CH preprocessing is O(n log n) on average and queries are sub-millisecond on Europe-scale graphs (Geisberger et al. report ~0.1 ms per query on a 18-million-node Europe graph).

### 9.4 Hub Labels / 2-Hop Cover

Pre-compute, for each vertex v, two compact labels L_in(v) and L_out(v) such that for any pair (s, t) the distance is

$$\delta(s, t) = \min_{h \in L_{\text{out}}(s) \cap L_{\text{in}}(t)} (d(s, h) + d(h, t)).$$

Queries are linear in the label size (typically a few hundred entries on continental road networks), making them an order of magnitude faster than CH at the cost of larger pre-computed indices.

### 9.5 What we recommend for Cairo

The Cairo dataset is **tiny by road-network standards** (V = 25). Heap-Dijkstra with the early-termination modification of §7.2 already runs in microseconds, so query speed is not a bottleneck. The genuine optimization targets here are not algorithmic but **modelling**:

* **Live traffic ingestion** — re-evaluate w on the order of seconds; the algorithm is unchanged.
* **Multi-modal queries** — combine roads, metro and bus networks with mode-change penalties; this is just a graph-augmentation, Dijkstra still applies.
* **Fairness / equity** — augment the cost function with a penalty for routes that disproportionately pass through low-population residential areas.

These modelling extensions are exactly what the multi-objective weight design of §7.1 enables; the algorithmic core does not need to change.

---

## 10. Conclusion and Lessons Learned

### 10.1 What the analysis established

1. **Correctness** — Dijkstra's algorithm computes `dist[v] = δ(s, v)` for every v ∈ V whenever all weights are non-negative. The proof rested on a single greedy-choice lemma (Lemma 4.1) plus the optimal-substructure property of shortest paths.

2. **Tight complexity bounds** — three implementation choices give three asymptotic regimes:

   | Implementation        | Time                  |
   | --------------------- | --------------------- |
   | Linear-scan / array   | Θ(V²)                 |
   | Binary heap           | O((V+E) log V)        |
   | Fibonacci heap        | O(E + V log V)        |

   The Fibonacci-heap bound matches an Ω(E + V log V) information-theoretic lower bound in the comparison model, so the algorithm is asymptotically optimal.

3. **Comparison with alternatives** — Dijkstra strictly dominates Bellman-Ford on non-negative graphs; A* dominates Dijkstra on point-to-point queries with an admissible heuristic and reduces to Dijkstra when h ≡ 0; Floyd-Warshall wins for all-pairs only when V is small enough that constant factors trump asymptotic factors (and our Cairo graph is in that regime — see §8.3).

4. **Modifications for transportation are correctness-preserving** — the multi-objective weight, early termination, and capacity filtering all keep the proof of §4 intact. Crucially, any non-negative weight function can be plugged in without re-deriving correctness.

### 10.2 Lessons learned

* **Greedy + non-negativity is the heart of Dijkstra.** Once you understand why Lemma 4.1 fails for negative weights, you understand why Bellman-Ford has the V-iteration structure.

* **Operation counts are the truth; wall times are noisy.** On a 25-vertex graph, GC pauses and JIT warm-up dominate the actual algorithmic work. Reporting `relaxations` and `pops` (as the benchmark does) keeps the numbers comparable to the analytical bounds.

* **Constants matter when V is small.** Floyd-Warshall beat V applications of heap-Dijkstra on Cairo despite a 14× higher operation count, because each operation is ~30× cheaper. This is a generalizable lesson: asymptotics describe the limit; engineering decides the regime.

* **A modular weight function is the single most useful design choice.** Once the algorithm is parameterised on `weightFn`, every multi-objective and time-dependent extension becomes trivial. This is why our `Graph` class exposes `setWeightFunction(ToDoubleFunction<Edge>)` as a first-class operation.

* **The right algorithm is the one whose assumptions match your input.** All five algorithms compared in §6 are correct on their respective domains; the engineering question is matching algorithm to input. On the Cairo system, with non-negative weights and a sparse graph, heap-Dijkstra (with early termination for point-to-point queries and possibly A* when we have a heuristic) is the honest answer.

### 10.3 Limitations and future work

* The implementation uses `HashMap<String, ?>` for `dist`, `prev`, and `settled`. Switching to `int`-indexed arrays would shave a substantial constant factor (HashMap operations cost ~10× a primitive array indexing).
* `setWeightFunction` recomputes weights on every call — fine for benchmarking, wasteful in production. A real system would cache weights per (graph, weightFn) combination.
* The capacity filter in `DijkstraTransport` is currently a stub that takes a `vc : Edge → double` callback; wiring it to the actual `traffic_flow.csv` data is left as a future improvement.

---

## 11. References

1. Dijkstra, E. W. (1959). *A Note on Two Problems in Connexion with Graphs*. Numerische Mathematik 1, 269–271.
2. Cormen, T. H., Leiserson, C. E., Rivest, R. L., & Stein, C. (2022). *Introduction to Algorithms*, 4th edition. MIT Press. Chapter 22 (Single-Source Shortest Paths).
3. Hart, P. E., Nilsson, N. J., & Raphael, B. (1968). *A Formal Basis for the Heuristic Determination of Minimum Cost Paths*. IEEE Transactions on Systems Science and Cybernetics, SSC-4(2), 100–107.
4. Fredman, M. L. & Tarjan, R. E. (1987). *Fibonacci Heaps and Their Uses in Improved Network Optimization Algorithms*. Journal of the ACM 34(3), 596–615.
5. Bellman, R. (1958). *On a Routing Problem*. Quarterly of Applied Mathematics 16(1), 87–90.
6. Floyd, R. W. (1962). *Algorithm 97: Shortest Path*. Communications of the ACM 5(6), 345.
7. Geisberger, R., Sanders, P., Schultes, D., & Delling, D. (2008). *Contraction Hierarchies: Faster and Simpler Hierarchical Routing in Road Networks*. In *Experimental Algorithms*. Springer.
8. Goldberg, A. V. & Harrelson, C. (2005). *Computing the Shortest Path: A* Search Meets Graph Theory*. Proc. 16th SODA, 156–165.
9. Branston, D. (1976). *Link Capacity Functions: A Review*. Transportation Research 10(4), 223–236.
10. Orda, A. & Rom, R. (1990). *Shortest-path and minimum-delay algorithms in networks with time-dependent edge-length*. Journal of the ACM 37(3), 607–625.
