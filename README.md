# roudan-jdbc-cli

[English](README.md) | [简体中文](readme/README.zh.md) | [繁體中文](readme/README.zht.md) | [한국어](readme/README.ko.md) | [Deutsch](readme/README.de.md) | [Español](readme/README.es.md) | [Français](readme/README.fr.md) | [Italiano](readme/README.it.md) | [Dansk](readme/README.da.md) | [日本語](readme/README.ja.md) | [Polski](readme/README.pl.md) | [Русский](readme/README.ru.md) | [Bosanski](readme/README.bs.md) | [العربية](readme/README.ar.md) | [Norsk](readme/README.no.md) | [Português (Brasil)](readme/README.br.md) | [ไทย](readme/README.th.md) | [Türkçe](readme/README.tr.md) | [Українська](readme/README.uk.md) | [বাংলা](readme/README.bn.md) | [Ελληνικά](readme/README.gr.md) | [Tiếng Việt](readme/README.vi.md)

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

### Install

```bash
# npm (recommended)
npm install -g roudan-jdbc-cli

# Docker
docker pull wsaaaqqq/roudan-jdbc-cli

# Direct download
curl -fsSL https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/install.sh | bash
```

### Usage

```bash
# ====== Connection Persistence (login once, use anywhere) ======

# Login with CLI args
rd -u jdbc:mysql://localhost:3306/test -n root -p pass -d com.mysql.cj.jdbc.Driver -j ./mysql-connector.jar login

# Login with YAML (auto-detect datasource from any nesting level)
rd login -f application-dm.yml -j DmJdbcDriver.jar

# Login with multi-datasource YAML, specify which datasource
rd login -f multi.yml mydb

# After login, all commands omit connection params:
rd test
rd tables
rd query -s "SELECT * FROM T_USER" --limit 5

# Use a different saved connection
rd use mydb

# One-off use of a saved connection
rd --name mydb tables

# List saved connections
rd connections

# Logout (clear current connection)
rd logout

# ====== Direct Connection (one-shot, no login) ======

# Test connection
rd -u jdbc:mysql://localhost:3306/test -n root -p pass -d com.mysql.cj.jdbc.Driver -j ./mysql-connector.jar test

# Query
rd query -s "SELECT * FROM T_USER" --limit 5

# Named params
rd query -s "SELECT * FROM T_USER WHERE id = :id" --named -a '{"id":"U01"}'

# Insert
rd modify -s "INSERT INTO T_USER (id, name) VALUES (:id, :name)" --named -a '{"id":"U01","name":"Alice"}'

# List tables
rd tables

# Describe table
rd describe -t T_USER

# Execute SQL file as single transaction
rd exec -f script.sql
rd exec -s "INSERT INTO t VALUES(1); INSERT INTO t VALUES(2); SELECT * FROM t"

# Execute SQL file with dry-run (parse only)
rd exec -f script.sql --dry-run
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
  ├── Picocli CLI entry → parse args
  ├── ConfigLoader    → Config merge (YAML + CLI)
  ├── DriverLoader    → URLClassLoader dynamically loads JDBC drivers
  ├── DataSourceFactory → Register with roudan-core
  └── CommandExecutor → RD.query() / RD.modify() / JDBC metadata
```

Built on [roudan-core](https://github.com/wsaaaqqq/xdb).

## License

Apache 2.0
