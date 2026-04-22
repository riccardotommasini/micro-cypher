package fr.cnrs.liris.insa.cypher.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Transaction;

public abstract class TransactionalGraphOperator<T> implements GraphOperator<T> {

    private final boolean write;

    protected TransactionalGraphOperator(boolean write) {
        this.write = write;
    }

    @Override
    public final T execute(GraphDatabaseService db) {
        try (Transaction tx = db.beginTx()) {
            T result = run(tx);
            if (write) {
                tx.commit();
            }
            return result;
        }
    }

    protected abstract T run(Transaction tx);
}
