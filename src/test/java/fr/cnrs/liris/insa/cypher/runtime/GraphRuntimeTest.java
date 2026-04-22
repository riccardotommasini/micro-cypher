package fr.cnrs.liris.insa.cypher.runtime;

import fr.cnrs.liris.insa.cypher.model.PGraphImpl;
import fr.cnrs.liris.insa.cypher.runtime.operator.*;
import fr.cnrs.liris.insa.cypher.runtime.value.NodeValue;
import fr.cnrs.liris.insa.cypher.runtime.value.PathValue;
import fr.cnrs.liris.insa.cypher.runtime.value.RelationshipDirection;
import fr.cnrs.liris.insa.cypher.runtime.value.RelationshipValue;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.*;

class GraphRuntimeTest {

    @Test
    void allNodesScanMatchesMatchNReturnN() throws FileNotFoundException {
        List<GraphRecord> rows = execute("testGraph1.json", new ProduceResultsOperator(
                new AllNodesScanOperator("n"),
                List.of("n")
        ));

        assertEquals(2, rows.size());
        assertEquals(
                Set.of("{n=Node(1:Station)}", "{n=Node(5:Bike)}"),
                rows.stream().map(GraphRecord::toString).collect(java.util.stream.Collectors.toSet())
        );
    }

    @Test
    void labelScansMatchBikeAndStationQueries() throws FileNotFoundException {
        List<GraphRecord> bikeRows = execute("testGraph1.json", new ProduceResultsOperator(
                new NodeByLabelScanOperator("n", "Bike"),
                List.of("n")
        ));
        List<GraphRecord> stationRows = execute("testGraph1.json", new ProduceResultsOperator(
                new NodeByLabelScanOperator("n", "Station"),
                List.of("n")
        ));

        assertEquals(List.of("{n=Node(5:Bike)}"), bikeRows.stream().map(GraphRecord::toString).toList());
        assertEquals(List.of("{n=Node(1:Station)}"), stationRows.stream().map(GraphRecord::toString).toList());
    }

    @Test
    void singleHopOutgoingQueryMatchesFixture() throws FileNotFoundException {
        List<GraphRecord> rows = execute("testGraph1.json", new ProduceResultsOperator(
                new ExpandAllOperator(new AllNodesScanOperator("n"), "n", "p", "m", RelationshipDirection.OUTGOING, null),
                List.of("n", "p", "m")
        ));

        assertEquals(List.of("{n=Node(5:Bike), p=Relationship(0:5-[rentedAt]->1), m=Node(1:Station)}"),
                rows.stream().map(GraphRecord::toString).toList());
    }

    @Test
    void singleHopIncomingQueryMatchesFixture() throws FileNotFoundException {
        List<GraphRecord> rows = execute("testGraph1.json", new ProduceResultsOperator(
                new ExpandAllOperator(new AllNodesScanOperator("n"), "n", "p", "m", RelationshipDirection.INCOMING, null),
                List.of("n", "p", "m")
        ));

        assertEquals(List.of("{n=Node(1:Station), p=Relationship(0:5-[rentedAt]->1), m=Node(5:Bike)}"),
                rows.stream().map(GraphRecord::toString).toList());
    }

    @Test
    void relationshipTypeAndPropertyFiltersMatchQueryCorpus() throws FileNotFoundException {
        CypherOperator typedExpand = new ExpandAllOperator(
                new AllNodesScanOperator("n"),
                "n",
                "p",
                "m",
                RelationshipDirection.OUTGOING,
                "rentedAt"
        );
        CypherOperator compliantOnly = new FilterOperator(typedExpand, new GraphPredicate() {
            @Override
            public boolean test(GraphExecutionContext context, GraphRecord record) {
                return "compliant".equals(((RelationshipValue) record.get("p")).property("permitted"));
            }

            @Override
            public String description() {
                return "p.permitted = 'compliant'";
            }
        });
        List<GraphRecord> compliantRows = execute("testGraph1.json", new ProduceResultsOperator(compliantOnly, List.of("n", "p", "m")));

        assertEquals(1, compliantRows.size());
        assertEquals("{n=Node(5:Bike), p=Relationship(0:5-[rentedAt]->1), m=Node(1:Station)}", compliantRows.getFirst().toString());
    }

