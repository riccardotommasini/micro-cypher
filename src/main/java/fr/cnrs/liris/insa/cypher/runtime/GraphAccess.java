package fr.cnrs.liris.insa.cypher.runtime;

import fr.cnrs.liris.insa.cypher.runtime.value.NodeValue;
import fr.cnrs.liris.insa.cypher.runtime.value.PathValue;
import fr.cnrs.liris.insa.cypher.runtime.value.RelationshipDirection;
import fr.cnrs.liris.insa.cypher.runtime.value.RelationshipValue;

import java.util.List;

public interface GraphAccess {

    List<NodeValue> allNodes();

    List<NodeValue> nodesWithLabel(String label);

    NodeValue nodeById(long id);

    RelationshipValue relationshipById(long id);

    List<RelationshipValue> outgoingRelationships(NodeValue node);

    List<RelationshipValue> outgoingRelationships(NodeValue node, String type);

    List<RelationshipValue> incomingRelationships(NodeValue node);

    List<RelationshipValue> incomingRelationships(NodeValue node, String type);

    List<PathValue> expandPaths(NodeValue start, RelationshipDirection direction, String relationshipType, int minHops, int maxHops);
}
