# AGENTS.md 鈥?roudan-jdbc-cli

## Purpose
A CLI tool invoked by AI agents via subprocess to execute SQL against JDBC databases.
Built on `roudan-core`. Input: command-line args. Output: JSON to stdout.

## Key Dependency
- **`io.github.wsaaaqqq:roudan-core:0.0.1`** 鈥?lightweight JDBC wrapper
  - Source: `C:\ws\java\roudan-core`
  - Core entry point: `org.xht.roudan.roudan` (static API)
  - Dependencies: hutool-core 5.8.25, hutool-cache 5.8.25, Lombok, Slf4j

## Build
- **Maven**, Java 8 (aligned with roudan-core)
- No Spring required 鈥?roudan-core accepts raw `javax.sql.DataSource`
- CLI framework: Picocli (recommended, not yet adopted)

## Architecture
```
roudan-jdbc-cli
  鈹溾攢鈹€ CLI entry 鈫?parse args (Picocli)
  鈹溾攢鈹€ Config loader 鈫?YAML or inline args 鈫?JDBC params
  鈹溾攢鈹€ Driver loader 鈫?URLClassLoader from user-supplied JAR path
  鈹溾攢鈹€ DataSource init 鈫?register with roudan.dataSourceConfig()
  鈹斺攢鈹€ Command exec 鈫?roudan.query() / roudan.modify() 鈫?JSON output
```

## roudan-core API Pattern
```java
// Register data source
roudan.dataSourceConfig(c -> c.addDataSource(dataSource));
RDConfig.setShowSql(false);

// Positional params (? placeholders)
roudan.query().sql("SELECT * FROM t WHERE id=?").args("U01").executeQuery();
roudan.modify().sql("INSERT INTO t VALUES(?,?)").args("U01", "name").execute();

// Named params (:paramName placeholders)
roudan.namedQuery().sql("SELECT * FROM t WHERE id=:id").args("id", "U01").executeQuery();
roudan.namedModify().sql("UPDATE t SET name=:name WHERE id=:id").args("id", "U01").args("name", "x").execute();
```

## Design Documents
- `doc/璇︾粏璁捐.md` 鈥?detailed CLI command design (Chinese)
- `doc/寮€鍙戣鍒?md` 鈥?development roadmap, architecture decisions, directory structure, CI/CD plan
- `doc/CLI_REFERENCE.md` 鈥?complete CLI usage reference: all commands, options, output format, error codes

## Conventions
- Follow roudan-core style: Lombok `@Slf4j`, hutool utilities, no comments unless non-obvious
- Output to stdout only (AI parses it); errors to stderr
- Each CLI invocation = one command execution, stateless, no interactive mode
