# roudan — Installation Guide

## CLI

Requires Java 8+ in the environment that actually runs the CLI. If `roudan --version` already works and the version is suitable, no reinstall is needed.

With npm in that same environment:

```text
npm install -g roudan-jdbc-cli
roudan --version
```

The package includes the JAR, not Java. A Windows npm installation invoked from WSL may not update WSL's native `roudan`. Verify the command that will actually be used; npm and GitHub releases may have different available versions.

Without Node.js, download the desired `roudan-jdbc-cli.jar` from [official Releases](https://github.com/wsaaaqqq/roudan-jdbc-cli/releases) and use it directly:

```text
java -jar "path/to/roudan-jdbc-cli.jar" --version
```

Replace the `roudan` prefix with `java -jar "path/to/roudan-jdbc-cli.jar"` for subsequent commands. No fixed installation path or global wrapper is required. JDBC driver reuse, discovery and configuration are covered in the skill references.

## Skill (any compatible AI client)

Use an existing checkout or download the [repository archive](https://github.com/wsaaaqqq/roudan-jdbc-cli/archive/refs/heads/main.zip). Copy the **entire `skill/` directory** into your client's configured skill search directory as `roudan-jdbc`:

```text
<configured-skill-directory>/roudan-jdbc/
    SKILL.md
    reference/
        setup.md
        connections.md
        exec.md
        query.md
        modify.md
        count.md
        tables.md
        describe.md
        test.md
```

Keep all relative paths intact. Do not copy only `SKILL.md` or assume a particular OpenCode, Windows or WSL directory. The skill content is identical across platforms; machine-specific paths/preferences belong in the host's local configuration. Review and back up local customizations before synchronizing.

For targeted environment or quoting troubleshooting, read [setup](skill/reference/setup.md); for connection/driver problems, read [connections](skill/reference/connections.md). Do not load every reference for an ordinary query.

## Updates

Only update when requested or required. For an npm-managed CLI, run `npm update -g roudan-jdbc-cli` in the same environment and verify `roudan --version` afterward. Update a direct JAR installation from official Releases, preserving a recovery copy when replacing an existing installation. Synchronize the complete skill directory separately; this need not change the CLI version.
