package com.aiu.cse112.algorithms;

import com.aiu.cse112.graph.Edge;
import com.aiu.cse112.graph.Graph;
import com.aiu.cse112.graph.Node;

import java.util.*;
import java.util.function.ToDoubleBiFunction;

/**
 * A* search for point-to-point shortest paths.
 *
 * <p>Compared in §6.3 of THEORETICAL_ANALYSIS.md. With an admissible
 * and consistent heuristic, A* returns an optimal path while
 * exploring only the part of the graph that lies "towards" the
 * target, often dramatically faster than Dijkstra in practice.</p>
 *
 * <p>The default heuristic is the great-circle (haversine) distance
 * between the current node and the target, which is admissible
 * whenever the edge weight is also a length in kilometers (no road
 * can be shorter than the straight line on the Earth's surface).
 * For a time-based weight, the heuristic should be
 * {@code haversine_km / v_max_kmph * 60} so it remains a valid
 * lower bound on travel time.</p>
 *
 * <p>When the heuristic is identically zero, A* reduces to
 * Dijkstra's algorithm — this is a standard sanity check used in
 * the unit tests.</p>
 */
public final class AStar {

    private record HeapEntry(double f, String vertex) implements Comparable<HeapEntry> {
        @Override public int compareTo(HeapEntry o) { return Double.compare(this.f, o.f); }
    }

    private AStar() { /* static utility */ }

    /** Default haversine heuristic (km). */
    public static final ToDoubleBiFunction<Node, Node> HAVERSINE_KM = Node::haversineKm;

    public static ShortestPathResult run(Graph g, String source, String target) {
        return run(g, source, target, HAVERSINE_KM);
    }

    public static ShortestPathResult run(Graph g, String source, String target,
                                         ToDoubleBiFunction<Node, Node> heuristic) {
        if (g.node(source) == null) throw new NoSuchElementException(source);
        if (g.node(target) == null) throw new NoSuchElementException(target);

        Map<String, Double> gScore  = new HashMap<>();
        Map<String, String> prev    = new HashMap<>();
        Map<String, Boolean> closed = new HashMap<>();
        for (String v : g.vertexIds()) {
            gScore.put(v, Double.POSITIVE_INFINITY);
            prev.put(v, null);
            closed.put(v, false);
        }
        gScore.put(source, 0.0);

        Node tgt = g.node(target);
        PriorityQueue<HeapEntry> open = new PriorityQueue<>();
        open.add(new HeapEntry(heuristic.applyAsDouble(g.node(source), tgt), source));

        long ops = 0, pops = 0;
        while (!open.isEmpty()) {
            HeapEntry top = open.poll();
            String u = top.vertex();
            if (closed.get(u)) continue;
            closed.put(u, true);
            pops++;
            if (u.equals(target)) break;

            for (Edge e : g.neighbors(u)) {
                ops++;
                double tentative = gScore.get(u) + e.weight();
                if (tentative < gScore.get(e.dst())) {
                    gScore.put(e.dst(), tentative);
                    prev.put(e.dst(), u);
                    double f = tentative + heuristic.applyAsDouble(g.node(e.dst()), tgt);
                    open.add(new HeapEntry(f, e.dst()));
                }
            }
        }

        return new ShortestPathResult(gScore, prev, ops, pops);
    }
}
