package com.aiu.cse112.algorithms;

import com.aiu.cse112.graph.Edge;
import com.aiu.cse112.graph.Graph;

import java.util.*;

/**
 * Floyd-Warshall all-pairs shortest paths via dynamic programming.
 *
 * <p>Compared in §6.2 of THEORETICAL_ANALYSIS.md.</p>
 *
 * <p><strong>Recurrence</strong>:
 * <pre>
 *   d^{(k)}[i][j] = min( d^{(k-1)}[i][j],
 *                        d^{(k-1)}[i][k] + d^{(k-1)}[k][j] ).
 * </pre>
 * <p>The k-th iteration uses k as an allowed intermediate vertex.
 * After processing all V values of k, {@code d[i][j]} equals the
 * length of the shortest path from i to j using any subset of the
 * vertices as intermediates.</p>
 *
 * <p><strong>Complexity</strong>:
 * <ul>
 *   <li>Time: {@code Θ(V³)}.</li>
 *   <li>Space: {@code Θ(V²)} for the distance matrix; we also keep
 *       a {@code next-hop} matrix of the same size for path
 *       reconstruction.</li>
 * </ul>
 *
 * <p>For dense graphs Floyd-Warshall ties Dijkstra-from-every-vertex
 * with a Fibonacci heap: both are O(V³). For sparse graphs (such as
 * the Cairo network, where E ≈ V), running Dijkstra V times is
 * asymptotically faster — O(V·(V+E) log V) ≈ O(V² log V).</p>
 */
public final class FloydWarshall {

    public record Result(double[][] dist, int[][] next, String[] index, long opsCounter) {
        public List<String> path(String source, String target) {
            int s = -1, t = -1;
            for (int i = 0; i < index.length; i++) {
                if (index[i].equals(source)) s = i;
                if (index[i].equals(target)) t = i;
            }
            if (s < 0 || t < 0) return List.of();
            if (next[s][t] == -1) return List.of();
            List<String> p = new ArrayList<>();
            p.add(index[s]);
            int cur = s;
            while (cur != t) {
                cur = next[cur][t];
                if (cur == -1) return List.of();
                p.add(index[cur]);
            }
            return p;
        }
    }

    private FloydWarshall() { /* static utility */ }

    public static Result run(Graph g) {
        // Build a stable index of vertices.
        List<String> ids = new ArrayList<>(g.vertexIds());
        int n = ids.size();
        Map<String, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(ids.get(i), i);

        double[][] dist = new double[n][n];
        int[][] next = new int[n][n];
        for (int i = 0; i < n; i++) {
            Arrays.fill(dist[i], Double.POSITIVE_INFINITY);
            Arrays.fill(next[i], -1);
            dist[i][i] = 0.0;
        }

        for (String u : ids) {
            int i = idx.get(u);
            for (Edge e : g.neighbors(u)) {
                int j = idx.get(e.dst());
                if (e.weight() < dist[i][j]) {
                    dist[i][j] = e.weight();
                    next[i][j] = j;
                }
            }
        }

        long ops = 0;
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                double dik = dist[i][k];
                if (dik == Double.POSITIVE_INFINITY) continue;
                for (int j = 0; j < n; j++) {
                    ops++;
                    double cand = dik + dist[k][j];
                    if (cand < dist[i][j]) {
                        dist[i][j] = cand;
                        next[i][j] = next[i][k];
                    }
                }
            }
        }

        return new Result(dist, next, ids.toArray(new String[0]), ops);
    }
}
