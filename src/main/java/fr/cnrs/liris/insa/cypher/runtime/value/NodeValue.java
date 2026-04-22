package fr.cnrs.liris.insa.cypher.runtime.value;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public record NodeValue(long id, List<String> labels, Map<String, Object> properties) implements PropertyContainer {

    public NodeValue {
        labels = List.copyOf(labels);
        properties = Map.copyOf(properties);
    }

    public boolean hasLabel(String label) {
        return labels.contains(label);
    }

    @Override
    public String toString() {
        if (labels.isEmpty()) {
            return "Node(" + id + ")";
        }

        StringJoiner joiner = new StringJoiner("|");
        labels.forEach(joiner::add);
        return "Node(" + id + ":" + joiner + ")";
    }
}
