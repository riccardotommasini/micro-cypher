package fr.cnrs.liris.insa.cypher.frontend.plan;

import fr.cnrs.liris.insa.cypher.frontend.ast.ExpressionAst;
import fr.cnrs.liris.insa.cypher.frontend.ast.QueryAst;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CypherSubsetPlanner {

    public LogicalPlan plan(QueryAst query) {
        QueryAst.PatternAst pattern = query.matchClause().pattern();
        LogicalPlan current = initialScan(pattern.startNode());
        List<String> nodeVariables = new ArrayList<>();
        List<String> relationshipVariables = new ArrayList<>();
        nodeVariables.add(pattern.startNode().variable());

        for (int i = 0; i < pattern.steps().size(); i++) {
            QueryAst.PatternStepAst step = pattern.steps().get(i);
            QueryAst.RelationshipPatternAst relationship = step.relationship();
            String relationshipVariable = relationship.variable() != null ? relationship.variable() : "__rel" + i;
            relationshipVariables.add(relationshipVariable);
            String toVariable = step.node().variable();
            nodeVariables.add(toVariable);

            if (relationship.isVarLength()) {
                String pathVariable = pattern.pathVariable() != null ? pattern.pathVariable() : "__path" + i;
                current = new LogicalPlan.VarLengthExpandPlan(
                        current,
                        nodeVariables.get(nodeVariables.size() - 2),
                        pathVariable,
                        toVariable,
                        relationship.direction(),
                        relationship.type(),
                        relationship.minHops(),
                        relationship.maxHops()
                );
            } else {
                current = new LogicalPlan.ExpandAllPlan(
                        current,
                        nodeVariables.get(nodeVariables.size() - 2),
                        relationshipVariable,
                        toVariable,
                        relationship.direction(),
                        relationship.type()
                );
            }
        }

        if (query.where() != null) {
            current = new LogicalPlan.FilterPlan(current, query.where());
        }

        Map<String, ExpressionAst> projectionExpressions = new LinkedHashMap<>();
        ExpressionAst fixedPathExpression = pattern.pathVariable() != null && !pattern.hasVarLengthRelationship()
                ? new ExpressionAst.PathConstruction(nodeVariables, relationshipVariables)
                : null;
        for (QueryAst.ReturnItemAst item : query.returnClause().items()) {
            ExpressionAst expression = item.expression();
            if (fixedPathExpression != null
                && expression instanceof ExpressionAst.VariableRef ref
                && ref.name().equals(pattern.pathVariable())) {
                expression = fixedPathExpression;
            }
            projectionExpressions.put(item.alias(), expression);
        }
        if (fixedPathExpression != null && query.returnClause().items().stream().noneMatch(item -> item.alias().equals(pattern.pathVariable()))) {
            projectionExpressions.put(pattern.pathVariable(), fixedPathExpression);
        }

        List<LogicalPlan.ProjectionItemPlan> projectionItems = projectionExpressions.entrySet().stream()
                .map(entry -> new LogicalPlan.ProjectionItemPlan(entry.getKey(), entry.getValue()))
                .toList();
        current = new LogicalPlan.ProjectionPlan(current, projectionItems);
        return new LogicalPlan.ProduceResultsPlan(
                current,
                query.returnClause().items().stream().map(QueryAst.ReturnItemAst::alias).toList()
        );
    }

    private LogicalPlan initialScan(QueryAst.NodePatternAst startNode) {
        if (startNode.label() != null) {
            return new LogicalPlan.NodeByLabelScanPlan(startNode.variable(), startNode.label());
        }
        return new LogicalPlan.AllNodesScanPlan(startNode.variable());
    }
}
