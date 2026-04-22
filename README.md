# Neo4j Experiments

This project explores a small read-only Cypher-style engine on top of an in-memory graph snapshot, while still keeping an embedded Neo4j path around for comparison.

## Structure

- `org.example.demo`
  Contains runnable demos such as [Main.java](/Users/rictomm/_Projects/neo4j-experiments/src/main/java/fr/cnrs/liris/insa/cypher/demo/Main.java:1) and [GraphBuildExample.java](/Users/rictomm/_Projects/neo4j-experiments/src/main/java/fr/cnrs/liris/insa/cypher/demo/GraphBuildExample.java:1).
- `org.example.model`
  Import/fixture model used to load graph JSON files such as [testGraph1.json](/Users/rictomm/_Projects/neo4j-experiments/src/main/resources/testGraph1.json:1) and [testGraphPaths.json](/Users/rictomm/_Projects/neo4j-experiments/src/main/resources/testGraphPaths.json:1).
- `org.example.runtime`
  Immutable graph snapshot, execution context, row records, and runtime predicates.
- `org.example.runtime.value`
  Typed runtime values: nodes, relationships, paths, and relationship directions.
- `org.example.runtime.operator`
  Executable operator tree for scans, expands, filters, projections, and result production.
- `org.example.frontend`
  End-to-end query front end.
- `org.example.frontend.ast`
  Internal AST for the supported Cypher subset.
- `org.example.frontend.parse`
  ANTLR-backed parser facade and subset grammar integration.
- `org.example.frontend.plan`
  Logical plan model, planner, compiler to runtime operators, and JSON serialization.
- `org.example.frontend.dsl`
  Fluent Java API for building queries programmatically.
- `org.example.neo4j`
  Embedded Neo4j helper operators used for the comparison/demo path.

## Supported Cypher Fragment

The implemented subset is intentionally narrow and matches the runtime we have today:

- `MATCH`
- a single pattern part
- node variables with optional labels
- relationship variables with optional types
- directed relationships
- bounded var-length relationships with explicit `*min..max`
- optional named path assignment
- optional `WHERE` with `=` and `<>`
- variable references
- property access like `p.permitted`
- string and integer literals
- `RETURN` of variables and named paths

The reference openCypher grammar is kept in [Cypher.g4](/Users/rictomm/_Projects/neo4j-experiments/src/main/resources/Cypher.g4:1). The executable subset grammar lives in [CypherSubset.g4](/Users/rictomm/_Projects/neo4j-experiments/src/main/antlr4/org/example/CypherSubset.g4:1).

## Flow

There are two main ways to build a query:

1. Parse query text through `CypherFrontend`
2. Build the same query through the fluent `Query` DSL

Both routes converge on the same pipeline:

1. query text or DSL
2. `QueryAst`
3. `LogicalPlan`
4. compiled `CypherOperator` tree
5. execution over `GraphSnapshot`

The logical plan can also be serialized as JSON and validated against [logical-plan.schema.json](/Users/rictomm/_Projects/neo4j-experiments/src/main/resources/logical-plan.schema.json:1).

## Build And Test

Use Java 21.

Run tests:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home mvn test
```

Run the demo:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home mvn -e exec:java -Dexec.mainClass=fr.cnrs.liris.insa.cypher.demo.Main
```

The demo shows:

- a parsed subset-Cypher query compiled into the runtime operator tree
- the same style of query built with the fluent DSL
- the embedded Neo4j comparison path

Run the graph merge/build example:

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home mvn -e exec:java -Dexec.mainClass=fr.cnrs.liris.insa.cypher.demo.GraphBuildExample
```

That example shows:

- safe `PGraph` union through `PGraphBuilder`
- incremental `GraphSnapshot` construction through `GraphSnapshotBuilder`

## Tests

- [CypherFrontendTest.java](/Users/rictomm/_Projects/neo4j-experiments/src/test/java/org/example/frontend/CypherFrontendTest.java:1)
  Covers parser acceptance/rejection, logical-plan shape, schema validation, parsed-query execution, and DSL parity.
- [GraphRuntimeTest.java](/Users/rictomm/_Projects/neo4j-experiments/src/test/java/org/example/runtime/GraphRuntimeTest.java:1)
  Covers the runtime operator tree directly over graph fixtures.
