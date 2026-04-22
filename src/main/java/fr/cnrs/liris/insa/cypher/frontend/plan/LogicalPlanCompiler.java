package fr.cnrs.liris.insa.cypher.frontend.plan;

import fr.cnrs.liris.insa.cypher.frontend.eval.ExpressionEvaluator;
import fr.cnrs.liris.insa.cypher.runtime.GraphExecutionContext;
import fr.cnrs.liris.insa.cypher.runtime.GraphPredicate;
import fr.cnrs.liris.insa.cypher.runtime.GraphRecord;
import fr.cnrs.liris.insa.cypher.runtime.operator.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;

public final class LogicalPlanCompiler {

    private final ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator();

    public CypherOperator compile(LogicalPlan plan) {
        return switch (plan) {
            case LogicalPlan.AllNodesScanPlan scan -> new AllNodesScanOperator(scan.variable());
            case LogicalPlan.NodeByLabelScanPlan scan -> new NodeByLabelScanOperator(scan.variable(), scan.label());
            case LogicalPlan.ExpandAllPlan expand -> new ExpandAllOperator(
                    compile(expand.source()),
                    expand.fromVariable(),
                    expand.relationshipVariable(),
                    expand.toVariable(),
                    expand.direction(),
                    expand.relationshipType()
            );
            case LogicalPlan.VarLengthExpandPlan expand -> new VarLengthExpandOperator(
                    compile(expand.source()),
                    expand.fromVariable(),
                    expand.pathVariable(),
                    expand.toVariable(),
                    expand.direction(),
                    expand.relationshipType(),
                    expand.minHops(),
                    expand.maxHops()
            );
            case LogicalPlan.FilterPlan filter -> new FilterOperator(compile(filter.source()), new GraphPredicate() {
                @Override
                public boolean test(GraphExecutionContext context, GraphRecord record) {
                    return Boolean.TRUE.equals(expressionEvaluator.evaluate(context, record, filter.predicate()));
                }

                @Override
                public String description() {
                    return filter.predicate().toString();
                }
            });
            case LogicalPlan.ProjectionPlan projection -> {
                Map<String, BiFunction<GraphExecutionContext, GraphRecord, Object>> expressions = new LinkedHashMap<>();
                for (LogicalPlan.ProjectionItemPlan item : projection.items()) {
                    expressions.put(item.alias(), (context, record) -> expressionEvaluator.evaluate(context, record, item.expression()));
                }
                yield new ProjectionOperator(compile(projection.source()), expressions);
            }
            case LogicalPlan.ProduceResultsPlan results ->
                    new ProduceResultsOperator(compile(results.source()), results.columns());
        };
    }
}
