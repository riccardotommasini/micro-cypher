package fr.cnrs.liris.insa.cypher.model;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ImmutablePGraph implements PGraph {

    private final Node[] nodes;
    private final Edge[] edges;
    private final long timestamp;

    public ImmutablePGraph(List<NodeData> nodes, List<EdgeData> edges) {
        this(nodes, edges, System.currentTimeMillis());
    }

    public ImmutablePGraph(List<NodeData> nodes, List<EdgeData> edges, long timestamp) {
        this.nodes = nodes.toArray(Node[]::new);
        this.edges = edges.toArray(Edge[]::new);
        this.timestamp = timestamp;
    }

    @Override
    public Node[] nodes() {
        return nodes;
    }

    @Override
    public Edge[] edges() {
        return edges;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "ImmutablePGraph{" +
               "nodes=" + Arrays.toString(nodes) +
               ", edges=" + Arrays.toString(edges) +
               ", timestamp=" + timestamp +
               '}';
    }

    public record NodeData(long id, String[] labels, Map<String, Object> propertyMap) implements PGraph.Node {
        public NodeData {
            labels = labels.clone();
            propertyMap = Map.copyOf(new LinkedHashMap<>(propertyMap));
        }

        @Override
        public String[] properties() {
            return propertyMap.keySet().toArray(String[]::new);
        }

        @Override
        public Object property(String p) {
            return propertyMap.get(p);
        }
    }

    public record EdgeData(long from, long to, String[] labels, Map<String, Object> propertyMap) implements PGraph.Edge {
        public EdgeData {
            labels = labels.clone();
            propertyMap = Map.copyOf(new LinkedHashMap<>(propertyMap));
        }

        @Override
        public long id() {
            return from;
        }

        @Override
        public String[] properties() {
            return propertyMap.keySet().toArray(String[]::new);
        }

        @Override
        public Object property(String p) {
            return propertyMap.get(p);
        }
    }
}
