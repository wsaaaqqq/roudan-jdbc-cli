# roudan-jdbc-cli — AI Agent Installation Guide

## Prerequisites

Check Java 8+ is available:

```bash
java -version 2>&1
```

Expected: `openjdk version "1.8.x"` or higher (Java 11 / 17 / 21+ also work).

If Java is not installed, install it:
- **macOS**: `brew install openjdk@17`
- **Ubuntu**: `apt-get install openjdk-17-jdk`
- **Windows**: download from https://adoptium.net/

## Install roudan-jdbc-cli

### Option A: npm (recommended)

```bash
npm install -g roudan-jdbc-cli
```

This downloads the fat-jar and creates a `roudan-jdbc-cli` command on PATH.

### Option B: Docker

```bash
docker pull wsaaaqqq/roudan-jdbc-cli:latest
alias roudan-jdbc-cli='docker run --rm -v "$(pwd):/workdir" -w /workdir wsaaaqqq/roudan-jdbc-cli'
```

### Option C: Direct download

```bash
mkdir -p ~/.roudan-cli/lib
curl -fsSL -o ~/.roudan-cli/lib/roudan-jdbc-cli.jar \
  https://github.com/wsaaaqqq/roudan-jdbc-cli/releases/latest/download/roudan-jdbc-cli.jar

# Create wrapper (Unix)
cat > ~/.roudan-cli/roudan-jdbc-cli << 'EOF'
#!/bin/sh
exec java -jar "$HOME/.roudan-cli/lib/roudan-jdbc-cli.jar" "$@"
EOF
chmod +x ~/.roudan-cli/roudan-jdbc-cli

# Add to PATH
export PATH="$HOME/.roudan-cli:$PATH"
echo 'export PATH="$HOME/.roudan-cli:$PATH"' >> ~/.bashrc
```

### Option D: One-line install script

```bash
curl -fsSL https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/install.sh | bash
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
roudan-jdbc-cli --help
```

Expected output shows available commands: `query`, `count`, `modify`, `tables`, `describe`, `test`, `begin`, `commit`, `rollback`.

## Quick Start

Test with H2 in-memory database (no external DB needed):

```bash
# Download H2 driver if not already in Maven cache
H2JAR="$HOME/.m2/repository/com/h2database/h2/2.2.220/h2-2.2.220.jar"

roudan-jdbc-cli -u jdbc:h2:mem:test -n sa -d org.h2.Driver -j "$H2JAR" test
roudan-jdbc-cli -u jdbc:h2:mem:test -n sa -d org.h2.Driver -j "$H2JAR" modify -s "CREATE TABLE T(ID INT, NAME VARCHAR)"
roudan-jdbc-cli -u jdbc:h2:mem:test -n sa -d org.h2.Driver -j "$H2JAR" modify -s "INSERT INTO T VALUES(:id,:name)" --named -a '{"id":1,"name":"hello"}'
roudan-jdbc-cli -u jdbc:h2:mem:test -n sa -d org.h2.Driver -j "$H2JAR" query -s "SELECT * FROM T"
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
