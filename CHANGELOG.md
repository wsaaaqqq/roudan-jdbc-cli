# Changelog

## v0.1.0 — 2026-05-15

### Features

- **6 core commands**: `query`, `count`, `modify`, `tables`, `describe`, `test`
- **3 transaction commands**: `begin`, `commit`, `rollback`
- **Positional params** (`?`) and **named params** (`:paramName`) SQL styles
- **Config auto-discovery**: CLI args > env vars > cwd config > ~/.roudan config
- **Multi-datasource** YAML support with `--datasource` switch
- **HikariCP connection pool** with configurable pool settings
- **SQL file execution** via `-f/--sql-file`
- **Output formats**: JSON (compact/pretty), CSV, table
- **Limit & pagination**: `--limit`, `--page`/`--size`
- **Dry-run mode**: `--dry-run` prints SQL without execution
- **Connection timeout**: `--connect-timeout <ms>`

### Distribution

- GitHub Releases (fat-jar + startup scripts)
- npm: `npm install roudan-jdbc-cli` (@roudan/jdbc-cli)
- Docker: `docker run wsaaaqqq/roudan-jdbc-cli`
- One-line install: `curl -fsSL https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/install.sh | bash`
- OpenCode skill: `skill/SKILL.md`

### Test Coverage

34 end-to-end tests covering:
- H2 default, MySQL mode, PostgreSQL mode
- All 9 CLI commands
- Multi-datasource switching
- Config auto-discovery (env, cwd, YAML)
- Error handling (SQL errors, missing args, connection errors)
- Type preservation in JSON output
