package fr.cnrs.liris.insa.cypher.frontend.plan;

import fr.cnrs.liris.insa.cypher.frontend.ast.ExpressionAst;
import fr.cnrs.liris.insa.cypher.runtime.value.RelationshipDirection;

import java.util.List;

public sealed interface LogicalPlan permits LogicalPlan.AllNodesScanPlan, LogicalPlan.NodeByLabelScanPlan,
        LogicalPlan.ExpandAllPlan, LogicalPlan.VarLengthExpandPlan, LogicalPlan.FilterPlan,
        LogicalPlan.ProjectionPlan, LogicalPlan.ProduceResultsPlan {

    record AllNodesScanPlan(String variable) implements LogicalPlan {
    }

    record NodeByLabelScanPlan(String variable, String label) implements LogicalPlan {
    }

    record ExpandAllPlan(
            LogicalPlan source,
            String fromVariable,
            String relationshipVariable,
            String toVariable,
            RelationshipDirection direction,
            String relationshipType
    ) implements LogicalPlan {
    }

    record VarLengthExpandPlan(
            LogicalPlan source,
            String fromVariable,
            String pathVariable,
            String toVariable,
            RelationshipDirection direction,
            String relationshipType,
            int minHops,
            int maxHops
    ) implements LogicalPlan {
    }

    record FilterPlan(LogicalPlan source, ExpressionAst predicate) implements LogicalPlan {
    }

    record ProjectionPlan(LogicalPlan source, List<ProjectionItemPlan> items) implements LogicalPlan {
        public ProjectionPlan {
            items = List.copyOf(items);
        }
    }

    record ProduceResultsPlan(LogicalPlan source, List<String> columns) implements LogicalPlan {
        public ProduceResultsPlan {
            columns = List.copyOf(columns);
        }
    }

    record ProjectionItemPlan(String alias, ExpressionAst expression) {
    }
}
