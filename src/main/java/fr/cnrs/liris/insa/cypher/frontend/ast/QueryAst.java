package fr.cnrs.liris.insa.cypher.frontend.ast;

import fr.cnrs.liris.insa.cypher.runtime.value.RelationshipDirection;

import java.util.List;

public record QueryAst(MatchClauseAst matchClause, ExpressionAst where, ReturnAst returnClause) {

    public record MatchClauseAst(PatternAst pattern) {
    }

    public record PatternAst(String pathVariable, NodePatternAst startNode, List<PatternStepAst> steps) {
        public PatternAst {
            steps = List.copyOf(steps);
        }

        public boolean hasVarLengthRelationship() {
            return steps.stream().anyMatch(step -> step.relationship().isVarLength());
        }
    }

    public record PatternStepAst(RelationshipPatternAst relationship, NodePatternAst node) {
    }

    public record NodePatternAst(String variable, String label) {
    }

    public record RelationshipPatternAst(
            String variable,
            String type,
            RelationshipDirection direction,
            Integer minHops,
            Integer maxHops
    ) {
        public boolean isVarLength() {
            return minHops != null && maxHops != null;
        }
    }

    public record ReturnAst(List<ReturnItemAst> items) {
        public ReturnAst {
            items = List.copyOf(items);
        }
    }

    public record ReturnItemAst(ExpressionAst expression, String alias) {
    }
}
