package fr.cnrs.liris.insa.cypher.runtime;

import fr.cnrs.liris.insa.cypher.model.PGraph;
import fr.cnrs.liris.insa.cypher.runtime.value.NodeValue;
import fr.cnrs.liris.insa.cypher.runtime.value.PathValue;
import fr.cnrs.liris.insa.cypher.runtime.value.RelationshipDirection;
import fr.cnrs.liris.insa.cypher.runtime.value.RelationshipValue;

import java.util.*;
import java.util.stream.Collectors;

public final class GraphSnapshot implements GraphAccess {

    private final Map<Long, NodeValue> nodesById;
    private final Map<String, List<NodeValue>> nodesByLabel;
    private final Map<Long, RelationshipValue> relationshipsById;
    private final Map<Long, List<RelationshipValue>> outgoingByNodeId;
    private final Map<Long, List<RelationshipValue>> incomingByNodeId;
    private final Map<Long, Map<String, List<RelationshipValue>>> outgoingByNodeIdAndType;
    private final Map<Long, Map<String, List<RelationshipValue>>> incomingByNodeIdAndType;

    private GraphSnapshot(
            Map<Long, NodeValue> nodesById,
            Map<String, List<NodeValue>> nodesByLabel,
            Map<Long, RelationshipValue> relationshipsById,
            Map<Long, List<RelationshipValue>> outgoingByNodeId,
            Map<Long, List<RelationshipValue>> incomingByNodeId,
            Map<Long, Map<String, List<RelationshipValue>>> outgoingByNodeIdAndType,
            Map<Long, Map<String, List<RelationshipValue>>> incomingByNodeIdAndType
    ) {
        this.nodesById = Map.copyOf(nodesById);
        this.nodesByLabel = immutableNestedNodeIndex(nodesByLabel);
        this.relationshipsById = Map.copyOf(relationshipsById);
        this.outgoingByNodeId = immutableAdjacency(outgoingByNodeId);
        this.incomingByNodeId = immutableAdjacency(incomingByNodeId);
        this.outgoingByNodeIdAndType = immutableTypedAdjacency(outgoingByNodeIdAndType);
        this.incomingByNodeIdAndType = immutableTypedAdjacency(incomingByNodeIdAndType);
    }

    public static GraphSnapshot from(PGraph graph) {
        Map<Long, NodeValue> nodesById = new LinkedHashMap<>();
        Map<String, List<NodeValue>> nodesByLabel = new LinkedHashMap<>();
        Map<Long, RelationshipValue> relationshipsById = new LinkedHashMap<>();
        Map<Long, List<RelationshipValue>> outgoingByNodeId = new LinkedHashMap<>();
        Map<Long, List<RelationshipValue>> incomingByNodeId = new LinkedHashMap<>();
        Map<Long, Map<String, List<RelationshipValue>>> outgoingByNodeIdAndType = new LinkedHashMap<>();
        Map<Long, Map<String, List<RelationshipValue>>> incomingByNodeIdAndType = new LinkedHashMap<>();

        Arrays.stream(graph.nodes()).forEach(node -> {
            NodeValue nodeValue = new NodeValue(
                    node.id(),
                    List.of(node.labels()),
                    extractProperties(node)
            );
            nodesById.put(nodeValue.id(), nodeValue);
            nodeValue.labels().forEach(label ->
                    nodesByLabel.computeIfAbsent(label, ignored -> new ArrayList<>()).add(nodeValue));
        });

        PGraph.Edge[] edges = graph.edges();
        for (int i = 0; i < edges.length; i++) {
            PGraph.Edge edge = edges[i];
            RelationshipValue relationship = new RelationshipValue(
                    i,
                    edge.from(),
                    edge.to(),
                    List.of(edge.labels()),
                    extractProperties(edge)
            );
            relationshipsById.put(relationship.id(), relationship);
            outgoingByNodeId.computeIfAbsent(relationship.startNodeId(), ignored -> new ArrayList<>()).add(relationship);
            incomingByNodeId.computeIfAbsent(relationship.endNodeId(), ignored -> new ArrayList<>()).add(relationship);

            for (String type : relationship.types()) {
                outgoingByNodeIdAndType
                        .computeIfAbsent(relationship.startNodeId(), ignored -> new LinkedHashMap<>())
                        .computeIfAbsent(type, ignored -> new ArrayList<>())
                        .add(relationship);
                incomingByNodeIdAndType
                        .computeIfAbsent(relationship.endNodeId(), ignored -> new LinkedHashMap<>())
                        .computeIfAbsent(type, ignored -> new ArrayList<>())
                        .add(relationship);
            }
        }

        return new GraphSnapshot(
                nodesById,
                nodesByLabel,
                relationshipsById,
                outgoingByNodeId,
                incomingByNodeId,
                outgoingByNodeIdAndType,
                incomingByNodeIdAndType
        );
    }

