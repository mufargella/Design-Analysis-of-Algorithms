package com.aiu.cse112.algorithms;

import com.aiu.cse112.graph.Edge;
import com.aiu.cse112.graph.Graph;

import java.util.*;

/**
 * Dijkstra's algorithm using a binary min-heap (Java's
 * {@link PriorityQueue}) for the priority queue.
 *
 * <p><strong>Complexity (proved in §5.2 of THEORETICAL_ANALYSIS.md)</strong>:
 * <ul>
 *   <li>Time: {@code O((V + E) log V)}.</li>
 *   <li>Space: {@code O(V + E)}.</li>
 * </ul>
 *
 * <p><strong>Lazy deletion.</strong> Java's {@code PriorityQueue} does
 * not support an efficient decrease-key. Instead of paying O(log V)
 * to locate and re-bubble an entry, we simply push a fresh
 * {@code (newDist, v)} pair and discard outdated entries when they
 * surface. Each edge causes at most one such push, so the heap holds
 * at most {@code O(E)} entries — the {@code log V} factor in the
 * complexity is unaffected because {@code log E ≤ log V² = 2 log V}.</p>
 */
public final class DijkstraHeap {

    /** Internal record that lives on the priority queue. */
    private record HeapEntry(double key, String vertex) implements Comparable<HeapEntry> {
        @Override public int compareTo(HeapEntry o) { return Double.compare(this.key, o.key); }
    }

    private DijkstraHeap() { /* static utility */ }

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

        PriorityQueue<HeapEntry> pq = new PriorityQueue<>();
        pq.add(new HeapEntry(0.0, source));

        long ops = 0, pops = 0;

        while (!pq.isEmpty()) {
            HeapEntry top = pq.poll();
            String u = top.vertex();
            // Lazy deletion: skip stale entries.
            if (settled.get(u)) continue;
            if (top.key() > dist.get(u)) continue;
            settled.put(u, true);
            pops++;

            for (Edge e : g.neighbors(u)) {
                if (settled.get(e.dst())) continue;
                ops++;
                double alt = dist.get(u) + e.weight();
                if (alt < dist.get(e.dst())) {
                    dist.put(e.dst(), alt);
                    prev.put(e.dst(), u);
                    pq.add(new HeapEntry(alt, e.dst()));   // "decrease-key by re-insertion"
                }
            }
        }

        return new ShortestPathResult(dist, prev, ops, pops);
    }
}
