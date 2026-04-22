package fr.cnrs.liris.insa.cypher.runtime.operator;

import fr.cnrs.liris.insa.cypher.runtime.GraphExecutionContext;
import fr.cnrs.liris.insa.cypher.runtime.GraphRecord;
import fr.cnrs.liris.insa.cypher.runtime.value.NodeValue;
import fr.cnrs.liris.insa.cypher.runtime.value.RelationshipDirection;
import fr.cnrs.liris.insa.cypher.runtime.value.RelationshipValue;

import java.util.List;

public final class ExpandAllOperator extends AbstractCypherOperator {

    private final CypherOperator source;
    private final String fromVariable;
    private final String relationshipVariable;
    private final String toVariable;
    private final RelationshipDirection direction;
    private final String relationshipType;

    public ExpandAllOperator(
            CypherOperator source,
            String fromVariable,
            String relationshipVariable,
            String toVariable,
            RelationshipDirection direction,
            String relationshipType
    ) {
        this.source = source;
        this.fromVariable = fromVariable;
        this.relationshipVariable = relationshipVariable;
        this.toVariable = toVariable;
        this.direction = direction;
        this.relationshipType = relationshipType;
    }

    @Override
    public String operatorType() {
        return "Expand(All)";
    }

    @Override
    public String details() {
        String typeSuffix = relationshipType == null ? "" : ":" + relationshipType;
        return "(" + fromVariable + ")-[" + relationshipVariable + typeSuffix + "]" + direction.arrow() + "(" + toVariable + ")";
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
        if (!(value instanceof NodeValue node)) {
            return List.of();
        }

        return switch (direction) {
            case OUTGOING -> relationships(context, node).stream()
                    .map(edge -> record.with(relationshipVariable, edge).with(toVariable, context.graph().nodeById(edge.endNodeId())))
                    .toList();
            case INCOMING -> relationships(context, node).stream()
                    .map(edge -> record.with(relationshipVariable, edge).with(toVariable, context.graph().nodeById(edge.startNodeId())))
                    .toList();
            case BOTH -> {
                List<GraphRecord> outgoing = (relationshipType == null
                        ? context.graph().outgoingRelationships(node)
                        : context.graph().outgoingRelationships(node, relationshipType)).stream()
                        .map(edge -> record.with(relationshipVariable, edge).with(toVariable, context.graph().nodeById(edge.endNodeId())))
                        .toList();
                List<GraphRecord> incoming = (relationshipType == null
                        ? context.graph().incomingRelationships(node)
                        : context.graph().incomingRelationships(node, relationshipType)).stream()
                        .map(edge -> record.with(relationshipVariable, edge).with(toVariable, context.graph().nodeById(edge.startNodeId())))
                        .toList();
                yield java.util.stream.Stream.concat(outgoing.stream(), incoming.stream()).toList();
            }
        };
    }

    private List<RelationshipValue> relationships(GraphExecutionContext context, NodeValue node) {
        return switch (direction) {
            case OUTGOING -> relationshipType == null
                    ? context.graph().outgoingRelationships(node)
                    : context.graph().outgoingRelationships(node, relationshipType);
            case INCOMING -> relationshipType == null
                    ? context.graph().incomingRelationships(node)
                    : context.graph().incomingRelationships(node, relationshipType);
            case BOTH -> throw new IllegalStateException("BOTH is handled separately");
        };
    }
}
