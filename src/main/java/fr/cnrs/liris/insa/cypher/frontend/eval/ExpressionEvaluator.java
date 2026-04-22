package fr.cnrs.liris.insa.cypher.frontend.eval;

import fr.cnrs.liris.insa.cypher.frontend.ast.ExpressionAst;
import fr.cnrs.liris.insa.cypher.runtime.GraphExecutionContext;
import fr.cnrs.liris.insa.cypher.runtime.GraphRecord;
import fr.cnrs.liris.insa.cypher.runtime.value.NodeValue;
import fr.cnrs.liris.insa.cypher.runtime.value.PathValue;
import fr.cnrs.liris.insa.cypher.runtime.value.PropertyContainer;
import fr.cnrs.liris.insa.cypher.runtime.value.RelationshipValue;

import java.util.List;

public final class ExpressionEvaluator {

    public Object evaluate(GraphExecutionContext context, GraphRecord record, ExpressionAst expression) {
        return switch (expression) {
            case ExpressionAst.VariableRef ref -> record.get(ref.name());
            case ExpressionAst.PropertyAccess property ->
                    property(record.get(property.variable()), property.property());
            case ExpressionAst.StringLiteral literal -> literal.value();
            case ExpressionAst.IntegerLiteral literal -> literal.value();
            case ExpressionAst.Comparison comparison -> compare(
                    evaluate(context, record, comparison.left()),
                    evaluate(context, record, comparison.right()),
                    comparison.operator()
            );
            case ExpressionAst.PathConstruction path -> buildPath(record, path);
        };
    }

    private Object property(Object value, String propertyName) {
        if (value instanceof PropertyContainer properties) {
            return properties.property(propertyName);
        }
        throw new IllegalArgumentException("Value does not expose properties: " + value);
    }

    private boolean compare(Object left, Object right, String operator) {
        return switch (operator) {
            case "=" -> java.util.Objects.equals(left, right);
            case "<>" -> !java.util.Objects.equals(left, right);
            default -> throw new IllegalArgumentException("Unsupported comparison operator: " + operator);
        };
    }

    private PathValue buildPath(GraphRecord record, ExpressionAst.PathConstruction path) {
        List<NodeValue> nodes = path.nodeVariables().stream()
                .map(variable -> (NodeValue) record.get(variable))
                .toList();
        List<RelationshipValue> relationships = path.relationshipVariables().stream()
                .map(variable -> (RelationshipValue) record.get(variable))
                .toList();
        return new PathValue(nodes, relationships);
    }
}
