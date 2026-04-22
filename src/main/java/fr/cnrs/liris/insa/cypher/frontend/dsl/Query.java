package fr.cnrs.liris.insa.cypher.frontend.dsl;

import fr.cnrs.liris.insa.cypher.frontend.ast.ExpressionAst;
import fr.cnrs.liris.insa.cypher.frontend.ast.QueryAst;
import fr.cnrs.liris.insa.cypher.runtime.value.RelationshipDirection;

import java.util.ArrayList;
import java.util.List;

public final class Query {

    private final PatternBuilder patternBuilder;
    private ExpressionAst where;
    private final List<QueryAst.ReturnItemAst> returnItems = new ArrayList<>();

    private Query(PatternBuilder patternBuilder) {
        this.patternBuilder = patternBuilder;
    }

    public static Query match(PatternBuilder patternBuilder) {
        return new Query(patternBuilder);
    }

    public Query where(ExpressionAst expression) {
        this.where = expression;
        return this;
    }

    public Query returning(String... variables) {
        for (String variable : variables) {
            returnItems.add(new QueryAst.ReturnItemAst(new ExpressionAst.VariableRef(variable), variable));
        }
        return this;
    }

    public Query returning(ExpressionAst expression, String alias) {
        returnItems.add(new QueryAst.ReturnItemAst(expression, alias));
        return this;
    }

    public QueryAst toAst() {
        return new QueryAst(
                new QueryAst.MatchClauseAst(patternBuilder.build()),
                where,
                new QueryAst.ReturnAst(returnItems)
        );
    }

    public static PatternBuilder pattern(QueryAst.NodePatternAst startNode) {
        return new PatternBuilder(startNode);
    }

    public static QueryAst.NodePatternAst node(String variable) {
        return new QueryAst.NodePatternAst(variable, null);
    }

    public static QueryAst.NodePatternAst node(String variable, String label) {
        return new QueryAst.NodePatternAst(variable, label);
    }

    public static RelationshipBuilder rel() {
        return new RelationshipBuilder(null);
    }

    public static RelationshipBuilder rel(String variable) {
        return new RelationshipBuilder(variable);
    }

    public static ExpressionAst.VariableRef var(String name) {
        return new ExpressionAst.VariableRef(name);
    }

    public static ExpressionAst.PropertyAccess prop(String variable, String property) {
        return new ExpressionAst.PropertyAccess(variable, property);
    }

    public static ExpressionAst.StringLiteral lit(String value) {
        return new ExpressionAst.StringLiteral(value);
    }

    public static ExpressionAst.IntegerLiteral lit(long value) {
        return new ExpressionAst.IntegerLiteral(value);
    }

    public static ExpressionAst.Comparison eq(ExpressionAst left, ExpressionAst right) {
        return new ExpressionAst.Comparison("=", left, right);
    }

    public static ExpressionAst.Comparison neq(ExpressionAst left, ExpressionAst right) {
        return new ExpressionAst.Comparison("<>", left, right);
    }

    public static final class PatternBuilder {
        private final QueryAst.NodePatternAst startNode;
        private final List<QueryAst.PatternStepAst> steps = new ArrayList<>();
        private String pathVariable;

        private PatternBuilder(QueryAst.NodePatternAst startNode) {
            this.startNode = startNode;
        }

        public PatternBuilder named(String pathVariable) {
            this.pathVariable = pathVariable;
            return this;
        }

        public PatternBuilder out(RelationshipBuilder relationship, QueryAst.NodePatternAst nextNode) {
            steps.add(new QueryAst.PatternStepAst(relationship.direction(RelationshipDirection.OUTGOING).build(), nextNode));
            return this;
        }

        public PatternBuilder in(RelationshipBuilder relationship, QueryAst.NodePatternAst nextNode) {
            steps.add(new QueryAst.PatternStepAst(relationship.direction(RelationshipDirection.INCOMING).build(), nextNode));
            return this;
        }

        private QueryAst.PatternAst build() {
            return new QueryAst.PatternAst(pathVariable, startNode, steps);
        }
    }

    public static final class RelationshipBuilder {
        private final String variable;
        private String type;
        private RelationshipDirection direction = RelationshipDirection.OUTGOING;
        private Integer minHops;
        private Integer maxHops;

        private RelationshipBuilder(String variable) {
            this.variable = variable;
        }

        public RelationshipBuilder type(String type) {
            this.type = type;
            return this;
        }

        public RelationshipBuilder hops(int minHops, int maxHops) {
            this.minHops = minHops;
            this.maxHops = maxHops;
            return this;
        }

        private RelationshipBuilder direction(RelationshipDirection direction) {
            this.direction = direction;
            return this;
        }

        private QueryAst.RelationshipPatternAst build() {
            return new QueryAst.RelationshipPatternAst(variable, type, direction, minHops, maxHops);
        }
    }
}
