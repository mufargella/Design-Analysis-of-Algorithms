package com.aiu.cse112.benchmark;

import com.aiu.cse112.algorithms.*;
import com.aiu.cse112.graph.Graph;
import com.aiu.cse112.io.DataLoader;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Empirical performance benchmark.
 *
 * <p>Runs each algorithm on the Cairo dataset, records wall-clock
 * runtime, the number of edge relaxations, and the number of
 * priority-queue extractions, then writes a table to
 * {@code results/benchmark_results.md}. Numbers reported in §8 of
 * THEORETICAL_ANALYSIS.md come from this script.</p>
 *
 * <p>Wall-clock numbers are noisy on small graphs (the Cairo
 * network is only ~25 vertices and ~28 roads), so we run each
 * algorithm {@code REPEATS} times and report the median. The more
 * informative numbers are the operation counters because they are
 * deterministic and let us directly compare against the analytical
 * bounds.</p>
 */
public final class PerformanceBenchmark {

    private static final int REPEATS = 1000;

    public static void main(String[] args) throws IOException {
        Graph g = DataLoader.loadGraph(false);
        System.out.printf("Loaded graph: |V| = %d, |E| = %d%n", g.V(), g.E());

        // Single-source benchmark from each neighborhood.
        StringBuilder out = new StringBuilder();
        out.append("# Empirical Benchmark Results\n\n");
        out.append("Cairo transportation network: |V| = ").append(g.V())
           .append(", |E| = ").append(g.E()).append(".\n\n");
        out.append("All numbers below are *medians* over ")
           .append(REPEATS).append(" runs.\n\n");

        runSSSPBenchmark(g, out);
        runPointToPointBenchmark(g, out);
        runAllPairsBenchmark(g, out);
        runWeightFunctionImpact(g, out);

        Path resultsDir = Paths.get("results");
        Files.createDirectories(resultsDir);
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(
                resultsDir.resolve("benchmark_results.md")))) {
            pw.print(out);
        }
        System.out.println("Wrote results/benchmark_results.md");
        System.out.println();
        System.out.println(out);
    }

    // -------------------------------------------------------------
    // Benchmarks
    // -------------------------------------------------------------

    private static void runSSSPBenchmark(Graph g, StringBuilder out) {
        out.append("## 1) Single-Source Shortest Paths (source = 3, Downtown Cairo)\n\n");
        out.append("| Algorithm | Median time (μs) | Edge relaxations | PQ extractions |\n");
        out.append("|---|---:|---:|---:|\n");

        BenchRow array  = bench("Dijkstra (array)",
                () -> DijkstraArray.run(g, "3"));
        BenchRow heap   = bench("Dijkstra (binary heap)",
                () -> DijkstraHeap.run(g, "3"));
        BenchRow trans  = bench("Dijkstra (transport)",
                () -> DijkstraTransport.run(g, "3"));
        BenchRow bf     = bench("Bellman-Ford",
                () -> {
                    BellmanFord.Result r = BellmanFord.run(g, "3");
                    return new ShortestPathResult(r.dist(), r.prev(), r.opsCounter(), 0);
                });

        out.append(String.format("| %s | %.2f | %d | %d |%n",
                array.name, array.medianMicros, array.ops, array.pops));
        out.append(String.format("| %s | %.2f | %d | %d |%n",
                heap.name, heap.medianMicros, heap.ops, heap.pops));
        out.append(String.format("| %s | %.2f | %d | %d |%n",
                trans.name, trans.medianMicros, trans.ops, trans.pops));
        out.append(String.format("| %s | %.2f | %d | n/a |%n%n",
                bf.name, bf.medianMicros, bf.ops));
    }

    private static void runPointToPointBenchmark(Graph g, StringBuilder out) {
        out.append("## 2) Point-to-Point: source = 13 (New Admin. Capital), target = 7 (6th October)\n\n");
        out.append("| Algorithm | Median time (μs) | Edge relaxations | PQ extractions |\n");
        out.append("|---|---:|---:|---:|\n");

        BenchRow heap = bench("Dijkstra (binary heap, full SSSP)",
                () -> DijkstraHeap.run(g, "13"));
        BenchRow trans = bench("Dijkstra (transport, early exit)",
                () -> DijkstraTransport.run(g, "13", "7", Graph::byDistance,
                        DijkstraTransport.EdgeVCRatio.NONE, 1.0));
        BenchRow astar = bench("A* (haversine heuristic)",
                () -> AStar.run(g, "13", "7"));

        out.append(String.format("| %s | %.2f | %d | %d |%n",
                heap.name, heap.medianMicros, heap.ops, heap.pops));
        out.append(String.format("| %s | %.2f | %d | %d |%n",
                trans.name, trans.medianMicros, trans.ops, trans.pops));
        out.append(String.format("| %s | %.2f | %d | %d |%n%n",
                astar.name, astar.medianMicros, astar.ops, astar.pops));
    }

    private static void runAllPairsBenchmark(Graph g, StringBuilder out) {
        out.append("## 3) All-Pairs Shortest Paths\n\n");
        out.append("| Algorithm | Median time (μs) | Operations |\n");
        out.append("|---|---:|---:|\n");

        // V applications of Dijkstra-heap
        long[] timings = new long[REPEATS];
        long ops = 0, pops = 0;
        for (int r = 0; r < REPEATS; r++) {
            long t0 = System.nanoTime();
            long o = 0, p = 0;
            for (String s : g.vertexIds()) {
                ShortestPathResult res = DijkstraHeap.run(g, s);
                o += res.opsCounter();
                p += res.popsCounter();
            }
            timings[r] = System.nanoTime() - t0;
            if (r == 0) { ops = o; pops = p; }
        }
        Arrays.sort(timings);
        double medUs = timings[REPEATS / 2] / 1000.0;
        out.append(String.format("| Dijkstra-heap × V | %.2f | %d relax, %d pops |%n", medUs, ops, pops));

        // Floyd-Warshall
        long[] timings2 = new long[REPEATS];
        long opsFw = 0;
        for (int r = 0; r < REPEATS; r++) {
            long t0 = System.nanoTime();
            FloydWarshall.Result res = FloydWarshall.run(g);
            timings2[r] = System.nanoTime() - t0;
            if (r == 0) opsFw = res.opsCounter();
        }
        Arrays.sort(timings2);
        double medUs2 = timings2[REPEATS / 2] / 1000.0;
        out.append(String.format("| Floyd-Warshall | %.2f | %d updates |%n%n", medUs2, opsFw));
    }

    private static void runWeightFunctionImpact(Graph g, StringBuilder out) {
        out.append("## 4) Effect of Weight Function on Optimal Route (3 → 13)\n\n");
        out.append("| Weight function | Distance label of target | Hops | Path |\n");
        out.append("|---|---:|---:|---|\n");

        // Distance
        var r1 = DijkstraTransport.run(g, "3", "13", Graph::byDistance,
                DijkstraTransport.EdgeVCRatio.NONE, 1.0);
        out.append(formatRouteRow("distance (km)", r1, "13"));

        // Free-flow time
        var r2 = DijkstraTransport.run(g, "3", "13", Graph::byFreeFlowTime,
                DijkstraTransport.EdgeVCRatio.NONE, 1.0);
        out.append(formatRouteRow("free-flow time (min)", r2, "13"));

        // BPR with v/c = 0.8 (peak hours)
        var r3 = DijkstraTransport.run(g, "3", "13",
                e -> Graph.byBPRTime(e, 0.8),
                DijkstraTransport.EdgeVCRatio.NONE, 1.0);
        out.append(formatRouteRow("BPR @ v/c=0.8 (min)", r3, "13"));

        out.append('\n');
    }

    private static String formatRouteRow(String label, ShortestPathResult r, String target) {
        Double d = r.dist().get(target);
        var path = r.path(target);
        return String.format("| %s | %.2f | %d | %s |%n",
                label,
                d == null || Double.isInfinite(d) ? Double.NaN : d,
                Math.max(0, path.size() - 1),
                String.join(" → ", path));
    }

    // -------------------------------------------------------------
    // Tiny benchmark harness
    // -------------------------------------------------------------
    private record BenchRow(String name, double medianMicros, long ops, long pops) { }

    private static BenchRow bench(String name, java.util.function.Supplier<ShortestPathResult> fn) {
        long[] timings = new long[REPEATS];
        long ops = 0, pops = 0;
        for (int r = 0; r < REPEATS; r++) {
            long t0 = System.nanoTime();
            ShortestPathResult res = fn.get();
            timings[r] = System.nanoTime() - t0;
            if (r == 0) { ops = res.opsCounter(); pops = res.popsCounter(); }
        }
        Arrays.sort(timings);
        return new BenchRow(name, timings[REPEATS / 2] / 1000.0, ops, pops);
    }
}
