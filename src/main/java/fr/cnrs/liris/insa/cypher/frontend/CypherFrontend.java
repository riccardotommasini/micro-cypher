package fr.cnrs.liris.insa.cypher.frontend;

import fr.cnrs.liris.insa.cypher.frontend.ast.QueryAst;
import fr.cnrs.liris.insa.cypher.frontend.dsl.Query;
import fr.cnrs.liris.insa.cypher.frontend.parse.CypherSubsetParserFacade;
import fr.cnrs.liris.insa.cypher.frontend.plan.CypherSubsetPlanner;
import fr.cnrs.liris.insa.cypher.frontend.plan.LogicalPlan;
import fr.cnrs.liris.insa.cypher.frontend.plan.LogicalPlanCompiler;
import fr.cnrs.liris.insa.cypher.runtime.GraphAccess;
import fr.cnrs.liris.insa.cypher.runtime.GraphExecutionContext;
import fr.cnrs.liris.insa.cypher.runtime.GraphRecord;
import fr.cnrs.liris.insa.cypher.runtime.operator.CypherOperator;

import java.util.List;

public final class CypherFrontend {

    private final CypherSubsetParserFacade parser = new CypherSubsetParserFacade();
    private final CypherSubsetPlanner planner = new CypherSubsetPlanner();
    private final LogicalPlanCompiler compiler = new LogicalPlanCompiler();

    public QueryAst parse(String queryText) {
        return parser.parse(queryText);
    }

    public LogicalPlan plan(String queryText) {
        return planner.plan(parse(queryText));
    }

    public LogicalPlan plan(Query query) {
        return planner.plan(query.toAst());
    }

    public CypherOperator compile(String queryText) {
        return compiler.compile(plan(queryText));
    }

    public CypherOperator compile(Query query) {
        return compiler.compile(plan(query));
    }

    public List<GraphRecord> execute(GraphAccess graph, String queryText) {
        return new GraphExecutionContext(graph).execute(compile(queryText));
    }

    public List<GraphRecord> execute(GraphAccess graph, Query query) {
        return new GraphExecutionContext(graph).execute(compile(query));
    }
}
