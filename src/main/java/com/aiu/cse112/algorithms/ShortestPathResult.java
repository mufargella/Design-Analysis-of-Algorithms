package com.aiu.cse112.algorithms;

import java.util.*;

/**
 * Result of a single-source shortest-path computation.
 *
 * <p>{@code dist.get(v)} is the length of the shortest path from the
 * source to {@code v}, or {@code Double.POSITIVE_INFINITY} if {@code v}
 * is unreachable. {@code prev.get(v)} is the predecessor of {@code v}
 * on a shortest path; the source maps to {@code null}.</p>
 *
 * <p>{@link #path(String)} reconstructs an explicit list of vertices
 * by walking the {@code prev} pointers from {@code target} back to
 * the source.</p>
 *
 * @param dist  shortest distances from the source.
 * @param prev  predecessor pointers (null at the source).
 * @param opsCounter   number of edge relaxations performed (for benchmarks).
 * @param popsCounter  number of priority-queue extractions performed.
 */
public record ShortestPathResult(
        Map<String, Double> dist,
        Map<String, String> prev,
        long opsCounter,
        long popsCounter
) {

    /** Reconstruct the path source → ... → target. Empty list if unreachable. */
    public List<String> path(String target) {
        if (!prev.containsKey(target) && !dist.containsKey(target))
            return Collections.emptyList();
        Double d = dist.get(target);
        if (d == null || Double.isInfinite(d)) return Collections.emptyList();

        Deque<String> stack = new ArrayDeque<>();
        String cur = target;
        // bound the loop length to V to be safe against stale data
        for (int i = 0; i <= dist.size() && cur != null; i++) {
            stack.push(cur);
            cur = prev.get(cur);
        }
        return new ArrayList<>(stack);
    }
}
