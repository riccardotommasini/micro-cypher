package fr.cnrs.liris.insa.cypher.frontend.plan;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import fr.cnrs.liris.insa.cypher.frontend.ast.ExpressionAst;

public final class LogicalPlanJson {

    public JsonObject toJson(LogicalPlan plan) {
        JsonObject json = new JsonObject();
        switch (plan) {
            case LogicalPlan.AllNodesScanPlan scan -> {
                json.addProperty("type", "all_nodes_scan");
                json.addProperty("variable", scan.variable());
            }
            case LogicalPlan.NodeByLabelScanPlan scan -> {
                json.addProperty("type", "node_by_label_scan");
                json.addProperty("variable", scan.variable());
                json.addProperty("label", scan.label());
            }
            case LogicalPlan.ExpandAllPlan expand -> {
                json.addProperty("type", "expand_all");
                json.add("source", toJson(expand.source()));
                json.addProperty("fromVariable", expand.fromVariable());
                json.addProperty("relationshipVariable", expand.relationshipVariable());
                json.addProperty("toVariable", expand.toVariable());
                json.addProperty("direction", expand.direction().name());
                if (expand.relationshipType() != null) {
                    json.addProperty("relationshipType", expand.relationshipType());
                }
            }
            case LogicalPlan.VarLengthExpandPlan expand -> {
                json.addProperty("type", "var_length_expand");
                json.add("source", toJson(expand.source()));
                json.addProperty("fromVariable", expand.fromVariable());
                json.addProperty("pathVariable", expand.pathVariable());
                json.addProperty("toVariable", expand.toVariable());
                json.addProperty("direction", expand.direction().name());
                if (expand.relationshipType() != null) {
                    json.addProperty("relationshipType", expand.relationshipType());
                }
                json.addProperty("minHops", expand.minHops());
                json.addProperty("maxHops", expand.maxHops());
            }
            case LogicalPlan.FilterPlan filter -> {
                json.addProperty("type", "filter");
                json.add("source", toJson(filter.source()));
                json.add("predicate", expression(filter.predicate()));
            }
            case LogicalPlan.ProjectionPlan projection -> {
                json.addProperty("type", "projection");
                json.add("source", toJson(projection.source()));
                JsonArray items = new JsonArray();
                projection.items().forEach(item -> {
                    JsonObject projectionItem = new JsonObject();
                    projectionItem.addProperty("alias", item.alias());
                    projectionItem.add("expression", expression(item.expression()));
                    items.add(projectionItem);
                });
                json.add("items", items);
            }
            case LogicalPlan.ProduceResultsPlan produceResults -> {
                json.addProperty("type", "produce_results");
                json.add("source", toJson(produceResults.source()));
                JsonArray columns = new JsonArray();
                produceResults.columns().forEach(columns::add);
                json.add("columns", columns);
            }
        }
        return json;
    }

    private JsonElement expression(ExpressionAst expression) {
        JsonObject json = new JsonObject();
        switch (expression) {
            case ExpressionAst.VariableRef ref -> {
                json.addProperty("type", "variable");
                json.addProperty("name", ref.name());
            }
            case ExpressionAst.PropertyAccess property -> {
                json.addProperty("type", "property");
                json.addProperty("variable", property.variable());
                json.addProperty("property", property.property());
            }
            case ExpressionAst.StringLiteral literal -> {
                json.addProperty("type", "string");
                json.addProperty("value", literal.value());
            }
            case ExpressionAst.IntegerLiteral literal -> {
                json.addProperty("type", "integer");
                json.addProperty("value", literal.value());
            }
            case ExpressionAst.Comparison comparison -> {
                json.addProperty("type", "comparison");
                json.addProperty("operator", comparison.operator());
                json.add("left", expression(comparison.left()));
                json.add("right", expression(comparison.right()));
            }
            case ExpressionAst.PathConstruction path -> {
                json.addProperty("type", "path");
                JsonArray nodeVariables = new JsonArray();
                path.nodeVariables().forEach(nodeVariables::add);
                JsonArray relationshipVariables = new JsonArray();
                path.relationshipVariables().forEach(relationshipVariables::add);
                json.add("nodeVariables", nodeVariables);
                json.add("relationshipVariables", relationshipVariables);
            }
        }
        return json;
    }
}
