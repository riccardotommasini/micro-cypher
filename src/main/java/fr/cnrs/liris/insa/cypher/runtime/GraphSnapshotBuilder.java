package fr.cnrs.liris.insa.cypher.runtime;

import fr.cnrs.liris.insa.cypher.model.PGraph;
import fr.cnrs.liris.insa.cypher.model.PGraphBuilder;

import java.util.List;
import java.util.Map;

public final class GraphSnapshotBuilder {

    private final PGraphBuilder graphBuilder = PGraphBuilder.create();

    public static GraphSnapshotBuilder create() {
        return new GraphSnapshotBuilder();
    }

    public GraphSnapshotBuilder addNode(long id, String... labels) {
        graphBuilder.addNode(id, labels);
        return this;
    }

    public GraphSnapshotBuilder addNode(long id, List<String> labels, Map<String, Object> properties) {
        graphBuilder.addNode(id, labels, properties);
        return this;
    }

    public GraphSnapshotBuilder addEdge(long from, long to, String label) {
        graphBuilder.addEdge(from, to, label);
        return this;
    }

    public GraphSnapshotBuilder addEdge(long from, long to, List<String> labels, Map<String, Object> properties) {
        graphBuilder.addEdge(from, to, labels, properties);
        return this;
    }

    public GraphSnapshotBuilder merge(PGraph graph) {
        graphBuilder.merge(graph);
        return this;
    }

    public GraphSnapshotBuilder merge(GraphSnapshot snapshot) {
        snapshot.allNodes().forEach(node -> graphBuilder.addNode(node.id(), node.labels(), node.properties()));
        snapshot.relationships().forEach(relationship ->
                graphBuilder.addEdge(
                        relationship.startNodeId(),
                        relationship.endNodeId(),
                        relationship.types(),
                        relationship.properties()
                )
        );
        return this;
    }

    public PGraph buildGraph() {
        return graphBuilder.build();
    }

    public GraphSnapshot build() {
        return GraphSnapshot.from(buildGraph());
    }
}
