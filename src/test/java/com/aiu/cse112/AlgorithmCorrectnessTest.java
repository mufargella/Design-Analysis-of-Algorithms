package com.aiu.cse112;

import com.aiu.cse112.algorithms.*;
import com.aiu.cse112.graph.Graph;
import com.aiu.cse112.graph.Node;
import com.aiu.cse112.io.DataLoader;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Correctness tests for the Dijkstra implementations, plus
 * cross-checks against alternative algorithms.
 *
 * <p>The fundamental check is that all four algorithms (DijkstraArray,
 * DijkstraHeap, DijkstraTransport with no early exit, and Bellman-Ford)
 * agree on the distance labels for every pair of vertices, and that
 * A* with an admissible heuristic agrees on the optimal cost for any
 * point-to-point query.</p>
 *
 * <p>If a future code change breaks the proof of correctness, these
 * tests fail loudly. They take less than a second to run.</p>
 */
public class AlgorithmCorrectnessTest {

    private static Graph G;

    @BeforeAll
    static void setUp() throws IOException {
        G = DataLoader.loadGraph(false);
    }

    /**
     * For every source vertex, the array and heap Dijkstra
     * implementations must produce identical distance labels.
     */
    @Test
    void arrayAndHeapDijkstraAgree() {
        for (String src : G.vertexIds()) {
            ShortestPathResult a = DijkstraArray.run(G, src);
            ShortestPathResult h = DijkstraHeap.run(G, src);
            for (String v : G.vertexIds()) {
                assertEquals(a.dist().get(v), h.dist().get(v), 1e-9,
                        "Disagreement: src=" + src + ", v=" + v);
            }
        }
    }

    /**
     * Bellman-Ford must agree with Dijkstra on a non-negative-weight
     * graph (the only setting where Dijkstra is correct).
     */
    @Test
    void bellmanFordAgreesWithDijkstra() {
        for (String src : List.of("3", "13", "F1", "7")) {
            ShortestPathResult d = DijkstraHeap.run(G, src);
            BellmanFord.Result bf = BellmanFord.run(G, src);
            assertFalse(bf.hasNegativeCycle(), "unexpected negative cycle");
            for (String v : G.vertexIds()) {
                assertEquals(d.dist().get(v), bf.dist().get(v), 1e-9,
                        "BF/Dijkstra disagree at v=" + v);
            }
        }
    }

    /**
     * A* with the haversine heuristic must produce the optimal
     * distance for any point-to-point query. The heuristic is
     * admissible because the haversine distance is a lower bound
     * on the road distance between two points.
     */
    @Test
    void aStarMatchesDijkstraOnPointToPoint() {
        for (String src : List.of("3", "13", "7", "12")) {
            ShortestPathResult d = DijkstraHeap.run(G, src);
            for (String tgt : G.vertexIds()) {
                if (src.equals(tgt)) continue;
                ShortestPathResult a = AStar.run(G, src, tgt);
                Double dDist = d.dist().get(tgt);
                Double aDist = a.dist().get(tgt);
                if (Double.isInfinite(dDist) && Double.isInfinite(aDist)) continue;
                assertEquals(dDist, aDist, 1e-9,
                        "A* disagrees: " + src + "→" + tgt
                                + " (Dijkstra=" + dDist + ", A*=" + aDist + ")");
            }
        }
    }

    /**
     * The transport variant with default settings (distance, no early
     * exit, no v/c filtering) is just Dijkstra and must agree on all
     * distances.
     */
    @Test
    void transportDefaultEqualsDijkstra() {
        for (String src : List.of("3", "F1", "13")) {
            ShortestPathResult d = DijkstraHeap.run(G, src);
            ShortestPathResult t = DijkstraTransport.run(G, src);
            for (String v : G.vertexIds()) {
                assertEquals(d.dist().get(v), t.dist().get(v), 1e-9,
                        "Transport-default disagrees at v=" + v);
            }
        }
    }

    /**
     * Early termination must produce the correct distance for the
     * given target (other vertices may be left unfinalized — that
     * is the whole point of early exit).
     */
    @Test
    void transportEarlyExitProducesCorrectTargetDist() {
        ShortestPathResult full = DijkstraHeap.run(G, "3");
        for (String tgt : List.of("12", "F1", "13", "7", "9")) {
            ShortestPathResult early = DijkstraTransport.run(G, "3", tgt,
                    Graph::byDistance, DijkstraTransport.EdgeVCRatio.NONE, 1.0);
            assertEquals(full.dist().get(tgt), early.dist().get(tgt), 1e-9,
                    "Early exit wrong for target " + tgt);
        }
    }

    /**
     * Floyd-Warshall must produce the same all-pairs distances as
     * V applications of Dijkstra.
     */
    @Test
    void floydWarshallMatchesDijkstra() {
        FloydWarshall.Result fw = FloydWarshall.run(G);
        // Build index lookup
        java.util.Map<String, Integer> idx = new java.util.HashMap<>();
        for (int i = 0; i < fw.index().length; i++) idx.put(fw.index()[i], i);

        for (String src : G.vertexIds()) {
            ShortestPathResult d = DijkstraHeap.run(G, src);
            for (String v : G.vertexIds()) {
                double a = d.dist().get(v);
                double b = fw.dist()[idx.get(src)][idx.get(v)];
                if (Double.isInfinite(a) && Double.isInfinite(b)) continue;
                assertEquals(a, b, 1e-9, "FW vs Dijkstra: src=" + src + " v=" + v);
            }
        }
    }

    /**
     * Reconstructed paths must be valid: every consecutive pair of
     * vertices must be connected by an actual edge in the graph.
     */
    @Test
    void reconstructedPathsAreValid() {
        ShortestPathResult r = DijkstraHeap.run(G, "3");
        for (String tgt : G.vertexIds()) {
            if (tgt.equals("3")) continue;
            if (Double.isInfinite(r.dist().get(tgt))) continue;
            List<String> path = r.path(tgt);
            assertEquals("3", path.get(0), "Path must start at source");
            assertEquals(tgt, path.get(path.size() - 1), "Path must end at target");
            // Every step is an edge
            for (int i = 0; i + 1 < path.size(); i++) {
                String u = path.get(i), v = path.get(i + 1);
                boolean found = G.neighbors(u).stream().anyMatch(e -> e.dst().equals(v));
                assertTrue(found, "No edge " + u + "->" + v);
            }
        }
    }

    /**
     * Triangle inequality: dist(s, t) ≤ dist(s, u) + dist(u, t)
     * for any intermediate u. This must hold for *every* triple
     * in the output of a correct shortest-path algorithm.
     */
    @Test
    void triangleInequalityHolds() {
        ShortestPathResult r = DijkstraHeap.run(G, "3");
        BellmanFord.Result bf2 = BellmanFord.run(G, "9");
        for (String t : List.of("12", "13", "F1", "7")) {
            double dst = r.dist().get(t);
            double d_su = r.dist().get("9");
            double d_ut = bf2.dist().get(t);
            if (Double.isInfinite(d_su) || Double.isInfinite(d_ut)) continue;
            assertTrue(dst <= d_su + d_ut + 1e-9,
                    "Triangle inequality broken for s=3 u=9 t=" + t);
        }
    }
}
