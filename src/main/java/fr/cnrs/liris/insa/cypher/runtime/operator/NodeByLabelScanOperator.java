package fr.cnrs.liris.insa.cypher.runtime.operator;

import fr.cnrs.liris.insa.cypher.runtime.GraphExecutionContext;
import fr.cnrs.liris.insa.cypher.runtime.GraphRecord;

import java.util.List;

public final class NodeByLabelScanOperator extends AbstractCypherOperator {

    private final String variable;
    private final String label;

    public NodeByLabelScanOperator(String variable, String label) {
        this.variable = variable;
        this.label = label;
    }

    @Override
    public String operatorType() {
        return "NodeByLabelScan";
    }

    @Override
    public String details() {
        return variable + ":" + label;
    }

    @Override
    public List<GraphRecord> execute(GraphExecutionContext context) {
        return context.graph().nodesWithLabel(label).stream()
                .map(node -> GraphRecord.empty().with(variable, node))
                .toList();
    }
}
