package fr.cnrs.liris.insa.cypher.runtime;

import fr.cnrs.liris.insa.cypher.runtime.operator.CypherOperator;

import java.util.List;

public final class GraphExecutionContext {

    private final GraphAccess graph;

    public GraphExecutionContext(GraphAccess graph) {
        this.graph = graph;
    }

    public List<GraphRecord> execute(CypherOperator operator) {
        return operator.execute(this);
    }

    public GraphAccess graph() {
        return graph;
    }
}
