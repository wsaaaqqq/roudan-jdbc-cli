# AGENTS.md — roudan-jdbc-cli

## Purpose
A CLI tool invoked by AI agents via subprocess to execute SQL against JDBC databases.
Built on `roudan-core`. Input: command-line args. Output: JSON to stdout.

## Key Dependency
- **`io.github.wsaaaqqq:roudan-core:0.0.1`** — lightweight JDBC wrapper
  - Source: `C:\ws\java\roudan-core`
  - Core entry point: `org.xht.rd.RD` (static API)
  - Dependencies: hutool-core 5.8.25, hutool-cache 5.8.25, Lombok, Slf4j

## Build
- **Maven**, Java 8 (aligned with roudan-core)
- No Spring required — roudan-core accepts raw `javax.sql.DataSource`
- CLI framework: Picocli (recommended, not yet adopted)

## Architecture
```
roudan-jdbc-cli
  ├── CLI entry → parse args (Picocli)
  ├── Config loader → YAML or inline args → JDBC params
  ├── Driver loader → URLClassLoader from user-supplied JAR path
  ├── DataSource init → register with RD.dataSourceConfig()
  └── Command exec → RD.query() / RD.modify() → JSON output
```

## roudan-core API Pattern
```java
// Register data source
RD.dataSourceConfig(c -> c.addDataSource(dataSource));
RDConfig.setShowSql(false);

// Positional params (? placeholders)
RD.query().sql("SELECT * FROM t WHERE id=?").args("U01").executeQuery();
RD.modify().sql("INSERT INTO t VALUES(?,?)").args("U01", "name").execute();

// Named params (:paramName placeholders)
RD.namedQuery().sql("SELECT * FROM t WHERE id=:id").args("id", "U01").executeQuery();
RD.namedModify().sql("UPDATE t SET name=:name WHERE id=:id").args("id", "U01").args("name", "x").execute();
```

## Design Documents
- `doc/详细设计.md` — detailed CLI command design (Chinese)
- `doc/开发规划.md` — development roadmap, architecture decisions, directory structure, CI/CD plan
- `doc/CLI_REFERENCE.md` — complete CLI usage reference: all commands, options, output format, error codes

## Conventions
- Follow roudan-core style: Lombok `@Slf4j`, hutool utilities, no comments unless non-obvious
- Output to stdout only (AI parses it); errors to stderr
- Each CLI invocation = one command execution, stateless, no interactive mode
