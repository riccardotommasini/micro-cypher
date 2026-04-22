package fr.cnrs.liris.insa.cypher.runtime.operator;

import fr.cnrs.liris.insa.cypher.runtime.GraphExecutionContext;
import fr.cnrs.liris.insa.cypher.runtime.GraphRecord;

import java.util.List;

public final class AllNodesScanOperator extends AbstractCypherOperator {

    private final String variable;

    public AllNodesScanOperator(String variable) {
        this.variable = variable;
    }

    @Override
    public String operatorType() {
        return "AllNodesScan";
    }

    @Override
    public String details() {
        return variable;
    }

    @Override
    public List<GraphRecord> execute(GraphExecutionContext context) {
        return context.graph().allNodes().stream()
                .map(node -> GraphRecord.empty().with(variable, node))
                .toList();
    }
}
