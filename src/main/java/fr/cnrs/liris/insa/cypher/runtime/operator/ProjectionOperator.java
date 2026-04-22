package fr.cnrs.liris.insa.cypher.runtime.operator;

import fr.cnrs.liris.insa.cypher.runtime.GraphExecutionContext;
import fr.cnrs.liris.insa.cypher.runtime.GraphRecord;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public final class ProjectionOperator extends AbstractCypherOperator {

    private final CypherOperator source;
    private final LinkedHashMap<String, BiFunction<GraphExecutionContext, GraphRecord, Object>> expressions;

    public ProjectionOperator(
            CypherOperator source,
            Map<String, BiFunction<GraphExecutionContext, GraphRecord, Object>> expressions
    ) {
        this.source = source;
        this.expressions = new LinkedHashMap<>(expressions);
    }

    @Override
    public String operatorType() {
        return "Projection";
    }

    @Override
    public String details() {
        return String.join(", ", expressions.keySet());
    }

    @Override
    public List<CypherOperator> children() {
        return List.of(source);
    }

    @Override
    public List<GraphRecord> execute(GraphExecutionContext context) {
        return source.execute(context).stream()
                .map(record -> project(context, record))
                .toList();
    }

    private GraphRecord project(GraphExecutionContext context, GraphRecord input) {
        GraphRecord output = GraphRecord.empty();
        for (Map.Entry<String, BiFunction<GraphExecutionContext, GraphRecord, Object>> entry : expressions.entrySet()) {
            output = output.with(entry.getKey(), entry.getValue().apply(context, input));
        }
        return output;
    }
}
