package fr.cnrs.liris.insa.cypher.runtime.value;

import java.util.List;
import java.util.Map;

public record RelationshipValue(
        long id,
        long startNodeId,
        long endNodeId,
        List<String> types,
        Map<String, Object> properties
) implements PropertyContainer {

    public RelationshipValue {
        types = List.copyOf(types);
        properties = Map.copyOf(properties);
    }

    public String type() {
        return types.isEmpty() ? "" : types.getFirst();
    }

    public boolean hasType(String type) {
        return types.contains(type);
    }

    @Override
    public String toString() {
        return "Relationship(" + id + ":" + startNodeId + "-[" + String.join("|", types) + "]->" + endNodeId + ")";
    }
}
