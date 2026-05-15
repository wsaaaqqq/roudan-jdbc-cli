# roudan-jdbc-cli

[English](README.md) | [简体中文](README.zh.md)

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

- Java 8+

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
# Test connection
roudan-jdbc-cli -u jdbc:mysql://localhost:3306/test -n root -p pass -d com.mysql.cj.jdbc.Driver -j ./mysql-connector.jar test

# Query
roudan-jdbc-cli query -s "SELECT * FROM T_USER" --limit 5

# Named params
roudan-jdbc-cli query -s "SELECT * FROM T_USER WHERE id = :id" --named -a '{"id":"U01"}'

# Insert
roudan-jdbc-cli modify -s "INSERT INTO T_USER (id, name) VALUES (:id, :name)" --named -a '{"id":"U01","name":"Alice"}'

# List tables
roudan-jdbc-cli tables

# Describe table
roudan-jdbc-cli describe -t T_USER
```

## Commands

| Command | Description |
|---------|------------|
| `query` | SELECT query |
| `count` | COUNT query |
| `modify` | INSERT/UPDATE/DELETE/DDL |
| `tables` | List tables/views |
| `describe` | Show table schema |
| `test` | Connection test |
| `begin` | Start transaction |
| `commit` | Commit transaction |
| `rollback` | Rollback transaction |

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
