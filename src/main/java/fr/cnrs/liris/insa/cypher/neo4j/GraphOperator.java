package fr.cnrs.liris.insa.cypher.neo4j;

import org.neo4j.graphdb.GraphDatabaseService;

public interface GraphOperator<T> {

    T execute(GraphDatabaseService db);
}
