package com.aiu.cse112.algorithms;

import com.aiu.cse112.graph.Edge;
import com.aiu.cse112.graph.Graph;

import java.util.*;
import java.util.function.ToDoubleFunction;

/**
 * Dijkstra's algorithm <em>adapted for the Cairo transportation
 * optimization system</em>.
 *
 * <p>Compared with the textbook implementation, this version makes
 * three modifications. They are described in §7 of
 * THEORETICAL_ANALYSIS.md and proved correct there.</p>
 *
 * <ol>
 *   <li><strong>Multi-objective edge weight.</strong> The caller passes
 *       a {@code weightFn} so we can route on distance, on
 *       free-flow time, on BPR-congested time, or on a composite
 *       {@code α·distance + β·time + γ·(11−condition)}. This
 *       changes only the weight definition, not the algorithm —
 *       so the proof of optimality is preserved as long as the
 *       weight function is non-negative.</li>
 *
 *   <li><strong>Early termination on a fixed target.</strong> When
 *       the caller supplies a {@code target}, the search stops as
 *       soon as {@code target} is settled. This is correct because
 *       once a vertex is settled its dist label equals the true
 *       distance (Lemma 4.1 in the analysis). The worst-case bound
 *       is unchanged but in practice the search visits only a
 *       fraction of the graph for point-to-point queries.</li>
 *
 *   <li><strong>Capacity-aware edge filtering.</strong> Edges whose
 *       volume/capacity ratio exceeds {@code vcThreshold} are
 *       skipped, modelling the operator's preference for routes
 *       that avoid gridlock. Because we only <em>remove</em> edges
 *       from consideration, the algorithm still computes a valid
 *       shortest path on the resulting subgraph; it never returns
 *       a non-optimal path within that subgraph.</li>
 * </ol>
 */
public final class DijkstraTransport {

    private record HeapEntry(double key, String vertex) implements Comparable<HeapEntry> {
        @Override public int compareTo(HeapEntry o) { return Double.compare(this.key, o.key); }
    }

    /** Per-edge volume/capacity ratio supplied by the traffic model. */
    @FunctionalInterface
    public interface EdgeVCRatio {
        double of(Edge e);
        EdgeVCRatio NONE = e -> 0.0;
    }

    private DijkstraTransport() { /* static utility */ }

    /**
     * Run modified Dijkstra.
     *
     * @param g          the graph (weights will be overwritten by {@code weightFn}).
     * @param source     the source vertex.
     * @param target     optional target vertex; pass {@code null} for full SSSP.
     * @param weightFn   per-edge weight function (must be non-negative).
     * @param vc         per-edge volume/capacity ratio supplier (use
     *                   {@link EdgeVCRatio#NONE} to disable filtering).
     * @param vcThreshold edges with v/c ≥ threshold are excluded.
     */
    public static ShortestPathResult run(Graph g,
                                         String source,
                                         String target,
                                         ToDoubleFunction<Edge> weightFn,
                                         EdgeVCRatio vc,
                                         double vcThreshold) {
        Objects.requireNonNull(source);
        Objects.requireNonNull(weightFn);
        if (g.node(source) == null) throw new NoSuchElementException(source);
        if (target != null && g.node(target) == null) throw new NoSuchElementException(target);

        // Modification 1: install the chosen weight function.
        g.setWeightFunction(weightFn);

        Map<String, Double>  dist    = new HashMap<>();
        Map<String, String>  prev    = new HashMap<>();
        Map<String, Boolean> settled = new HashMap<>();

        for (String v : g.vertexIds()) {
            dist.put(v, Double.POSITIVE_INFINITY);
            prev.put(v, null);
            settled.put(v, false);
        }
        dist.put(source, 0.0);

        PriorityQueue<HeapEntry> pq = new PriorityQueue<>();
        pq.add(new HeapEntry(0.0, source));

        long ops = 0, pops = 0;

        while (!pq.isEmpty()) {
            HeapEntry top = pq.poll();
            String u = top.vertex();
            if (settled.get(u)) continue;
            if (top.key() > dist.get(u)) continue;
            settled.put(u, true);
            pops++;

            // Modification 2: early termination at a designated target.
            if (target != null && u.equals(target)) break;

            for (Edge e : g.neighbors(u)) {
                if (settled.get(e.dst())) continue;

                // Modification 3: skip near-saturated edges.
                if (vc != EdgeVCRatio.NONE && vc.of(e) >= vcThreshold) continue;

                ops++;
                double alt = dist.get(u) + e.weight();
                if (alt < dist.get(e.dst())) {
                    dist.put(e.dst(), alt);
                    prev.put(e.dst(), u);
                    pq.add(new HeapEntry(alt, e.dst()));
                }
            }
        }

        return new ShortestPathResult(dist, prev, ops, pops);
    }

    /** Convenience overload: distance-only, no early termination, no v/c filter. */
    public static ShortestPathResult run(Graph g, String source) {
        return run(g, source, null, Graph::byDistance, EdgeVCRatio.NONE, 1.0);
    }
}
