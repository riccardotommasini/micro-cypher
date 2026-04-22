package fr.cnrs.liris.insa.cypher.runtime;

import fr.cnrs.liris.insa.cypher.runtime.value.NodeValue;
import fr.cnrs.liris.insa.cypher.runtime.value.PathValue;
import fr.cnrs.liris.insa.cypher.runtime.value.RelationshipValue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public final class GraphRecord {

    private final LinkedHashMap<String, Object> values;

    private GraphRecord(LinkedHashMap<String, Object> values) {
        this.values = values;
    }

    public static GraphRecord empty() {
        return new GraphRecord(new LinkedHashMap<>());
    }

    public Object get(String key) {
        return values.get(key);
    }

    public GraphRecord with(String key, Object value) {
        LinkedHashMap<String, Object> copy = new LinkedHashMap<>(values);
        copy.put(key, value);
        return new GraphRecord(copy);
    }

    public GraphRecord project(Iterable<String> keys) {
        LinkedHashMap<String, Object> projection = new LinkedHashMap<>();
        for (String key : keys) {
            projection.put(key, values.get(key));
        }
        return new GraphRecord(projection);
    }

    public Map<String, Object> asMap() {
        return Map.copyOf(values);
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        values.forEach((key, value) -> joiner.add(key + "=" + renderValue(value)));
        return joiner.toString();
    }

    private static String renderValue(Object value) {
        if (value instanceof NodeValue || value instanceof RelationshipValue || value instanceof PathValue) {
            return value.toString();
        }
        return Objects.toString(value);
    }
}
