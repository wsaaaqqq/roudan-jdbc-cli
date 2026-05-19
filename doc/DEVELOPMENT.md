# Development Guide

## Prerequisites

- JDK 8+
- Apache Maven 3.6+
- roudan-core (local dependency)

## Build

```bash
# 1. Build roudan-core first
cd ../roudan-core
mvn install -DskipTests

# 2. Build this project
cd ../roudan-jdbc-cli
mvn package

# JAR location: target/roudan-jdbc-cli.jar
```

## Build distribution

```bash
mvn package
mkdir -p dist/roudan-jdbc-cli
cp target/roudan-jdbc-cli.jar dist/roudan-jdbc-cli/lib/
cp src/main/scripts/roudan-jdbc-cli.bat dist/roudan-jdbc-cli/
cp src/main/scripts/roudan-jdbc-cli dist/roudan-jdbc-cli/
chmod +x dist/roudan-jdbc-cli/roudan-jdbc-cli
# dist/roudan-jdbc-cli/ is now a runnable distribution
```

## Test

```bash
mvn test
```

Tests use H2 in-memory database with sample schema.

## Project Structure

```
src/main/java/org/xht/roudan/cli/
├── Main.java                  # Picocli entry
├── config/
│   ├── CliConfig.java         # Config POJO
│   └── ConfigLoader.java      # YAML + CLI merge
├── driver/
│   └── DriverLoader.java      # URLClassLoader
├── datasource/
│   └── DataSourceFactory.java # DataSource creation
├── command/
│   ├── QueryCommand.java      # query
│   ├── CountCommand.java      # count
│   ├── ModifyCommand.java     # modify
│   ├── TablesCommand.java     # tables
│   ├── DescribeCommand.java   # describe
│   └── TestCommand.java       # test
└── output/
    └── ResultWriter.java      # JSON output
```

## Release Process

```bash
# Tag and push
git tag v0.0.1
git push origin v0.0.1

# CI will automatically:
# 1. Build the fat-jar
# 2. Create distribution zip
# 3. Create GitHub Release
```
