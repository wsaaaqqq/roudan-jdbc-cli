# roudan 鈥?AI Agent Installation Guide

## Install roudan-jdbc-cli

Choose one of the following methods:

### Option A: One-line install (recommended)

```bash
curl -fsSL https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/install.sh | bash
```

This downloads the CLI jar. Requires Java 8+ pre-installed.

### Option B: npm

```bash
npm install -g roudan-jdbc-cli
```

Requires Java 8+ on PATH.

### Option C: Docker

```bash
docker pull wsaaaqqq/roudan-jdbc-cli:latest
alias roudan='docker run --rm -v "$(pwd):/workdir" -w /workdir wsaaaqqq/roudan-jdbc-cli'
```

### Option D: Manual download

```bash
# Create directory
mkdir -p ~/.roudan-cli/lib

# Download CLI jar
curl -fsSL -o ~/.roudan-cli/lib/roudan-jdbc-cli.jar \
  https://github.com/wsaaaqqq/roudan-jdbc-cli/releases/latest/download/roudan-jdbc-cli.jar

# Create wrapper (Unix)
cat > ~/.roudan-cli/roudan << 'EOF'
#!/bin/sh
DIR="$(dirname "$(readlink -f "$0")")"
exec java -jar "$DIR/lib/roudan-jdbc-cli.jar" "$@"
EOF
chmod +x ~/.roudan-cli/roudan

# Add to PATH
export PATH="$HOME/.roudan-cli:$PATH"
echo 'export PATH="$HOME/.roudan-cli:$PATH"' >> ~/.bashrc
```

## Install OpenCode / Claude Code Skill

```bash
mkdir -p ~/.agents/skills/roudan-jdbc/reference
curl -fsSL -o ~/.agents/skills/roudan-jdbc/SKILL.md \
  https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/skill/SKILL.md
curl -fsSL -o ~/.agents/skills/roudan-jdbc/reference/query.md \
  https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/skill/reference/query.md
curl -fsSL -o ~/.agents/skills/roudan-jdbc/reference/count.md \
  https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/skill/reference/count.md
curl -fsSL -o ~/.agents/skills/roudan-jdbc/reference/modify.md \
  https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/skill/reference/modify.md
curl -fsSL -o ~/.agents/skills/roudan-jdbc/reference/tables.md \
  https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/skill/reference/tables.md
curl -fsSL -o ~/.agents/skills/roudan-jdbc/reference/describe.md \
  https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/skill/reference/describe.md
curl -fsSL -o ~/.agents/skills/roudan-jdbc/reference/test.md \
  https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/skill/reference/test.md
```

Restart OpenCode for the skill to be loaded.

## Verify

```bash
roudan --help
```

Expected output shows available commands: `query`, `count`, `modify`, `tables`, `describe`, `test`, `begin`, `commit`, `rollback`, `login`, `logout`, `use`, `connections`.

## Quick Start

Test with H2 in-memory database (no external DB needed):

```bash
# Download H2 driver if not already in Maven cache
H2JAR="$HOME/.m2/repository/com/h2database/h2/2.2.220/h2-2.2.220.jar"

roudan -u jdbc:h2:mem:test -n sa -d org.h2.Driver -j "$H2JAR" test
roudan -u jdbc:h2:mem:test -n sa -d org.h2.Driver -j "$H2JAR" modify -s "CREATE TABLE T(ID INT, NAME VARCHAR)"
roudan -u jdbc:h2:mem:test -n sa -d org.h2.Driver -j "$H2JAR" modify -s "INSERT INTO T VALUES(:id,:name)" --named -a '{"id":1,"name":"hello"}'
roudan -u jdbc:h2:mem:test -n sa -d org.h2.Driver -j "$H2JAR" query -s "SELECT * FROM T"
```

## Connection Persistence (login)

Save connection params once, use them everywhere:

```bash
# Login with YAML config (supports Spring DataSource format)
roudan login -f application-dm.yml -j DmJdbcDriver.jar

# Login with CLI args
roudan -u jdbc:mysql://localhost:3306/test -n root -p pass -d com.mysql.cj.jdbc.Driver -j ./mysql-connector.jar login

# After login, all commands reuse the saved connection:
roudan test
roudan tables
roudan query -s "SELECT * FROM T_USER" --limit 5
```

## Connection Configuration

Three ways to provide connection info (priority order):

### 1. Command-line flags
```
-u jdbc:mysql://host:3306/db -n user -p pass -d com.mysql.cj.jdbc.Driver -j /path/to/mysql-connector.jar
```

### 2. Environment variables
```
ROUDAN_JDBC_URL, ROUDAN_JDBC_USER, ROUDAN_JDBC_PASSWORD, ROUDAN_JDBC_DRIVER, ROUDAN_JDBC_DRIVER_JAR
```

### 3. Config file (roudan-config.yaml)
```yaml
datasources:
  default:
    url: jdbc:mysql://localhost:3306/test
    user: root
    password: "123456"
    driver: com.mysql.cj.jdbc.Driver
    driverJar: /path/to/mysql-connector.jar
```

## Full Reference

See [CLI_REFERENCE.md](https://github.com/wsaaaqqq/roudan-jdbc-cli/blob/main/doc/CLI_REFERENCE.md) for all commands, options, and JSON output formats.
