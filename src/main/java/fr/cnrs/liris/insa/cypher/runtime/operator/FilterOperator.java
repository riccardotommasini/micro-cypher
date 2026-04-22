package fr.cnrs.liris.insa.cypher.runtime.operator;

import fr.cnrs.liris.insa.cypher.runtime.GraphExecutionContext;
import fr.cnrs.liris.insa.cypher.runtime.GraphPredicate;
import fr.cnrs.liris.insa.cypher.runtime.GraphRecord;

import java.util.List;

public final class FilterOperator extends AbstractCypherOperator {

    private final CypherOperator source;
    private final GraphPredicate predicate;

    public FilterOperator(CypherOperator source, GraphPredicate predicate) {
        this.source = source;
        this.predicate = predicate;
    }

    @Override
    public String operatorType() {
        return "Filter";
    }

    @Override
    public String details() {
        return predicate.description();
    }

    @Override
    public List<CypherOperator> children() {
        return List.of(source);
    }

    @Override
    public List<GraphRecord> execute(GraphExecutionContext context) {
        return source.execute(context).stream()
                .filter(record -> predicate.test(context, record))
                .toList();
    }
}
