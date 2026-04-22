package fr.cnrs.liris.insa.cypher.runtime.value;

import java.util.List;
import java.util.StringJoiner;

public record PathValue(List<NodeValue> nodes, List<RelationshipValue> relationships) {

    public PathValue {
        nodes = List.copyOf(nodes);
        relationships = List.copyOf(relationships);
    }

    public static PathValue single(NodeValue node) {
        return new PathValue(List.of(node), List.of());
    }

    public NodeValue start() {
        return nodes.getFirst();
    }

    public NodeValue end() {
        return nodes.getLast();
    }

    public int length() {
        return relationships.size();
    }

    public PathValue append(NodeValue node, RelationshipValue relationship) {
        List<NodeValue> nextNodes = new java.util.ArrayList<>(nodes);
        nextNodes.add(node);
        List<RelationshipValue> nextRelationships = new java.util.ArrayList<>(relationships);
        nextRelationships.add(relationship);
        return new PathValue(nextNodes, nextRelationships);
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner("");
        joiner.add(start().toString());
        for (int i = 0; i < relationships.size(); i++) {
            joiner.add("-[" + relationships.get(i).type() + "]->");
            joiner.add(nodes.get(i + 1).toString());
        }
        return "Path(" + joiner + ")";
    }
}
