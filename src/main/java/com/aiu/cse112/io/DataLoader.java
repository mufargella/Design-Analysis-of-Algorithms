package com.aiu.cse112.io;

import com.aiu.cse112.graph.Edge;
import com.aiu.cse112.graph.Graph;
import com.aiu.cse112.graph.Node;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Loads the Cairo transportation dataset from CSV files.
 *
 * <p>The CSV files are direct transcriptions of the tables in
 * {@code CSE112-Project_Provided_Data.pdf}. Loading is split into
 * three steps so callers can selectively include the 15 "potential
 * new roads" entries (which are needed for the MST/network-design
 * discussion but not for shortest-path benchmarking on the actual
 * built network).</p>
 */
public final class DataLoader {

    private static final Path DATA_DIR = Paths.get("data");

    private DataLoader() { /* utility class */ }

    /**
     * Build the full Cairo graph (15 neighborhoods + 10 facilities + 28 roads).
     *
     * @param includePotential if true, the 15 potential-new-road records
     *                         are added as well (with {@code isPotential=true}).
     */
    public static Graph loadGraph(boolean includePotential) throws IOException {
        Graph g = new Graph();

        // 1) Neighborhoods
        for (Map<String, String> row : readCsv(DATA_DIR.resolve("neighborhoods.csv"))) {
            g.addNode(new Node(
                    row.get("id"),
                    row.get("name"),
                    Double.parseDouble(row.get("x_coord")),
                    Double.parseDouble(row.get("y_coord")),
                    Integer.parseInt(row.get("population")),
                    row.get("type")
            ));
        }

        // 2) Facilities
        for (Map<String, String> row : readCsv(DATA_DIR.resolve("facilities.csv"))) {
            g.addNode(new Node(
                    row.get("id"),
                    row.get("name"),
                    Double.parseDouble(row.get("x_coord")),
                    Double.parseDouble(row.get("y_coord")),
                    0,
                    row.get("type")
            ));
        }

        // 3) Existing roads
        for (Map<String, String> row : readCsv(DATA_DIR.resolve("existing_roads.csv"))) {
            g.addRoad(
                    row.get("from_id"),
                    row.get("to_id"),
                    Double.parseDouble(row.get("distance_km")),
                    Integer.parseInt(row.get("capacity")),
                    Integer.parseInt(row.get("condition")),
                    false,
                    0.0
            );
        }

        // 4) Potential new roads (optional)
        if (includePotential) {
            for (Map<String, String> row : readCsv(DATA_DIR.resolve("potential_roads.csv"))) {
                g.addRoad(
                        row.get("from_id"),
                        row.get("to_id"),
                        Double.parseDouble(row.get("distance_km")),
                        Integer.parseInt(row.get("capacity")),
                        8,         // assume "new road" condition = 8
                        true,
                        Double.parseDouble(row.get("construction_cost_million_egp"))
                );
            }
        }

        // Set default weight function: distance in km
        g.setWeightFunction(Graph::byDistance);
        return g;
    }

    /**
     * Loads the per-road traffic flow table.
     *
     * @return {@code flow.get("u-v").get("morning_peak")}, etc. The keys
     *         are the road identifiers as written in the dataset
     *         (e.g. {@code "1-3"}, {@code "F1-2"}).
     */
    public static Map<String, Map<String, Integer>> loadTrafficFlow() throws IOException {
        Map<String, Map<String, Integer>> flow = new LinkedHashMap<>();
        for (Map<String, String> row : readCsv(DATA_DIR.resolve("traffic_flow.csv"))) {
            Map<String, Integer> rec = new HashMap<>();
            rec.put("morning_peak", Integer.parseInt(row.get("morning_peak")));
            rec.put("afternoon",    Integer.parseInt(row.get("afternoon")));
            rec.put("evening_peak", Integer.parseInt(row.get("evening_peak")));
            rec.put("night",        Integer.parseInt(row.get("night")));
            flow.put(row.get("road_id"), rec);
        }
        return flow;
    }

    // -----------------------------------------------------------------
    // Minimal CSV parser — handles quoted fields containing commas.
    // We intentionally avoid Apache Commons CSV to keep the project
    // dependency-free for the grader.
    // -----------------------------------------------------------------
    public static List<Map<String, String>> readCsv(Path file) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        try (BufferedReader br = Files.newBufferedReader(file)) {
            String header = br.readLine();
            if (header == null) return rows;
            String[] cols = splitCsvLine(header);
            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] vals = splitCsvLine(line);
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < cols.length && i < vals.length; i++) {
                    row.put(cols[i].trim(), vals[i].trim());
                }
                rows.add(row);
            }
        }
        return rows;
    }

    private static String[] splitCsvLine(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }
}
