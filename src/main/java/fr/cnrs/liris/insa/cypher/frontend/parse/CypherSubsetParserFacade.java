package fr.cnrs.liris.insa.cypher.frontend.parse;

import org.antlr.v4.runtime.*;
import org.example.antlr.*;
import fr.cnrs.liris.insa.cypher.frontend.ast.ExpressionAst;
import fr.cnrs.liris.insa.cypher.frontend.ast.QueryAst;
import fr.cnrs.liris.insa.cypher.runtime.value.RelationshipDirection;

import java.util.ArrayList;
import java.util.List;

public final class CypherSubsetParserFacade {

    public QueryAst parse(String queryText) {
        CypherSubsetLexer lexer = new CypherSubsetLexer(CharStreams.fromString(queryText));
        CypherSubsetParser parser = new CypherSubsetParser(new CommonTokenStream(lexer));
        ThrowingErrorListener errorListener = new ThrowingErrorListener();
        lexer.removeErrorListeners();
        parser.removeErrorListeners();
        lexer.addErrorListener(errorListener);
        parser.addErrorListener(errorListener);
        return new AstBuilder().visitQuery(parser.query());
    }

    private static final class ThrowingErrorListener extends BaseErrorListener {
        @Override
        public void syntaxError(
                Recognizer<?, ?> recognizer,
                Object offendingSymbol,
                int line,
                int charPositionInLine,
                String msg,
                RecognitionException e
        ) {
            throw new IllegalArgumentException("Cypher parse error at " + line + ":" + charPositionInLine + " - " + msg, e);
        }
    }

    private static final class AstBuilder extends CypherSubsetBaseVisitor<QueryAst> {

        @Override
        public QueryAst visitQuery(CypherSubsetParser.QueryContext ctx) {
            QueryAst.MatchClauseAst matchClause = new QueryAst.MatchClauseAst(toPattern(ctx.pattern()));
            ExpressionAst where = ctx.expression() == null ? null : expression(ctx.expression());
            QueryAst.ReturnAst returnClause = new QueryAst.ReturnAst(ctx.returnItems().returnItem().stream()
                    .map(item -> {
                        ExpressionAst expression = expression(item.expression());
                        return new QueryAst.ReturnItemAst(expression, defaultAlias(expression));
                    })
                    .toList());
            return new QueryAst(matchClause, where, returnClause);
        }

        private QueryAst.PatternAst toPattern(CypherSubsetParser.PatternContext ctx) {
            QueryAst.NodePatternAst start = toNode(ctx.nodePattern());
            List<QueryAst.PatternStepAst> steps = new ArrayList<>();
            for (CypherSubsetParser.PatternChainContext chain : ctx.patternChain()) {
                steps.add(new QueryAst.PatternStepAst(toRelationship(chain.relationshipPattern()), toNode(chain.nodePattern())));
            }
            return new QueryAst.PatternAst(ctx.variable() == null ? null : ctx.variable().getText(), start, steps);
        }

        private QueryAst.NodePatternAst toNode(CypherSubsetParser.NodePatternContext ctx) {
            String variable = ctx.variable(0).getText();
            String label = ctx.variable().size() > 1 ? ctx.variable(1).getText() : null;
            return new QueryAst.NodePatternAst(variable, label);
        }

        private QueryAst.RelationshipPatternAst toRelationship(CypherSubsetParser.RelationshipPatternContext ctx) {
            CypherSubsetParser.RelationshipDetailContext detail = ctx.relationshipDetail();
            String variable = null;
            String type = null;
            Integer minHops = null;
            Integer maxHops = null;
            if (detail != null) {
                if (detail.variable().size() == 1) {
                    if (detail.COLON() != null) {
                        type = detail.variable(0).getText();
                    } else {
                        variable = detail.variable(0).getText();
                    }
                } else if (detail.variable().size() == 2) {
                    variable = detail.variable(0).getText();
                    type = detail.variable(1).getText();
                }
                if (detail.rangeLiteral() != null) {
                    minHops = Integer.parseInt(detail.rangeLiteral().integer(0).getText());
                    maxHops = Integer.parseInt(detail.rangeLiteral().integer(1).getText());
                }
            }
            RelationshipDirection direction = ctx.LEFT_ARROW() != null
                    ? RelationshipDirection.INCOMING
                    : RelationshipDirection.OUTGOING;
            return new QueryAst.RelationshipPatternAst(variable, type, direction, minHops, maxHops);
        }

        private ExpressionAst expression(CypherSubsetParser.ExpressionContext ctx) {
            ExpressionAst left = primary(ctx.primary(0));
            if (ctx.primary().size() == 1) {
                return left;
            }
            return new ExpressionAst.Comparison(ctx.comparator().getText(), left, primary(ctx.primary(1)));
        }

        private ExpressionAst primary(CypherSubsetParser.PrimaryContext ctx) {
            if (ctx.propertyAccess() != null) {
                return new ExpressionAst.PropertyAccess(
                        ctx.propertyAccess().variable(0).getText(),
                        ctx.propertyAccess().variable(1).getText()
                );
            }
            if (ctx.variable() != null) {
                return new ExpressionAst.VariableRef(ctx.variable().getText());
            }
            if (ctx.integer() != null) {
                return new ExpressionAst.IntegerLiteral(Long.parseLong(ctx.integer().getText()));
            }
            String raw = ctx.stringLiteral().getText();
            return new ExpressionAst.StringLiteral(unescape(raw.substring(1, raw.length() - 1)));
        }

        private String defaultAlias(ExpressionAst expression) {
            return switch (expression) {
                case ExpressionAst.VariableRef ref -> ref.name();
                case ExpressionAst.PropertyAccess property -> property.variable() + "." + property.property();
                case ExpressionAst.StringLiteral literal -> literal.value();
                case ExpressionAst.IntegerLiteral literal -> Long.toString(literal.value());
                case ExpressionAst.Comparison ignored -> "expr";
                case ExpressionAst.PathConstruction ignored -> "path";
            };
        }

        private String unescape(String raw) {
            return raw.replace("\\'", "'").replace("\\\\", "\\");
        }
    }
}
