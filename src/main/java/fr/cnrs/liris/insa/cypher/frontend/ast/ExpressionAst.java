package fr.cnrs.liris.insa.cypher.frontend.ast;

import java.util.List;

public sealed interface ExpressionAst permits ExpressionAst.VariableRef, ExpressionAst.PropertyAccess,
        ExpressionAst.StringLiteral, ExpressionAst.IntegerLiteral, ExpressionAst.Comparison, ExpressionAst.PathConstruction {

    record VariableRef(String name) implements ExpressionAst {
    }

    record PropertyAccess(String variable, String property) implements ExpressionAst {
    }

    record StringLiteral(String value) implements ExpressionAst {
    }

    record IntegerLiteral(long value) implements ExpressionAst {
    }

    record Comparison(String operator, ExpressionAst left, ExpressionAst right) implements ExpressionAst {
    }

    record PathConstruction(List<String> nodeVariables, List<String> relationshipVariables) implements ExpressionAst {
        public PathConstruction {
            nodeVariables = List.copyOf(nodeVariables);
            relationshipVariables = List.copyOf(relationshipVariables);
        }
    }
}
