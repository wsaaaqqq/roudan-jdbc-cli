# roudan-jdbc-cli

[English](README.md) | [绠€浣撲腑鏂嘳(readme/README.zh.md) | [绻侀珨涓枃](readme/README.zht.md) | [頃滉淡鞏碷(readme/README.ko.md) | [Deutsch](readme/README.de.md) | [Espa帽ol](readme/README.es.md) | [Fran莽ais](readme/README.fr.md) | [Italiano](readme/README.it.md) | [Dansk](readme/README.da.md) | [鏃ユ湰瑾瀅(readme/README.ja.md) | [Polski](readme/README.pl.md) | [袪褍褋褋泻懈泄](readme/README.ru.md) | [Bosanski](readme/README.bs.md) | [丕賱毓乇亘賷丞](readme/README.ar.md) | [Norsk](readme/README.no.md) | [Portugu锚s (Brasil)](readme/README.br.md) | [喙勦笚喔(readme/README.th.md) | [T眉rk莽e](readme/README.tr.md) | [校泻褉邪褩薪褋褜泻邪](readme/README.uk.md) | [唳唳傕Σ唳綸(readme/README.bn.md) | [螘位位畏谓喂魏维](readme/README.gr.md) | [Ti岷縩g Vi峄噒](readme/README.vi.md)

JDBC CLI tool for AI agents. Executes SQL against any JDBC database, outputs JSON to stdout.

## One-Click Activation (Prompt for AI Agents)

Copy this line and paste it to OpenCode / Claude Code / Cursor:

> Install and use roudan-jdbc-cli, a JDBC CLI that lets me execute SQL against any database:  
> https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/INSTALL.md

Or let the agent read the install guide directly:

```bash
curl -fsSL https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/INSTALL.md
```

## Quick Start

### Prerequisites

- **Java 8+** (JRE or JDK on PATH) — install from https://adoptium.net
- **JDBC driver JAR** for your database (MySQL, PostgreSQL, DM, etc.)

### Install

```bash
npm install -g roudan-jdbc-cli    # jar included, ready to use

# Or Docker
docker pull wsaaaqqq/roudan-jdbc-cli

# Or one-line curl install
curl -fsSL https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/install.sh | bash
```

### Usage

```bash
# ====== Connection Persistence (login once, use anywhere) ======

# Login with CLI args
roudan -u jdbc:mysql://localhost:3306/test -n root -p pass -d com.mysql.cj.jdbc.Driver -j ./mysql-connector.jar login

# Login with YAML (auto-detect datasource from any nesting level)
roudan login -f application-dm.yml -j DmJdbcDriver.jar

# Login with multi-datasource YAML, specify which datasource
roudan login -f multi.yml mydb

# After login, all commands omit connection params:
roudan test
roudan tables
roudan query -s "SELECT * FROM T_USER" --limit 5

# Use a different saved connection
roudan use mydb

# One-off use of a saved connection
roudan --name mydb tables

# List saved connections
roudan connections

# Logout (clear current connection)
roudan logout

# ====== Direct Connection (one-shot, no login) ======

# Test connection
roudan -u jdbc:mysql://localhost:3306/test -n root -p pass -d com.mysql.cj.jdbc.Driver -j ./mysql-connector.jar test

# Query
roudan query -s "SELECT * FROM T_USER" --limit 5

# Named params
roudan query -s "SELECT * FROM T_USER WHERE id = :id" --named -a '{"id":"U01"}'

# Insert
roudan modify -s "INSERT INTO T_USER (id, name) VALUES (:id, :name)" --named -a '{"id":"U01","name":"Alice"}'

# List tables
roudan tables

# Describe table
roudan describe -t T_USER

# Execute SQL file as single transaction
roudan exec -f script.sql
roudan exec -s "INSERT INTO t VALUES(1); INSERT INTO t VALUES(2); SELECT * FROM t"

# Execute SQL file with dry-run (parse only)
roudan exec -f script.sql --dry-run
```

## Commands

| Command | Description |
|---------|------------|
| `login` | Save connection for reuse (CLI args or `-f yml`) |
| `logout` | Clear current saved connection |
| `use` | Switch to a saved connection by name |
| `connections` | List all saved connections |
| `query` | SELECT query |
| `count` | COUNT query |
| `modify` | INSERT/UPDATE/DELETE/DDL |
| `tables` | List tables/views |
| `describe` | Show table schema |
| `test` | Connection test |
| `exec` | Execute SQL file as single transaction (auto commit/rollback) |
| `begin` | Start transaction |
| `commit` | Commit transaction |
| `rollback` | Rollback transaction |
| `--name <name>` | (global) One-off use of a saved connection |

### Config Priority

```
CLI args (highest) > Environment variables > Saved connections > YAML config files
```

The `login` command saves connection parameters to `~/.roudan/connections.json`. After login, all subsequent commands reuse the saved connection automatically.

Full reference: [CLI_REFERENCE.md](doc/CLI_REFERENCE.md)

## OpenCode Skill

This repo includes an [OpenCode](https://opencode.ai) skill:

```bash
# Install skill
cp -r skill ~/.agents/skills/roudan-jdbc
```

Or add to `opencode.json`:

```json
{
  "skills": {
    "paths": ["/path/to/roudan-jdbc-cli/skill"]
  }
}
```

## Build

```bash
# Prerequisite: build roudan-core
cd ../roudan-core && mvn install -DskipTests

# Build this project
cd ../roudan-jdbc-cli
mvn package
# Output: target/roudan-jdbc-cli.jar
```

## Architecture

```
roudan-jdbc-cli
  鈹溾攢鈹€ Picocli CLI entry 鈫?parse args
  鈹溾攢鈹€ ConfigLoader    鈫?Config merge (YAML + CLI)
  鈹溾攢鈹€ DriverLoader    鈫?URLClassLoader dynamically loads JDBC drivers
  鈹溾攢鈹€ DataSourceFactory 鈫?Register with roudan-core
  鈹斺攢鈹€ CommandExecutor 鈫?roudan.query() / roudan.modify() / JDBC metadata
```

Built on [roudan-core](https://github.com/wsaaaqqq/xdb).

## License

Apache 2.0
