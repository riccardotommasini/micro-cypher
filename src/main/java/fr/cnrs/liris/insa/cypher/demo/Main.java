package fr.cnrs.liris.insa.cypher.demo;

import fr.cnrs.liris.insa.cypher.frontend.CypherFrontend;
import fr.cnrs.liris.insa.cypher.frontend.dsl.Query;
import fr.cnrs.liris.insa.cypher.model.PGraph;
import fr.cnrs.liris.insa.cypher.model.PGraphImpl;
import fr.cnrs.liris.insa.cypher.neo4j.ClearDatabaseOperator;
import fr.cnrs.liris.insa.cypher.neo4j.ExecuteQueryOperator;
import fr.cnrs.liris.insa.cypher.neo4j.ImportGraphOperator;
import fr.cnrs.liris.insa.cypher.runtime.GraphExecutionContext;
import fr.cnrs.liris.insa.cypher.runtime.GraphSnapshot;
import fr.cnrs.liris.insa.cypher.runtime.operator.CypherOperator;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.test.TestDatabaseManagementServiceBuilder;

import java.io.FileNotFoundException;
import java.io.FileReader;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        String query = "MATCH (n)-[p]->(m) RETURN *";
        String parsedQuery = "MATCH (n)-[p:rentedAt]->(m) WHERE p.permitted = 'compliant' RETURN n, p, m";
        PGraph g = PGraphImpl.fromJson(new FileReader("src/main/resources/testGraph1.json"));
        GraphSnapshot snapshot = GraphSnapshot.from(g);
        CypherFrontend frontend = new CypherFrontend();
        CypherOperator compiledPlan = frontend.compile(parsedQuery);
        Query fluentQuery = Query.match(
                        Query.pattern(Query.node("n", "Bike"))
                                .out(Query.rel("p").type("rentedAt"), Query.node("m", "Station"))
                )
                .returning("n", "p", "m");

        System.out.println("Parsed query operator tree");
        System.out.println(compiledPlan.explain());
        new GraphExecutionContext(snapshot).execute(compiledPlan).forEach(System.out::println);
        System.out.println("Fluent query rows");
        frontend.execute(snapshot, fluentQuery).forEach(System.out::println);

        TestDatabaseManagementServiceBuilder builder = new TestDatabaseManagementServiceBuilder();
        DatabaseManagementService dbm = builder.impermanent().build();

        try {
            GraphDatabaseService db = dbm.database(GraphDatabaseSettings.DEFAULT_DATABASE_NAME);

            new ClearDatabaseOperator().execute(db);
            new ImportGraphOperator(g).execute(db);
            new ExecuteQueryOperator(query).execute(db).forEach(System.out::println);

            System.out.println("Job Done!");
        } finally {
            dbm.shutdown();
        }
    }
}
