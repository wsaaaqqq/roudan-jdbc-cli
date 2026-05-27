# roudan -- AI Agent Installation Guide

## Prerequisites

- **Java 8+** (JRE or JDK) -- install from https://adoptium.net
- **JDBC driver JAR** for your database (MySQL, PostgreSQL, Oracle, DM, etc.)

## Install roudan-jdbc-cli

### Option A: One-line install (recommended)

```bash
curl -fsSL https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/install.sh | bash
```

Downloads the CLI jar to `~/.roudan-cli/lib/` and creates a wrapper. Requires Java 8+ pre-installed.

### Option B: npm

```bash
npm install -g roudan-jdbc-cli
```

Installs the wrapper only. You must also download the jar (see Option D) to `~/.roudan-cli/lib/`.

### Option C: Docker

```bash
docker pull wsaaaqqq/roudan-jdbc-cli:latest
alias roudan='docker run --rm -v "$(pwd):/workdir" -w /workdir wsaaaqqq/roudan-jdbc-cli'
```

### Option D: Manual download

```bash
mkdir -p ~/.roudan-cli/lib
curl -fL -o ~/.roudan-cli/lib/roudan-jdbc-cli.jar \
  https://github.com/wsaaaqqq/roudan-jdbc-cli/releases/latest/download/roudan-jdbc-cli.jar
java -jar ~/.roudan-cli/lib/roudan-jdbc-cli.jar --help
```

## Install OpenCode / Claude Code Skill

```bash
mkdir -p ~/.agents/skills/roudan-jdbc/reference
curl -fsSL -o ~/.agents/skills/roudan-jdbc/SKILL.md \
  https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/skill/SKILL.md
for f in query count modify tables describe test; do
  curl -fsSL -o ~/.agents/skills/roudan-jdbc/reference/$f.md \
    https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/skill/reference/$f.md
done
```

## Update

```bash
npm update -g roudan-jdbc-cli
# Then download latest jar:
curl -fL -o ~/.roudan-cli/lib/roudan-jdbc-cli.jar \
  https://github.com/wsaaaqqq/roudan-jdbc-cli/releases/latest/download/roudan-jdbc-cli.jar
```
