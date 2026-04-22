package fr.cnrs.liris.insa.cypher.runtime.value;

public enum RelationshipDirection {
    OUTGOING("->"),
    INCOMING("<-"),
    BOTH("-");

    private final String arrow;

    RelationshipDirection(String arrow) {
        this.arrow = arrow;
    }

    public String arrow() {
        return arrow;
    }
}
