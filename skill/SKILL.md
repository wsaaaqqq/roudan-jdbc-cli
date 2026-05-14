---
name: roudan-jdbc
description: JDBC 数据库 CLI 工具。用于通过 JDBC 驱动执行 SQL 查询、修改、表结构查看、连接测试等数据库操作。当 AI 需要访问 JDBC 数据库、执行 SQL、查看表结构或进行 DDL 操作时使用。
metadata:
  requires:
    bins: ["roudan-jdbc-cli"]
  cliHelp: "roudan-jdbc-cli --help"
---

# roudan-jdbc-cli

> roudan-jdbc-cli 是一个命令行 JDBC 数据库工具，所有数据库操作通过执行 `roudan-jdbc-cli` 命令完成。

## 安装

前置要求：Java 8+

AI agent 自动安装步骤：

1. 检查是否有 Java：`java -version`
2. 从 [GitHub Releases](https://github.com/wsaaaqqq/roudan-jdbc-cli/releases) 下载最新 zip
3. 解压到 `~/.roudan-cli/`
4. 将 `~/.roudan-cli/` 加入 PATH
5. 验证：`roudan-jdbc-cli --help`

或者从源码构建：

```bash
cd ../roudan-core && mvn install -DskipTests
cd ../roudan-jdbc-cli && mvn package
# jar 在 target/roudan-jdbc-cli.jar
# 用 java -jar target/roudan-jdbc-cli.jar 直接运行
```

## 调用方式

```bash
roudan-jdbc-cli [连接选项] <子命令> [子命令选项]
```

### 连接选项

| 选项 | 说明 |
|------|------|
| `-u, --url <jdbc-url>` | JDBC URL |
| `-n, --user <user>` | 用户名 |
| `-p, --password <pw>` | 密码 |
| `-d, --driver <class>` | 驱动类名 |
| `-j, --driver-jar <path>` | 驱动 JAR 路径 |
| `-c, --config <file>` | YAML 配置文件 |

连接参数也可通过环境变量设置：`ROUDAN_JDBC_URL`, `ROUDAN_JDBC_USER`, `ROUDAN_JDBC_PASSWORD`, `ROUDAN_JDBC_DRIVER`, `ROUDAN_JDBC_DRIVER_JAR`

### 子命令

#### `query` — 查询

```bash
roudan-jdbc-cli query -s "SELECT * FROM T_USER WHERE age > ?" -a '[18]' --limit 5
roudan-jdbc-cli query -s "SELECT * FROM T_USER WHERE id = :id" --named -a '{"id":"U01"}'
```

参数：`-s/--sql <sql>` 或 `-f/--sql-file <path>`，`--named` 使用命名参数，`-a/--args` 传参（JSON 数组或对象），`--limit <n>` 限制行数，`--page/--size` 分页。

输出：
```json
{"success":true,"rowCount":42,"cols":["ID","NAME"],"rows":[[1,"张三"],[2,"李四"]],"timeMs":15}
```

rows 中的值保留 JDBC 原生类型：number、string、null。

#### `count` — 计数

```bash
roudan-jdbc-cli count -s "SELECT * FROM T_USER WHERE status = :st" --named -a '{"st":"ACTIVE"}'
```

输出：`{"success":true,"count":1503,"timeMs":8}`

#### `modify` — 增删改 / DDL

```bash
roudan-jdbc-cli modify -s "INSERT INTO T_USER (id, name) VALUES (:id, :name)" --named -a '{"id":"U01","name":"张三"}'
roudan-jdbc-cli modify -s "CREATE TABLE T_LOG (id VARCHAR(32), msg TEXT)"
```

输出：`{"success":true,"affectedRows":1,"timeMs":12}`

#### `tables` — 表列表

```bash
roudan-jdbc-cli tables
roudan-jdbc-cli tables --pattern "T_%"
```

输出：`{"success":true,"tables":[{"name":"T_USER","type":"TABLE"}],"timeMs":5}`

#### `describe` — 表结构

```bash
roudan-jdbc-cli describe -t T_USER
```

输出：`{"success":true,"table":"T_USER","columns":[{"name":"ID","type":"VARCHAR","size":32,"nullable":false,"pk":true}],"timeMs":3}`

#### `test` — 连接测试

```bash
roudan-jdbc-cli test
```

输出：`{"success":true,"message":"connection ok","dbProduct":"MySQL 8.0.33","timeMs":120}`

## 输出格式

默认 JSON（紧凑单行）。支持 `--output json-pretty|csv|table` 切换格式。`--no-header` 隐藏列头。

## 错误处理

所有命令失败时返回相同结构的 JSON 到 stdout，stderr 输出纯文本错误。

```json
{"success":false,"error":"SQLException: Table 'T_USERX' doesn't exist","errorCode":"SQL_ERROR","timeMs":2}
```

错误码：`CONFIG_ERROR`, `DRIVER_ERROR`, `CONNECTION_ERROR`, `SQL_ERROR`, `PARAM_ERROR`, `UNKNOWN_ERROR`

退出码：0=成功, 1=失败。

## 详细参考

每个子命令的详细文档见 `reference/` 目录。
