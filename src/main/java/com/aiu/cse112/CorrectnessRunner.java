package com.aiu.cse112;

import com.aiu.cse112.algorithms.*;
import com.aiu.cse112.graph.Graph;
import com.aiu.cse112.io.DataLoader;

import java.io.IOException;
import java.util.List;

/**
 * Self-contained correctness checker that does not require JUnit.
 *
 * <p>The {@code AlgorithmCorrectnessTest} class is the canonical
 * test suite (run by {@code mvn test}); this class is a thin
 * wrapper that exercises the same checks via plain {@code assert}
 * statements so the tests can be verified with a single
 * {@code java} invocation.</p>
 *
 * <p>Run with:</p>
 * <pre>
 *   javac -d out src/main/java/com/aiu/cse112/&hellip;/*.java
 *   java -ea -cp out com.aiu.cse112.CorrectnessRunner
 * </pre>
 */
public final class CorrectnessRunner {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws IOException {
        Graph g = DataLoader.loadGraph(false);

        run("Array == Heap (all sources)",        () -> arrayEqualsHeap(g));
        run("Bellman-Ford == Dijkstra",           () -> bellmanFordEqualsDijkstra(g));
        run("A* == Dijkstra (point-to-point)",    () -> aStarEqualsDijkstra(g));
        run("Transport-default == Dijkstra",      () -> transportDefaultEqualsDijkstra(g));
        run("Transport early exit returns truth", () -> transportEarlyExit(g));
        run("Floyd-Warshall == Dijkstra (all-pairs)", () -> floydWarshallEqualsDijkstra(g));
        run("Reconstructed paths are valid",      () -> pathsAreValid(g));
        run("Triangle inequality holds",          () -> triangleInequality(g));

        System.out.printf("%n=========================%n");
        System.out.printf("Tests: %d passed, %d failed%n", passed, failed);
        System.out.printf("=========================%n");
        if (failed > 0) System.exit(1);
    }

    @FunctionalInterface
    private interface Test { void run() throws Exception; }

    private static void run(String name, Test t) {
        try {
            t.run();
            System.out.printf("  PASS  %s%n", name);
            passed++;
        } catch (AssertionError | Exception ex) {
            System.out.printf("  FAIL  %s : %s%n", name, ex.getMessage());
            failed++;
        }
    }

    // -------------------------------------------------------------
    private static void arrayEqualsHeap(Graph g) {
        for (String src : g.vertexIds()) {
            var a = DijkstraArray.run(g, src);
            var h = DijkstraHeap.run(g, src);
            for (String v : g.vertexIds()) {
                double da = a.dist().get(v), dh = h.dist().get(v);
                if (Math.abs(da - dh) > 1e-9 && !(Double.isInfinite(da) && Double.isInfinite(dh)))
                    throw new AssertionError("src=" + src + " v=" + v
                            + " arr=" + da + " heap=" + dh);
            }
        }
    }

    private static void bellmanFordEqualsDijkstra(Graph g) {
        for (String src : List.of("3", "13", "F1", "7")) {
            var d = DijkstraHeap.run(g, src);
            var bf = BellmanFord.run(g, src);
            if (bf.hasNegativeCycle())
                throw new AssertionError("unexpected negative cycle");
            for (String v : g.vertexIds()) {
                double dd = d.dist().get(v), db = bf.dist().get(v);
                if (Math.abs(dd - db) > 1e-9 && !(Double.isInfinite(dd) && Double.isInfinite(db)))
                    throw new AssertionError("src=" + src + " v=" + v
                            + " dij=" + dd + " bf=" + db);
            }
        }
    }

    private static void aStarEqualsDijkstra(Graph g) {
        for (String src : List.of("3", "13", "7", "12")) {
            var d = DijkstraHeap.run(g, src);
            for (String tgt : g.vertexIds()) {
                if (src.equals(tgt)) continue;
                var a = AStar.run(g, src, tgt);
                double dd = d.dist().get(tgt), da = a.dist().get(tgt);
                if (Double.isInfinite(dd) && Double.isInfinite(da)) continue;
                if (Math.abs(dd - da) > 1e-9)
                    throw new AssertionError(src + "->" + tgt + " dij=" + dd + " a*=" + da);
            }
        }
    }

    private static void transportDefaultEqualsDijkstra(Graph g) {
        for (String src : List.of("3", "F1", "13")) {
            var d = DijkstraHeap.run(g, src);
            var t = DijkstraTransport.run(g, src);
            for (String v : g.vertexIds()) {
                double dd = d.dist().get(v), dt = t.dist().get(v);
                if (Math.abs(dd - dt) > 1e-9 && !(Double.isInfinite(dd) && Double.isInfinite(dt)))
                    throw new AssertionError("src=" + src + " v=" + v
                            + " dij=" + dd + " transport=" + dt);
            }
        }
    }

    private static void transportEarlyExit(Graph g) {
        var full = DijkstraHeap.run(g, "3");
        for (String tgt : List.of("12", "F1", "13", "7", "9")) {
            var early = DijkstraTransport.run(g, "3", tgt,
                    Graph::byDistance, DijkstraTransport.EdgeVCRatio.NONE, 1.0);
            double df = full.dist().get(tgt), de = early.dist().get(tgt);
            if (Math.abs(df - de) > 1e-9)
                throw new AssertionError("tgt=" + tgt + " full=" + df + " early=" + de);
        }
    }

    private static void floydWarshallEqualsDijkstra(Graph g) {
        var fw = FloydWarshall.run(g);
        java.util.Map<String, Integer> idx = new java.util.HashMap<>();
        for (int i = 0; i < fw.index().length; i++) idx.put(fw.index()[i], i);

        for (String src : g.vertexIds()) {
            var d = DijkstraHeap.run(g, src);
            for (String v : g.vertexIds()) {
                double a = d.dist().get(v);
                double b = fw.dist()[idx.get(src)][idx.get(v)];
                if (Double.isInfinite(a) && Double.isInfinite(b)) continue;
                if (Math.abs(a - b) > 1e-9)
                    throw new AssertionError("src=" + src + " v=" + v
                            + " dij=" + a + " fw=" + b);
            }
        }
    }

    private static void pathsAreValid(Graph g) {
        var r = DijkstraHeap.run(g, "3");
        for (String tgt : g.vertexIds()) {
            if (tgt.equals("3")) continue;
            if (Double.isInfinite(r.dist().get(tgt))) continue;
            List<String> path = r.path(tgt);
            if (!path.get(0).equals("3"))
                throw new AssertionError("path doesn't start at source");
            if (!path.get(path.size() - 1).equals(tgt))
                throw new AssertionError("path doesn't end at target");
            for (int i = 0; i + 1 < path.size(); i++) {
                String u = path.get(i), v = path.get(i + 1);
                boolean found = g.neighbors(u).stream().anyMatch(e -> e.dst().equals(v));
                if (!found)
                    throw new AssertionError("no edge " + u + "->" + v);
            }
        }
    }

    private static void triangleInequality(Graph g) {
        var r = DijkstraHeap.run(g, "3");
        var bf2 = BellmanFord.run(g, "9");
        for (String t : List.of("12", "13", "F1", "7")) {
            double dst   = r.dist().get(t);
            double d_su  = r.dist().get("9");
            double d_ut  = bf2.dist().get(t);
            if (Double.isInfinite(d_su) || Double.isInfinite(d_ut)) continue;
            if (dst > d_su + d_ut + 1e-9)
                throw new AssertionError("triangle inequality violated for t=" + t
                        + " : " + dst + " > " + d_su + " + " + d_ut);
        }
    }
}
