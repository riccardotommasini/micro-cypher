package fr.cnrs.liris.insa.cypher.runtime.operator;

import fr.cnrs.liris.insa.cypher.runtime.GraphExecutionContext;
import fr.cnrs.liris.insa.cypher.runtime.GraphRecord;

import java.util.List;

public final class ProduceResultsOperator extends AbstractCypherOperator {

    private final CypherOperator source;
    private final List<String> columns;

    public ProduceResultsOperator(CypherOperator source, List<String> columns) {
        this.source = source;
        this.columns = List.copyOf(columns);
    }

    @Override
    public String operatorType() {
        return "ProduceResults";
    }

    @Override
    public String details() {
        return String.join(", ", columns);
    }

    @Override
    public List<CypherOperator> children() {
        return List.of(source);
    }

    @Override
    public List<GraphRecord> execute(GraphExecutionContext context) {
        return source.execute(context).stream()
                .map(record -> record.project(columns))
                .toList();
    }
}
