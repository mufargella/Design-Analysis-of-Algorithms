package com.aiu.cse112;

import com.aiu.cse112.algorithms.*;
import com.aiu.cse112.graph.Graph;
import com.aiu.cse112.graph.Node;
import com.aiu.cse112.io.DataLoader;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Command-line demo for the project.
 *
 * <p>Run with:</p>
 * <pre>
 *   mvn -q exec:java -Dexec.mainClass=com.aiu.cse112.Main
 * </pre>
 * <p>or, after {@code mvn package},</p>
 * <pre>
 *   java -jar target/algo-1.0.0.jar
 * </pre>
 */
public final class Main {

    public static void main(String[] args) throws IOException {
        Graph g = DataLoader.loadGraph(false);
        System.out.printf("Loaded Cairo network: |V| = %d, |E| = %d%n%n", g.V(), g.E());

        printDemo(g, "3", "12");    // Downtown Cairo → Helwan
        printDemo(g, "F1", "7");    // Airport       → 6th October
        printDemo(g, "13", "9");    // New Admin Cap → Mohandessin
        printWeightImpact(g, "3", "13");
    }

    private static void printDemo(Graph g, String src, String dst) {
        System.out.printf("Shortest path from %s (%s) to %s (%s):%n",
                src, g.node(src).name(), dst, g.node(dst).name());

        // 1) array Dijkstra
        ShortestPathResult arr = DijkstraArray.run(g, src);
        printResult("Dijkstra (array)", arr, dst, g);

        // 2) heap Dijkstra
        ShortestPathResult heap = DijkstraHeap.run(g, src);
        printResult("Dijkstra (heap)", heap, dst, g);

        // 3) transport Dijkstra with early termination
        ShortestPathResult t = DijkstraTransport.run(g, src, dst,
                Graph::byDistance, DijkstraTransport.EdgeVCRatio.NONE, 1.0);
        printResult("Dijkstra (transport, early exit)", t, dst, g);

        // 4) A*
        ShortestPathResult a = AStar.run(g, src, dst);
        printResult("A* (haversine)", a, dst, g);

        System.out.println();
    }

    private static void printResult(String label, ShortestPathResult r,
                                    String dst, Graph g) {
        Double d = r.dist().get(dst);
        List<String> path = r.path(dst);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) sb.append(" → ");
            sb.append(g.node(path.get(i)).name());
        }
        System.out.printf("  %-38s d=%.2f (relax=%d, pops=%d) : %s%n",
                label, d, r.opsCounter(), r.popsCounter(), sb);
    }

    private static void printWeightImpact(Graph g, String s, String t) {
        System.out.printf("Effect of weight function on optimal route %s → %s:%n", s, t);

        ShortestPathResult byDist = DijkstraTransport.run(g, s, t,
                Graph::byDistance, DijkstraTransport.EdgeVCRatio.NONE, 1.0);
        ShortestPathResult byTime = DijkstraTransport.run(g, s, t,
                Graph::byFreeFlowTime, DijkstraTransport.EdgeVCRatio.NONE, 1.0);
        ShortestPathResult byPeak = DijkstraTransport.run(g, s, t,
                e -> Graph.byBPRTime(e, 0.8),
                DijkstraTransport.EdgeVCRatio.NONE, 1.0);

        System.out.printf("  by distance      : %.2f km, path = %s%n",
                byDist.dist().get(t),
                pathStr(byDist.path(t), g));
        System.out.printf("  by free-flow time: %.2f min, path = %s%n",
                byTime.dist().get(t),
                pathStr(byTime.path(t), g));
        System.out.printf("  by BPR @ v/c=0.8 : %.2f min, path = %s%n",
                byPeak.dist().get(t),
                pathStr(byPeak.path(t), g));
    }

    private static String pathStr(List<String> path, Graph g) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) sb.append(" → ");
            sb.append(g.node(path.get(i)).name());
        }
        return sb.toString();
    }
}
