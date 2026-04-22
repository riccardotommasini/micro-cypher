package fr.cnrs.liris.insa.cypher.runtime.operator;

import fr.cnrs.liris.insa.cypher.runtime.GraphExecutionContext;
import fr.cnrs.liris.insa.cypher.runtime.GraphRecord;
import fr.cnrs.liris.insa.cypher.runtime.value.NodeValue;
import fr.cnrs.liris.insa.cypher.runtime.value.RelationshipDirection;

import java.util.List;

public final class VarLengthExpandOperator extends AbstractCypherOperator {

    private final CypherOperator source;
    private final String fromVariable;
    private final String pathVariable;
    private final String toVariable;
    private final RelationshipDirection direction;
    private final String relationshipType;
    private final int minHops;
    private final int maxHops;

    public VarLengthExpandOperator(
            CypherOperator source,
            String fromVariable,
            String pathVariable,
            String toVariable,
            RelationshipDirection direction,
            String relationshipType,
            int minHops,
            int maxHops
    ) {
        this.source = source;
        this.fromVariable = fromVariable;
        this.pathVariable = pathVariable;
        this.toVariable = toVariable;
        this.direction = direction;
        this.relationshipType = relationshipType;
        this.minHops = minHops;
        this.maxHops = maxHops;
    }

    @Override
    public String operatorType() {
        return "VarLengthExpand";
    }

    @Override
    public String details() {
        String typeSuffix = relationshipType == null ? "*" : ":" + relationshipType + "*";
        return "(" + fromVariable + ")-[" + pathVariable + typeSuffix + minHops + ".." + maxHops + "]" + direction.arrow() + "(" + toVariable + ")";
    }

    @Override
    public List<CypherOperator> children() {
        return List.of(source);
    }

    @Override
    public List<GraphRecord> execute(GraphExecutionContext context) {
        return source.execute(context).stream()
                .flatMap(record -> expandRecord(context, record).stream())
                .toList();
    }

    private List<GraphRecord> expandRecord(GraphExecutionContext context, GraphRecord record) {
        Object value = record.get(fromVariable);
        if (!(value instanceof NodeValue start)) {
            return List.of();
        }

        return context.graph().expandPaths(start, direction, relationshipType, minHops, maxHops).stream()
                .map(path -> record.with(pathVariable, path).with(toVariable, path.end()))
                .toList();
    }
}
