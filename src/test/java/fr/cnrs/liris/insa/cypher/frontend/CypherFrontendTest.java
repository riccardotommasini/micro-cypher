package fr.cnrs.liris.insa.cypher.frontend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import fr.cnrs.liris.insa.cypher.frontend.ast.ExpressionAst;
import fr.cnrs.liris.insa.cypher.frontend.ast.QueryAst;
import fr.cnrs.liris.insa.cypher.frontend.dsl.Query;
import fr.cnrs.liris.insa.cypher.frontend.plan.LogicalPlan;
import fr.cnrs.liris.insa.cypher.frontend.plan.LogicalPlanJson;
import fr.cnrs.liris.insa.cypher.model.PGraphImpl;
import fr.cnrs.liris.insa.cypher.runtime.GraphRecord;
import fr.cnrs.liris.insa.cypher.runtime.GraphSnapshot;
import org.junit.jupiter.api.Test;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CypherFrontendTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final CypherFrontend FRONTEND = new CypherFrontend();

    private static List<String> supportedQueries() {
        return List.of(
                "MATCH (n) RETURN n",
                "MATCH (n:Bike) RETURN n",
                "MATCH (n)-[p]->(m) RETURN n, p, m",
                "MATCH (n)<-[p]-(m) RETURN n, p, m",
                "MATCH (n)-[p:rentedAt]->(m) WHERE p.permitted = 'compliant' RETURN n, p, m",
                "MATCH p = (a)-[*1..2]->(b) RETURN p",
                "MATCH p = (a)-[:rentedAt]->(b)-[:locatedIn]->(c) RETURN p",
                "MATCH p = (a)-[*1..2]->(b) WHERE a <> b RETURN a, b, p"
        );
    }

    private static GraphSnapshot snapshot(String resourceName) throws FileNotFoundException {
        return GraphSnapshot.from(PGraphImpl.fromJson(new FileReader(java.nio.file.Path.of("src/main/resources", resourceName).toFile())));
    }

    @Test
    void parserAcceptsSupportedQueryCorpus() {
        supportedQueries().forEach(query -> {
            QueryAst ast = FRONTEND.parse(query);
            assertEquals("MATCH", "MATCH");
            assertTrue(ast.returnClause().items().size() >= 1);
        });
    }

    @Test
    void parserRejectsUnsupportedQueries() {
        List<String> unsupported = List.of(
                "MATCH (n), (m) RETURN n, m",
                "MATCH (n) WITH n RETURN n",
                "MATCH (n) RETURN n ORDER BY n",
                "MATCH (n) RETURN n LIMIT 1",
                "OPTIONAL MATCH (n) RETURN n",
                "CREATE (n) RETURN n",
                "MATCH p = (a)-[*]->(b) RETURN p",
                "MATCH p = (a)-[*3]->(b) RETURN p"
        );

        unsupported.forEach(query -> assertThrows(IllegalArgumentException.class, () -> FRONTEND.parse(query)));
    }

    @Test
    void plannerBuildsExpectedLogicalPlanShape() {
        LogicalPlan plan = FRONTEND.plan("MATCH (n:Bike)-[p:rentedAt]->(m:Station) RETURN n, p, m");

        assertInstanceOf(LogicalPlan.ProduceResultsPlan.class, plan);
        LogicalPlan.ProduceResultsPlan produceResults = (LogicalPlan.ProduceResultsPlan) plan;
        assertEquals(List.of("n", "p", "m"), produceResults.columns());
        assertInstanceOf(LogicalPlan.ProjectionPlan.class, produceResults.source());
        LogicalPlan.ProjectionPlan projection = (LogicalPlan.ProjectionPlan) produceResults.source();
        assertInstanceOf(LogicalPlan.ExpandAllPlan.class, projection.source());
        LogicalPlan.ExpandAllPlan expand = (LogicalPlan.ExpandAllPlan) projection.source();
        assertEquals("n", expand.fromVariable());
        assertEquals("p", expand.relationshipVariable());
        assertEquals("m", expand.toVariable());
        assertEquals("rentedAt", expand.relationshipType());
        assertInstanceOf(LogicalPlan.NodeByLabelScanPlan.class, expand.source());
    }

    @Test
    void logicalPlanJsonMatchesSchema() throws Exception {
        LogicalPlan plan = FRONTEND.plan("MATCH p = (a)-[*1..2]->(b) WHERE a <> b RETURN a, b, p");
        LogicalPlanJson serializer = new LogicalPlanJson();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        JsonSchema schema = factory.getSchema(
                OBJECT_MAPPER.readTree(Files.readString(java.nio.file.Path.of("src/main/resources/logical-plan.schema.json")))
        );

        Set<com.networknt.schema.ValidationMessage> messages =
                schema.validate(OBJECT_MAPPER.readTree(serializer.toJson(plan).toString()));

        assertTrue(messages.isEmpty(), messages.toString());
    }

    @Test
    void parsedQueriesExecuteLikeTheCurrentRuntimePlans() throws FileNotFoundException {
        GraphSnapshot graph1 = snapshot("testGraph1.json");
        GraphSnapshot graph2 = snapshot("testGraphPaths.json");

        assertEquals(
                List.of("{n=Node(5:Bike), p=Relationship(0:5-[rentedAt]->1), m=Node(1:Station)}"),
                FRONTEND.execute(graph1, "MATCH (n)-[p]->(m) RETURN n, p, m").stream().map(GraphRecord::toString).toList()
        );
        assertEquals(
                8,
                FRONTEND.execute(graph2, "MATCH p = (a)-[*1..2]->(b) RETURN p").size()
        );
        assertEquals(
                3,
                FRONTEND.execute(graph2, "MATCH p = (a)-[:rentedAt]->(b)-[:locatedIn]->(c) RETURN p").size()
        );
    }

    @Test
    void parsedQueryAndBuilderProduceEquivalentPlans() {
        String queryText = "MATCH p = (a)-[:rentedAt]->(b)-[:locatedIn]->(c) RETURN p";
        Query query = Query.match(
                        Query.pattern(Query.node("a"))
                                .named("p")
                                .out(Query.rel().type("rentedAt"), Query.node("b"))
                                .out(Query.rel().type("locatedIn"), Query.node("c"))
                )
                .returning("p");

        LogicalPlanJson serializer = new LogicalPlanJson();
        String parsedJson = serializer.toJson(FRONTEND.plan(queryText)).toString();
        String builderJson = serializer.toJson(FRONTEND.plan(query)).toString();

        assertEquals(parsedJson, builderJson);
    }

    @Test
    void builderQueriesExecuteAndMatchParsedQueries() throws FileNotFoundException {
        GraphSnapshot graph = snapshot("testGraph1.json");
        Query fluent = Query.match(
                        Query.pattern(Query.node("n"))
                                .out(Query.rel("p"), Query.node("m"))
                )
                .returning("n", "p", "m");

        List<String> parsedRows = FRONTEND.execute(graph, "MATCH (n)-[p]->(m) RETURN n, p, m").stream()
                .map(GraphRecord::toString)
                .toList();
        List<String> builderRows = FRONTEND.execute(graph, fluent).stream()
                .map(GraphRecord::toString)
                .toList();

        assertEquals(parsedRows, builderRows);
    }

    @Test
    void parserProducesMeaningfulAstForNamedVarLengthPath() {
        QueryAst ast = FRONTEND.parse("MATCH p = (a)-[*1..2]->(b) WHERE a <> b RETURN a, b, p");

        assertEquals("p", ast.matchClause().pattern().pathVariable());
        assertEquals(1, ast.matchClause().pattern().steps().size());
        assertInstanceOf(ExpressionAst.Comparison.class, ast.where());
    }

    @Test
    void subsetGrammarExistsAlongsideReferenceGrammar() {
        assertTrue(Files.exists(java.nio.file.Path.of("src/main/antlr4/org/example/CypherSubset.g4")));
        assertTrue(Files.exists(java.nio.file.Path.of("src/main/resources/Cypher.g4")));
        assertNotEquals(Path.of("src/main/antlr4/org/example/CypherSubset.g4"), Path.of("src/main/resources/Cypher.g4"));
    }
}
