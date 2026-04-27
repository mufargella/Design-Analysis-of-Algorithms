package com.aiu.cse112.graph;

import java.util.*;
import java.util.function.ToDoubleFunction;

/**
 * Weighted, directed graph backed by adjacency lists.
 *
 * <p>The Cairo road network is undirected: calling
 * {@link #addRoad(String, String, double, int, int, boolean, double)}
 * inserts <em>both</em> directed edges (u→v and v→u). All
 * shortest-path algorithms in this project consume the directed
 * representation, so they work uniformly on directed and
 * undirected inputs.</p>
 *
 * <p>The class supports pluggable edge weights via
 * {@link #setWeightFunction(ToDoubleFunction)}. We use this to
 * compare three different objectives (distance, time, multi-objective)
 * on identical topology in the benchmarks.</p>
 */
public final class Graph {

    /** Vertex id → Node. LinkedHashMap to preserve insertion order in tests. */
    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private final Map<String, List<Edge>> adj = new HashMap<>();

    // ---------------------------------------------------------------
    // Construction
    // ---------------------------------------------------------------
    public void addNode(Node n) {
        nodes.put(n.id(), n);
        adj.computeIfAbsent(n.id(), k -> new ArrayList<>());
    }

    /** Adds an undirected road as two directed edges. */
    public void addRoad(String u, String v, double distanceKm, int capacity,
                        int condition, boolean potential, double constructionCost) {
        if (!nodes.containsKey(u) || !nodes.containsKey(v)) {
            throw new NoSuchElementException(
                    "Cannot add edge " + u + "->" + v + ": missing endpoint.");
        }
        adj.get(u).add(new Edge(u, v, distanceKm, capacity, condition,
                                 potential, constructionCost));
        adj.get(v).add(new Edge(v, u, distanceKm, capacity, condition,
                                 potential, constructionCost));
    }

    // ---------------------------------------------------------------
    // Queries
    // ---------------------------------------------------------------
    public List<Edge> neighbors(String u) {
        return adj.getOrDefault(u, Collections.emptyList());
    }

    public Node node(String id) {
        return nodes.get(id);
    }

    public Set<String> vertexIds() {
        return Collections.unmodifiableSet(nodes.keySet());
    }

    public Collection<Node> allNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    /** Number of vertices |V|. */
    public int V() { return nodes.size(); }

    /** Number of <em>logical</em> (undirected) edges |E|. */
    public int E() {
        int directed = 0;
        for (List<Edge> es : adj.values()) directed += es.size();
        return directed / 2;
    }

    /** Stream of all edges, optionally including the "potential new roads". */
    public List<Edge> allEdges(boolean includePotential) {
        List<Edge> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (var entry : adj.entrySet()) {
            for (Edge e : entry.getValue()) {
                if (!includePotential && e.isPotential()) continue;
                String key = e.src().compareTo(e.dst()) < 0
                        ? e.src() + "|" + e.dst()
                        : e.dst() + "|" + e.src();
                if (seen.add(key)) result.add(e);
            }
        }
        return result;
    }

    // ---------------------------------------------------------------
    // Weight functions
    // ---------------------------------------------------------------
    public void setWeightFunction(ToDoubleFunction<Edge> fn) {
        for (List<Edge> es : adj.values())
            for (Edge e : es) e.setWeight(fn.applyAsDouble(e));
    }

    /** Pure distance objective, in kilometers. */
    public static double byDistance(Edge e) {
        return e.distanceKm();
    }

    /**
     * Free-flow travel time in minutes.
     * Free-flow speed is interpolated linearly with road condition:
     * condition 1 → 30 km/h, condition 10 → 60 km/h.
     */
    public static double byFreeFlowTime(Edge e) {
        double vFree = 30.0 + (e.condition() - 1) * (30.0 / 9.0); // 30..60 km/h
        return (e.distanceKm() / vFree) * 60.0;                    // minutes
    }

    /**
     * Bureau-of-Public-Roads (BPR) congested travel time in minutes,
     * assuming a given volume/capacity ratio.
     *
     *   t = t_free * (1 + 0.15 * (v/c)^4)
     *
     * v is supplied externally per-edge (see TrafficData), but this
     * static helper assumes a fixed v/c so the comparison runs are
     * deterministic.
     */
    public static double byBPRTime(Edge e, double vc) {
        return byFreeFlowTime(e) * (1.0 + 0.15 * Math.pow(vc, 4));
    }

    /** Construction cost — for the MST/network-design comparison. */
    public static double byConstructionCost(Edge e) {
        return e.isPotential() ? e.constructionCost() : 0.0;
    }
}
