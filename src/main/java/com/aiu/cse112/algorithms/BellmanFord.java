package com.aiu.cse112.algorithms;

import com.aiu.cse112.graph.Edge;
import com.aiu.cse112.graph.Graph;

import java.util.*;

/**
 * Bellman-Ford single-source shortest-path algorithm.
 *
 * <p>Used as a comparison baseline in §6.1 of THEORETICAL_ANALYSIS.md.</p>
 *
 * <p><strong>Complexity</strong>:
 * <ul>
 *   <li>Time: {@code O(V · E)}.</li>
 *   <li>Space: {@code O(V)}.</li>
 * </ul>
 *
 * <p>Bellman-Ford handles graphs with negative edge weights and
 * detects negative cycles. Both features are unnecessary for the
 * Cairo network — every weight is a non-negative distance, time, or
 * cost — so Dijkstra strictly dominates Bellman-Ford in this
 * setting.</p>
 *
 * @return triple (dist, prev, hasNegativeCycle).
 */
public final class BellmanFord {

    public record Result(Map<String, Double> dist,
                         Map<String, String> prev,
                         boolean hasNegativeCycle,
                         long opsCounter) { }

    private BellmanFord() { /* static utility */ }

    public static Result run(Graph g, String source) {
        if (g.node(source) == null) throw new NoSuchElementException(source);

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        for (String v : g.vertexIds()) {
            dist.put(v, Double.POSITIVE_INFINITY);
            prev.put(v, null);
        }
        dist.put(source, 0.0);

        // Pre-compute the directed edge list once.
        List<Edge> edges = new ArrayList<>();
        for (String v : g.vertexIds()) edges.addAll(g.neighbors(v));

        long ops = 0;

        // Relax all edges V-1 times.
        for (int i = 0; i < g.V() - 1; i++) {
            boolean changed = false;
            for (Edge e : edges) {
                ops++;
                double du = dist.get(e.src());
                if (du == Double.POSITIVE_INFINITY) continue;
                if (du + e.weight() < dist.get(e.dst())) {
                    dist.put(e.dst(), du + e.weight());
                    prev.put(e.dst(), e.src());
                    changed = true;
                }
            }
            if (!changed) break;     // early termination if a full pass made no relaxation
        }

        // One more pass to detect a negative cycle.
        boolean hasNegCycle = false;
        for (Edge e : edges) {
            double du = dist.get(e.src());
            if (du == Double.POSITIVE_INFINITY) continue;
            if (du + e.weight() < dist.get(e.dst())) { hasNegCycle = true; break; }
        }

        return new Result(dist, prev, hasNegCycle, ops);
    }
}
