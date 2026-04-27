package com.aiu.cse112.graph;

/**
 * A vertex of the Cairo transportation graph.
 *
 * <p>A {@code Node} can represent either a neighborhood/district
 * (numeric ID such as {@code "3"} for Downtown Cairo) or an
 * "important facility" (ID prefixed with F, such as {@code "F1"}
 * for Cairo International Airport). The data layout follows the
 * tables given in <em>CSE112-Project_Provided_Data.pdf</em>.</p>
 *
 * <p>The geographic coordinates ({@code x} = longitude, {@code y}
 * = latitude) are used to compute the great-circle (haversine)
 * distance, which serves as an admissible heuristic for the A*
 * comparison in {@code AStar}.</p>
 */
public final class Node {

    private final String id;
    private final String name;
    private final double x;        // longitude
    private final double y;        // latitude
    private final int population;  // 0 for facilities
    private final String kind;     // Residential / Mixed / Business / ...

    public Node(String id, String name, double x, double y,
                int population, String kind) {
        this.id = id;
        this.name = name;
        this.x = x;
        this.y = y;
        this.population = population;
        this.kind = kind;
    }

    public String id()       { return id; }
    public String name()     { return name; }
    public double x()        { return x; }
    public double y()        { return y; }
    public int population()  { return population; }
    public String kind()     { return kind; }

    /**
     * Great-circle distance to another node, in kilometers.
     *
     * <p>Used as the default A* heuristic. It is admissible whenever
     * the edge weight is also a length in kilometers, because no
     * road can be shorter than the straight line between its
     * endpoints on the Earth's surface.</p>
     */
    public double haversineKm(Node other) {
        final double R = 6371.0; // Earth radius in km
        double lat1 = Math.toRadians(this.y);
        double lat2 = Math.toRadians(other.y);
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(other.x - this.x);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(lat1) * Math.cos(lat2)
                   * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * R * Math.asin(Math.sqrt(a));
    }

    @Override
    public String toString() {
        return String.format("Node{%s, %s}", id, name);
    }
}
