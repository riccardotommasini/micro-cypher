package fr.cnrs.liris.insa.cypher.demo;

import fr.cnrs.liris.insa.cypher.model.PGraph;
import fr.cnrs.liris.insa.cypher.model.PGraphBuilder;
import fr.cnrs.liris.insa.cypher.runtime.GraphSnapshot;
import fr.cnrs.liris.insa.cypher.runtime.GraphSnapshotBuilder;

import java.util.List;
import java.util.Map;

public final class GraphBuildExample {

    public static void main(String[] args) {
        PGraph rentals = PGraphBuilder.create()
                .addNode(1, List.of("Bike"), Map.of("bike_id", 1))
                .addNode(2, List.of("Station"), Map.of("station_id", 2))
                .addEdge(1, 2, List.of("rentedAt"), Map.of("permitted", "compliant"))
                .build();

        PGraph location = PGraphBuilder.create()
                .addNode(2, List.of("Station"), Map.of("station_id", 2))
                .addNode(3, List.of("City"), Map.of("city_id", 3))
                .addEdge(2, 3, List.of("locatedIn"), Map.of("region", "north"))
                .build();

        PGraph merged = PGraphBuilder.union(rentals, location);

        GraphSnapshot snapshot = GraphSnapshotBuilder.create()
                .merge(merged)
                .addNode(4, List.of("Station"), Map.of("station_id", 4))
                .addEdge(1, 4, List.of("rentedAt"), Map.of("permitted", "manual_review"))
                .build();

        System.out.println("Merged PGraph");
        System.out.println("nodes=" + merged.nodes().length + ", edges=" + merged.edges().length);

        System.out.println("Incrementally built GraphSnapshot");
        snapshot.allNodes().forEach(System.out::println);
        snapshot.relationships().forEach(System.out::println);
    }
}
