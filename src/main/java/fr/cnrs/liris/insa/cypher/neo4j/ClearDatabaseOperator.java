package fr.cnrs.liris.insa.cypher.neo4j;

import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;

public class ClearDatabaseOperator extends TransactionalGraphOperator<Void> {

    public ClearDatabaseOperator() {
        super(true);
    }

    @Override
    protected Void run(Transaction tx) {
        tx.getAllNodes().forEach(node -> {
            node.getRelationships().forEach(Relationship::delete);
            node.delete();
        });
        return null;
    }
}
