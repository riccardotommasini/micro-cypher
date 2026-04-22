package fr.cnrs.liris.insa.cypher.neo4j;

import fr.cnrs.liris.insa.cypher.model.PGraph;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class ImportGraphOperator extends TransactionalGraphOperator<Void> {

    private final PGraph graph;

    public ImportGraphOperator(PGraph graph) {
        super(true);
        this.graph = graph;
    }

    @Override
    protected Void run(Transaction tx) {
        Map<Long, Node> ids = new HashMap<>();

        Arrays.stream(graph.nodes()).forEach(sourceNode -> {
            Node node = tx.createNode();
            Arrays.stream(sourceNode.labels()).forEach(label -> node.addLabel(Label.label(label)));
            Arrays.stream(sourceNode.properties()).forEach(propertyName ->
                    node.setProperty(propertyName, propertyValue(sourceNode, propertyName)));
            ids.put(sourceNode.id(), node);
        });

        Arrays.stream(graph.edges()).forEach(edge -> {
            Node from = ids.computeIfAbsent(edge.from(), ignored -> tx.createNode());
            Node to = ids.computeIfAbsent(edge.to(), ignored -> tx.createNode());

            Arrays.stream(edge.labels()).forEach(label -> {
                var relationship = from.createRelationshipTo(to, RelationshipType.withName(label));
                Arrays.stream(edge.properties()).forEach(propertyName ->
                        relationship.setProperty(propertyName, propertyValue(edge, propertyName)));
            });
        });

        return null;
    }

    private static Object propertyValue(PGraph.Node element, String propertyName) {
        Object property = element.property(propertyName);
        if (property instanceof Map<?, ?> map) {
            return map.entrySet().stream().findFirst().orElseThrow().getValue();
        }
        return property;
    }
}
