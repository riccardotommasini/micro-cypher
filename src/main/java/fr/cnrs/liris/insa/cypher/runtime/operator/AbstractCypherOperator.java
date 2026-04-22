package fr.cnrs.liris.insa.cypher.runtime.operator;

import java.util.List;

public abstract class AbstractCypherOperator implements CypherOperator {

    @Override
    public String details() {
        return "";
    }

    @Override
    public List<CypherOperator> children() {
        return List.of();
    }
}