    @Test
    void labelAndTypeConstrainedQueryReturnsExpectedRow() throws FileNotFoundException {
        CypherOperator plan = new ProduceResultsOperator(
                new ExpandAllOperator(new NodeByLabelScanOperator("n", "Bike"), "n", "p", "m", RelationshipDirection.OUTGOING, "rentedAt"),
                List.of("n", "p", "m")
        );

        List<GraphRecord> rows = execute("testGraph1.json", plan).stream()
                .filter(row -> ((NodeValue) row.get("m")).hasLabel("Station"))
                .toList();

        assertEquals(1, rows.size());
        assertEquals("{n=Node(5:Bike), p=Relationship(0:5-[rentedAt]->1), m=Node(1:Station)}", rows.getFirst().toString());
    }

    @Test
    void impossibleStationToRentedAtPatternReturnsNoRows() throws FileNotFoundException {
        CypherOperator plan = new ProduceResultsOperator(
                new ExpandAllOperator(new NodeByLabelScanOperator("n", "Station"), "n", "p", "m", RelationshipDirection.OUTGOING, "rentedAt"),
                List.of("n", "p", "m")
        );

        assertTrue(execute("testGraph1.json", plan).isEmpty());
    }

    @Test
    void varLengthExpandSupportsDirectedBoundedPaths() throws FileNotFoundException {
        CypherOperator plan = new ProduceResultsOperator(
                new VarLengthExpandOperator(new AllNodesScanOperator("a"), "a", "p", "b", RelationshipDirection.OUTGOING, null, 1, 2),
                List.of("p")
        );

        List<GraphRecord> rows = execute("testGraphPaths.json", plan);
        assertEquals(8, rows.size());
        assertEquals(8, rows.stream().map(GraphRecord::toString).collect(java.util.stream.Collectors.toSet()).size());
    }

    @Test
    void typedVarLengthExpandSupportsRentedAtOnly() throws FileNotFoundException {
        CypherOperator plan = new ProduceResultsOperator(
                new VarLengthExpandOperator(new NodeByLabelScanOperator("a", "Bike"), "a", "p", "b", RelationshipDirection.OUTGOING, "rentedAt", 1, 2),
                List.of("p")
        );

        List<String> rows = execute("testGraphPaths.json", plan).stream().map(GraphRecord::toString).toList();
        assertEquals(3, rows.size());
        assertTrue(rows.stream().allMatch(row -> row.contains("rentedAt")));
    }

    @Test
    void exactTypedPathCanBeBuiltFromChainedExpands() throws FileNotFoundException {
        CypherOperator firstHop = new ExpandAllOperator(
                new AllNodesScanOperator("a"),
                "a",
                "r1",
                "b",
                RelationshipDirection.OUTGOING,
                "rentedAt"
        );
        CypherOperator secondHop = new ExpandAllOperator(
                firstHop,
                "b",
                "r2",
                "c",
                RelationshipDirection.OUTGOING,
                "locatedIn"
        );
        Map<String, BiFunction<GraphExecutionContext, GraphRecord, Object>> projection = new LinkedHashMap<>();
        projection.put("p", (context, record) -> new PathValue(
                List.of(
                        (NodeValue) record.get("a"),
                        (NodeValue) record.get("b"),
                        (NodeValue) record.get("c")
                ),
                List.of(
                        (RelationshipValue) record.get("r1"),
                        (RelationshipValue) record.get("r2")
                )
        ));

        List<GraphRecord> rows = execute("testGraphPaths.json", new ProduceResultsOperator(
                new ProjectionOperator(secondHop, projection),
                List.of("p")
        ));

        assertEquals(3, rows.size());
        assertTrue(rows.stream().allMatch(row -> row.toString().contains("locatedIn")));
    }

    @Test
    void boundedRangeAndUnknownTypeBehaveAsExpected() throws FileNotFoundException {
        List<GraphRecord> rangeRows = execute("testGraphPaths.json", new ProduceResultsOperator(
                new VarLengthExpandOperator(new AllNodesScanOperator("a"), "a", "p", "b", RelationshipDirection.OUTGOING, null, 2, 3),
                List.of("p")
        ));
        List<GraphRecord> unknownTypeRows = execute("testGraphPaths.json", new ProduceResultsOperator(
                new VarLengthExpandOperator(new AllNodesScanOperator("a"), "a", "p", "b", RelationshipDirection.OUTGOING, "unknownType", 1, 2),
                List.of("p")
        ));

        assertEquals(3, rangeRows.size());
        assertTrue(unknownTypeRows.isEmpty());
    }

