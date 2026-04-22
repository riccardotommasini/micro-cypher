package fr.cnrs.liris.insa.cypher.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PGraphBuilderTest {

    @Test
    void unionMergesCompatibleNodesAndEdges() {
        PGraph left = PGraphBuilder.create()
                .addNode(1, List.of("Bike"), Map.of("bike_id", 1))
                .addNode(2, List.of("Station"), Map.of("station_id", 2))
                .addEdge(1, 2, List.of("rentedAt"), Map.of("permitted", "compliant"))
                .build();

        PGraph right = PGraphBuilder.create()
                .addNode(1, List.of("Vehicle"), Map.of("bike_id", 1))
                .addNode(3, List.of("City"), Map.of("city_id", 3))
                .addEdge(2, 3, List.of("locatedIn"), Map.of("region", "north"))
                .build();

        PGraph merged = PGraphBuilder.union(left, right);

        assertEquals(3, merged.nodes().length);
        assertEquals(2, merged.edges().length);
        assertEquals(List.of("Bike", "Vehicle"), List.of(merged.nodes()[0].labels()));
    }

    @Test
    void unionRejectsConflictingProperties() {
        PGraph left = PGraphBuilder.create()
                .addNode(1, List.of("Bike"), Map.of("bike_id", 1))
                .build();
        PGraph right = PGraphBuilder.create()
                .addNode(1, List.of("Bike"), Map.of("bike_id", 99))
                .build();

        assertThrows(IllegalArgumentException.class, () -> PGraphBuilder.union(left, right));
    }
}
