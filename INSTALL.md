# roudan -- Installation Guide

## Prerequisites

- **Java 8+** (JRE or JDK) -- install from https://adoptium.net
- **JDBC driver JAR** for your database (MySQL, PostgreSQL, DM, etc.)

## Install

```bash
npm install -g roudan-jdbc-cli    # jar included, ready to use
```

## Install OpenCode Skill

```bash
mkdir -p ~/.agents/skills/roudan-jdbc/reference
for f in SKILL query count modify tables describe test; do
  curl -fsSL -o ~/.agents/skills/roudan-jdbc/${f}.md \
    https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/skill/${f}.md
done
for f in query count modify tables describe test; do
  curl -fsSL -o ~/.agents/skills/roudan-jdbc/reference/${f}.md \
    https://raw.githubusercontent.com/wsaaaqqq/roudan-jdbc-cli/main/skill/reference/${f}.md
done
```

## Update

```bash
npm update -g roudan-jdbc-cli
```