    @Test
    void pathFilteringAndReturningEndpointsWorks() throws FileNotFoundException {
        CypherOperator expand = new VarLengthExpandOperator(
                new AllNodesScanOperator("a"),
                "a",
                "p",
                "b",
                RelationshipDirection.OUTGOING,
                null,
                1,
                2
        );
        CypherOperator filtered = new FilterOperator(expand, new GraphPredicate() {
            @Override
            public boolean test(GraphExecutionContext context, GraphRecord record) {
                return !record.get("a").equals(record.get("b"));
            }

            @Override
            public String description() {
                return "a <> b";
            }
        });

        List<GraphRecord> rows = execute("testGraphPaths.json", new ProduceResultsOperator(filtered, List.of("a", "b", "p")));

        assertEquals(8, rows.size());
        GraphRecord first = rows.getFirst();
        assertInstanceOf(NodeValue.class, first.get("a"));
        assertInstanceOf(NodeValue.class, first.get("b"));
        assertInstanceOf(PathValue.class, first.get("p"));
        assertFalse(first.get("a").equals(first.get("b")));
    }

    @Test
    void graphSnapshotProvidesLookupAndTypedAccess() throws FileNotFoundException {
        GraphSnapshot snapshot = snapshot("testGraphPaths.json");

        NodeValue bike = snapshot.nodeById(3);
        RelationshipValue relationship = snapshot.relationshipById(0);

        assertNotNull(bike);
        assertNotNull(relationship);
        assertTrue(bike.hasLabel("Bike"));
        assertEquals("rentedAt", relationship.type());
        assertEquals(2, snapshot.outgoingRelationships(bike, "rentedAt").size());
    }

    @Test
    void graphSnapshotBuilderBuildsIncrementally() {
        GraphSnapshot snapshot = GraphSnapshotBuilder.create()
                .addNode(1, List.of("Bike"), Map.of("bike_id", 1))
                .addNode(2, List.of("Station"), Map.of("station_id", 2))
                .addEdge(1, 2, List.of("rentedAt"), Map.of("permitted", "compliant"))
                .build();

        assertEquals(2, snapshot.allNodes().size());
        assertEquals(1, snapshot.relationships().size());
        assertEquals(1, snapshot.outgoingRelationships(snapshot.nodeById(1), "rentedAt").size());
    }

    @Test
    void graphSnapshotBuilderCanMergeExistingGraphs() throws FileNotFoundException {
        GraphSnapshot snapshot = GraphSnapshotBuilder.create()
                .merge(snapshot("testGraph1.json"))
                .merge(snapshot("testGraphPaths.json"))
                .build();

        assertTrue(snapshot.allNodes().size() >= 5);
        assertTrue(snapshot.relationships().size() >= 5);
    }

    @Test
    void runtimeSourcesNoLongerUsePGraphDirectly() throws Exception {
        assertNoPGraphReference("src/main/java/fr/cnrs/liris/insa/cypher/runtime/GraphExecutionContext.java");
        assertNoPGraphReference("src/main/java/fr/cnrs/liris/insa/cypher/runtime/operator/AllNodesScanOperator.java");
        assertNoPGraphReference("src/main/java/fr/cnrs/liris/insa/cypher/runtime/operator/NodeByLabelScanOperator.java");
        assertNoPGraphReference("src/main/java/fr/cnrs/liris/insa/cypher/runtime/operator/ExpandAllOperator.java");
        assertNoPGraphReference("src/main/java/fr/cnrs/liris/insa/cypher/runtime/operator/VarLengthExpandOperator.java");
    }

    private static List<GraphRecord> execute(String resourceName, CypherOperator plan) throws FileNotFoundException {
        return new GraphExecutionContext(snapshot(resourceName)).execute(plan);
    }

    private static GraphSnapshot snapshot(String resourceName) throws FileNotFoundException {
        return GraphSnapshot.from(PGraphImpl.fromJson(new FileReader(java.nio.file.Path.of("src/main/resources", resourceName).toFile())));
    }

    private static void assertNoPGraphReference(String file) throws Exception {
        String source = Files.readString(java.nio.file.Path.of(file));
        assertFalse(source.contains("PGraph"), "Expected no direct PGraph reference in " + file);
    }
}
