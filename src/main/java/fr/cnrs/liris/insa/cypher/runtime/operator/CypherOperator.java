package fr.cnrs.liris.insa.cypher.runtime.operator;

import fr.cnrs.liris.insa.cypher.runtime.GraphExecutionContext;
import fr.cnrs.liris.insa.cypher.runtime.GraphRecord;

import java.util.List;

public interface CypherOperator {

    String operatorType();

    String details();

    List<CypherOperator> children();

    List<GraphRecord> execute(GraphExecutionContext context);

    default String explain() {
        StringBuilder builder = new StringBuilder();
        explain(builder, "", true);
        return builder.toString();
    }

    private void explain(StringBuilder builder, String prefix, boolean last) {
        if (!prefix.isEmpty()) {
            builder.append(prefix).append(last ? "\\- " : "|- ");
        }
        builder.append(operatorType());
        if (!details().isBlank()) {
            builder.append(" [").append(details()).append("]");
        }
        builder.append(System.lineSeparator());

        List<CypherOperator> children = children();
        for (int i = 0; i < children.size(); i++) {
            CypherOperator child = children.get(i);
            String childPrefix = prefix + (prefix.isEmpty() ? "" : (last ? "   " : "|  "));
            child.explain(builder, childPrefix, i == children.size() - 1);
        }
    }
}
