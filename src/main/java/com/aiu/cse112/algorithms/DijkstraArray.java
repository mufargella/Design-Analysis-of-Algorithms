package com.aiu.cse112.algorithms;

import com.aiu.cse112.graph.Edge;
import com.aiu.cse112.graph.Graph;

import java.util.*;

/**
 * Dijkstra's single-source shortest-path algorithm using a linear
 * scan to extract the next minimum vertex.
 *
 * <p><strong>Complexity (proved in §5.1 of THEORETICAL_ANALYSIS.md)</strong>:
 * <ul>
 *   <li>Time: {@code O(V² + E) = O(V²)} for connected graphs.</li>
 *   <li>Space: {@code O(V + E)}.</li>
 * </ul>
 *
 * <p>This is the implementation Edsger Dijkstra published in 1959
 * <a href="https://doi.org/10.1007/BF01386390">"A Note on Two
 * Problems in Connexion with Graphs"</a>. It is the right choice
 * for dense graphs where {@code E = Θ(V²)} because then it ties
 * the binary-heap version asymptotically and avoids heap overhead.</p>
 *
 * <p><strong>Precondition.</strong> All edge weights must be
 * non-negative. The proof of correctness depends on this assumption
 * (the greedy-choice argument in §4.2 fails for negative weights).</p>
 */
public final class DijkstraArray {

    private DijkstraArray() { /* static utility */ }

    public static ShortestPathResult run(Graph g, String source) {
        Objects.requireNonNull(source);
        if (g.node(source) == null) throw new NoSuchElementException(source);

        Map<String, Double> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        Map<String, Boolean> settled = new HashMap<>();

        for (String v : g.vertexIds()) {
            dist.put(v, Double.POSITIVE_INFINITY);
            prev.put(v, null);
            settled.put(v, false);
        }
        dist.put(source, 0.0);

        long ops = 0, pops = 0;

        for (int iter = 0; iter < g.V(); iter++) {
            // Linear-scan extract-min — this is the O(V) step that
            // makes the overall complexity O(V²).
            String u = null;
            double best = Double.POSITIVE_INFINITY;
            for (String v : g.vertexIds()) {
                if (!settled.get(v) && dist.get(v) < best) {
                    best = dist.get(v);
                    u = v;
                }
            }
            if (u == null) break;          // remaining vertices unreachable
            settled.put(u, true);
            pops++;

            for (Edge e : g.neighbors(u)) {
                if (settled.get(e.dst())) continue;
                ops++;
                double alt = dist.get(u) + e.weight();
                if (alt < dist.get(e.dst())) {
                    dist.put(e.dst(), alt);
                    prev.put(e.dst(), u);
                }
            }
        }

        return new ShortestPathResult(dist, prev, ops, pops);
    }
}
