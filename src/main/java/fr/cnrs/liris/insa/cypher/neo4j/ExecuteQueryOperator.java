package fr.cnrs.liris.insa.cypher.neo4j;

import org.neo4j.graphdb.Result;
import org.neo4j.graphdb.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExecuteQueryOperator extends TransactionalGraphOperator<List<Map<String, Object>>> {

    private final String query;

    public ExecuteQueryOperator(String query) {
        super(false);
        this.query = query;
    }

    @Override
    protected List<Map<String, Object>> run(Transaction tx) {
        List<Map<String, Object>> rows = new ArrayList<>();
        Result result = tx.execute(query);
        while (result.hasNext()) {
            rows.add(result.next());
        }
        return rows;
    }
}