    @Override
    public List<NodeValue> allNodes() {
        return List.copyOf(nodesById.values());
    }

    @Override
    public List<NodeValue> nodesWithLabel(String label) {
        return nodesByLabel.getOrDefault(label, List.of());
    }

    @Override
    public NodeValue nodeById(long id) {
        return nodesById.get(id);
    }

    @Override
    public RelationshipValue relationshipById(long id) {
        return relationshipsById.get(id);
    }

    public List<RelationshipValue> relationships() {
        return List.copyOf(relationshipsById.values());
    }

    @Override
    public List<RelationshipValue> outgoingRelationships(NodeValue node) {
        return outgoingByNodeId.getOrDefault(node.id(), List.of());
    }

    @Override
    public List<RelationshipValue> outgoingRelationships(NodeValue node, String type) {
        return outgoingByNodeIdAndType
                .getOrDefault(node.id(), Map.of())
                .getOrDefault(type, List.of());
    }

    @Override
    public List<RelationshipValue> incomingRelationships(NodeValue node) {
        return incomingByNodeId.getOrDefault(node.id(), List.of());
    }

    @Override
    public List<RelationshipValue> incomingRelationships(NodeValue node, String type) {
        return incomingByNodeIdAndType
                .getOrDefault(node.id(), Map.of())
                .getOrDefault(type, List.of());
    }

    @Override
    public List<PathValue> expandPaths(
            NodeValue start,
            RelationshipDirection direction,
            String relationshipType,
            int minHops,
            int maxHops
    ) {
        if (minHops < 0 || maxHops < minHops) {
            throw new IllegalArgumentException("Invalid hop bounds: " + minHops + ".." + maxHops);
        }

        List<PathValue> results = new ArrayList<>();
        ArrayDeque<PathState> frontier = new ArrayDeque<>();
        frontier.push(new PathState(start, PathValue.single(start), 0));

        while (!frontier.isEmpty()) {
            PathState state = frontier.pop();
            if (state.depth >= minHops && state.depth <= maxHops) {
                results.add(state.path);
            }
            if (state.depth == maxHops) {
                continue;
            }

            List<RelationshipValue> relationships = relationships(state.current, direction, relationshipType);
            for (RelationshipValue relationship : relationships) {
                NodeValue next = switch (direction) {
                    case OUTGOING -> nodeById(relationship.endNodeId());
                    case INCOMING -> nodeById(relationship.startNodeId());
                    case BOTH -> relationship.startNodeId() == state.current.id()
                            ? nodeById(relationship.endNodeId())
                            : nodeById(relationship.startNodeId());
                };
                frontier.push(new PathState(next, state.path.append(next, relationship), state.depth + 1));
            }
        }

        return results.stream()
                .filter(path -> path.length() >= minHops)
                .toList();
    }

    private List<RelationshipValue> relationships(NodeValue node, RelationshipDirection direction, String type) {
        return switch (direction) {
            case OUTGOING -> type == null ? outgoingRelationships(node) : outgoingRelationships(node, type);
            case INCOMING -> type == null ? incomingRelationships(node) : incomingRelationships(node, type);
            case BOTH -> {
                List<RelationshipValue> outgoing = type == null ? outgoingRelationships(node) : outgoingRelationships(node, type);
                List<RelationshipValue> incoming = type == null ? incomingRelationships(node) : incomingRelationships(node, type);
                yield java.util.stream.Stream.concat(outgoing.stream(), incoming.stream()).toList();
            }
        };
    }

    private static Map<String, Object> extractProperties(PGraph.Node element) {
        return Arrays.stream(element.properties())
                .collect(Collectors.toMap(
                        property -> property,
                        property -> normalizeValue(element.property(property)),
                        (left, right) -> right,
                        LinkedHashMap::new
                ));
    }

    private static Object normalizeValue(Object value) {
        if (value instanceof Map<?, ?> map && map.size() == 1) {
            return map.values().iterator().next();
        }
        return value;
    }

    private static Map<String, List<NodeValue>> immutableNestedNodeIndex(Map<String, List<NodeValue>> source) {
        return source.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }

    private static Map<Long, List<RelationshipValue>> immutableAdjacency(Map<Long, List<RelationshipValue>> source) {
        return source.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, entry -> List.copyOf(entry.getValue())));
    }

    private static Map<Long, Map<String, List<RelationshipValue>>> immutableTypedAdjacency(
            Map<Long, Map<String, List<RelationshipValue>>> source
    ) {
        return source.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().entrySet().stream()
                                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, nested -> List.copyOf(nested.getValue())))
                ));
    }

    private record PathState(NodeValue current, PathValue path, int depth) {
    }
}
