package fr.cnrs.liris.insa.cypher.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class PGraphBuilder {

    private final Map<Long, NodeAccumulator> nodesById = new LinkedHashMap<>();
    private final Map<EdgeKey, EdgeAccumulator> edgesByKey = new LinkedHashMap<>();

    public static PGraphBuilder create() {
        return new PGraphBuilder();
    }

    public static PGraphBuilder from(PGraph graph) {
        return create().merge(graph);
    }

    public PGraphBuilder addNode(long id, String... labels) {
        return addNode(id, Arrays.asList(labels), Map.of());
    }

    public PGraphBuilder addNode(long id, List<String> labels, Map<String, Object> properties) {
        NodeAccumulator accumulator = nodesById.computeIfAbsent(id, NodeAccumulator::new);
        accumulator.mergeLabels(labels);
        accumulator.mergeProperties(properties);
        return this;
    }

    public PGraphBuilder addEdge(long from, long to, String label) {
        return addEdge(from, to, List.of(label), Map.of());
    }

    public PGraphBuilder addEdge(long from, long to, List<String> labels, Map<String, Object> properties) {
        addNode(from, List.of(), Map.of());
        addNode(to, List.of(), Map.of());
        EdgeKey key = new EdgeKey(from, to, normalizeLabels(labels));
        EdgeAccumulator accumulator = edgesByKey.computeIfAbsent(key, ignored -> new EdgeAccumulator(from, to, key.labels()));
        accumulator.mergeProperties(properties);
        return this;
    }

    public PGraphBuilder merge(PGraph graph) {
        Arrays.stream(graph.nodes()).forEach(node ->
                addNode(node.id(), Arrays.asList(node.labels()), extractProperties(node)));
        Arrays.stream(graph.edges()).forEach(edge ->
                addEdge(edge.from(), edge.to(), Arrays.asList(edge.labels()), extractProperties(edge)));
        return this;
    }

    public PGraph build() {
        List<ImmutablePGraph.NodeData> nodes = nodesById.values().stream()
                .sorted(Comparator.comparingLong(NodeAccumulator::id))
                .map(NodeAccumulator::toNodeData)
                .toList();
        List<ImmutablePGraph.EdgeData> edges = edgesByKey.values().stream()
                .map(EdgeAccumulator::toEdgeData)
                .toList();
        return new ImmutablePGraph(nodes, edges);
    }

    public static PGraph union(PGraph left, PGraph right) {
        return PGraphBuilder.create().merge(left).merge(right).build();
    }

    private static Map<String, Object> extractProperties(PGraph.Node element) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (String property : element.properties()) {
            properties.put(property, element.property(property));
        }
        return properties;
    }

    private static List<String> normalizeLabels(List<String> labels) {
        return labels.stream()
                .distinct()
                .sorted()
                .toList();
    }

    private static final class NodeAccumulator {
        private final long id;
        private final Set<String> labels = new LinkedHashSet<>();
        private final Map<String, Object> properties = new LinkedHashMap<>();

        private NodeAccumulator(long id) {
            this.id = id;
        }

        long id() {
            return id;
        }

        void mergeLabels(List<String> newLabels) {
            labels.addAll(newLabels);
        }

        void mergeProperties(Map<String, Object> newProperties) {
            newProperties.forEach((key, value) -> mergeValue(properties, key, value, "node " + id));
        }

        ImmutablePGraph.NodeData toNodeData() {
            return new ImmutablePGraph.NodeData(id, labels.toArray(String[]::new), properties);
        }
    }

    private static final class EdgeAccumulator {
        private final long from;
        private final long to;
        private final List<String> labels;
        private final Map<String, Object> properties = new LinkedHashMap<>();

        private EdgeAccumulator(long from, long to, List<String> labels) {
            this.from = from;
            this.to = to;
            this.labels = labels;
        }

        void mergeProperties(Map<String, Object> newProperties) {
            newProperties.forEach((key, value) -> mergeValue(properties, key, value, "edge " + from + "->" + to + " " + labels));
        }

        ImmutablePGraph.EdgeData toEdgeData() {
            return new ImmutablePGraph.EdgeData(from, to, labels.toArray(String[]::new), properties);
        }
    }

    private static void mergeValue(Map<String, Object> target, String key, Object value, String scope) {
        Object existing = target.putIfAbsent(key, value);
        if (existing != null && !Objects.equals(existing, value)) {
            throw new IllegalArgumentException(
                    "Conflicting property '" + key + "' while merging " + scope + ": " + existing + " vs " + value
            );
        }
    }

    private record EdgeKey(long from, long to, List<String> labels) {
    }
}
