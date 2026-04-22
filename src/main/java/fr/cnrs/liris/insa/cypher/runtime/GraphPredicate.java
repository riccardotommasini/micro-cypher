package fr.cnrs.liris.insa.cypher.runtime;

@FunctionalInterface
public interface GraphPredicate {

    boolean test(GraphExecutionContext context, GraphRecord record);

    default String description() {
        return "anonymous";
    }
}
