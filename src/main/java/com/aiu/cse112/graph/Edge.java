package com.aiu.cse112.graph;

/**
 * A directed edge of the transportation graph.
 *
 * <p>Each edge carries the three attributes provided by the dataset
 * (distance, capacity, condition) plus a {@code weight} field that
 * the algorithm currently runs on. Different weight functions can
 * be plugged in via {@link Graph#setWeightFunction}.</p>
 *
 * <p>Roads in Cairo are bidirectional, so the {@link Graph} class
 * inserts two directed Edge objects (u→v and v→u) for every road
 * read from {@code data/existing_roads.csv}.</p>
 */
public final class Edge {

    private final String src;
    private final String dst;
    private final double distanceKm;
    private final int capacity;          // vehicles / hour
    private final int condition;         // 1 (poor) – 10 (excellent)
    private final boolean potential;     // true for "potential new road" entries
    private final double constructionCost;  // Million EGP, only for potential roads

    /** The weight actually consumed by the shortest-path algorithm. */
    private double weight;

    public Edge(String src, String dst, double distanceKm,
                int capacity, int condition,
                boolean potential, double constructionCost) {
        this.src = src;
        this.dst = dst;
        this.distanceKm = distanceKm;
        this.capacity = capacity;
        this.condition = condition;
        this.potential = potential;
        this.constructionCost = constructionCost;
        this.weight = distanceKm;  // sensible default
    }

    public String src()                { return src; }
    public String dst()                { return dst; }
    public double distanceKm()         { return distanceKm; }
    public int capacity()              { return capacity; }
    public int condition()             { return condition; }
    public boolean isPotential()       { return potential; }
    public double constructionCost()   { return constructionCost; }
    public double weight()             { return weight; }

    public void setWeight(double w)    { this.weight = w; }

    @Override
    public String toString() {
        return String.format("%s→%s [w=%.3f, d=%.1fkm, cap=%d, cond=%d]",
                src, dst, weight, distanceKm, capacity, condition);
    }
}
