---
name: roudan-jdbc
description: JDBC database CLI tool for AI agents. Execute SQL queries, DDL, inspect schemas, import/export data, test connections against any JDBC database. Use when the user needs to query a database, run SQL, explore tables, connect to MySQL/PostgreSQL/Oracle/SQL Server/DM/H2/Derby, or perform any database operation via JDBC.
metadata:
  requires:
    bins: ["roudan"]
  cliHelp: "roudan --help"
---

# roudan-jdbc-cli

> roudan 是一个命令行 JDBC 数据库工具，所有数据库操作通过执行 `roudan` 命令完成。

## 安装

前置要求：Java 8+

```bash
npm install -g roudan-jdbc-cli
```

验证：`roudan --help`

## 调用方式

```bash
roudan [连接选项] <子命令> [子命令选项]
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

### 连接持久化

`roudan` 支持将连接信息保存到 `~/.roudan/connections.json`，后续无需每次传递连接参数。

```bash
# 登录并保存连接
roudan -u jdbc:mysql://localhost:3306/test -n root -p pass -d com.mysql.cj.jdbc.Driver -j ./mysql-connector.jar login --name dev

# 从 YAML 文件登录
roudan login -f application.yml

# 列出已保存连接
roudan ls
roudan connections            # 详细列表（含 url/user）

# 切换当前连接
roudan use dev

# 删除连接
roudan logout dev
```

保存后，所有命令省略连接参数：
```bash
roudan test
roudan tables
roudan query -s "SELECT * FROM T_USER" --limit 5
```

### 子命令

#### `query` — 查询

```bash
roudan query -s "SELECT * FROM T_USER WHERE age > ?" -a '[18]' --limit 5
roudan query -s "SELECT * FROM T_USER WHERE id = :id" --named -a '{"id":"U01"}'
```

参数：`-s/--sql <sql>` 或 `-f/--sql-file <path>`，`--named` 使用命名参数，`-a/--args` 传参（JSON 数组或对象），`--limit <n>` 限制行数，`--page/--size` 分页。

输出：
```json
{"success":true,"rowCount":42,"cols":["ID","NAME"],"rows":[[1,"张三"],[2,"李四"]],"timeMs":15}
```

rows 中的值保留 JDBC 原生类型：number、string、null。

#### `count` — 计数

```bash
roudan count -s "SELECT * FROM T_USER WHERE status = :st" --named -a '{"st":"ACTIVE"}'
```

输出：`{"success":true,"count":1503,"timeMs":8}`

#### `modify` — 增删改 / DDL

```bash
roudan modify -s "INSERT INTO T_USER (id, name) VALUES (:id, :name)" --named -a '{"id":"U01","name":"张三"}'
roudan modify -s "CREATE TABLE T_LOG (id VARCHAR(32), msg TEXT)"
```

输出：`{"success":true,"affectedRows":1,"timeMs":12}`

#### `exec` — 多语句执行

```bash
roudan exec -s "INSERT INTO t VALUES(1); SELECT * FROM t"
roudan exec -f script.sql
roudan exec -s "..." --mode auto      # 每条语句独立提交
roudan exec -f script.sql --dry-run   # 只解析不执行
```

参数：`-s/--sql <text>` 或 `-f/--file <path>`，`--mode transaction|auto`（默认 transaction：全成功提交/任意失败回滚）。

输出：
```json
{"success":true,"statementCount":2,"results":[{"success":true,"type":"update","affectedRows":1},{"success":true,"type":"query","rowCount":1}],"timeMs":25}
```

#### `import` — 数据导入

```bash
roudan import -t T_USER -f data.csv
roudan import -t T_USER -f data.json --batch 100 --dry-run
```

参数：`-t/--table <name>`，`-f/--file <path>`（CSV 或 JSON），`--batch <n>`（默认 100），`--delimiter <char>`（CSV），`--dry-run`。

JSON 格式：`[{"col":"val"},...]` 或 `{"cols":["c1","c2"],"rows":[["v1","v2"]]}`

#### `export` — 数据导出

```bash
roudan export -s "SELECT * FROM T_USER" -o out.json
roudan export -s "SELECT * FROM T_USER" --format csv --limit 1000
```

参数：`-s/--sql <sql>`，`-o/--output <path>`（默认 stdout），`--format csv|json`，`--limit <n>`。

#### `gen` — 代码生成

```bash
roudan gen -t T_USER --ddl                    # 生成 CREATE TABLE
roudan gen -t T_USER --insert --sample 10      # 生成 INSERT 语句（取 10 行）
roudan gen -t T_USER --ddl --insert            # 同时生成 DDL + INSERT
```

参数：`-t/--table <name>`，`--ddl`，`--insert`，`--sample <n>`。

#### `tail` — 表变更监控

```bash
roudan tail -t T_USER --interval 3 --count 5
roudan tail -t T_USER --pk id --interval 5     # 按主键增量检测
roudan tail -s "SELECT * FROM T_LOG ORDER BY ts" --interval 2
```

参数：`-t/--table <name>` 或 `-s/--sql <sql>`，`--interval <sec>`（默认 3），`--count <n>`（轮询次数），`--pk <col>`（增量检测列）。

#### `tables` — 表列表

```bash
roudan tables
roudan tables --pattern "T_%"
```

输出：`{"success":true,"tables":[{"name":"T_USER","type":"TABLE"}],"timeMs":5}`

#### `describe` — 表结构

```bash
roudan describe -t T_USER
```

输出：`{"success":true,"table":"T_USER","columns":[{"name":"ID","type":"VARCHAR","size":32,"nullable":false,"pk":true}],"timeMs":3}`

#### `test` — 连接测试

```bash
roudan test
```

输出：`{"success":true,"message":"connection ok","dbProduct":"MySQL 8.0.33","timeMs":120}`

## 输出格式

默认 JSON（紧凑单行）。支持 `-o/--output json|json-pretty|csv|table` 切换格式。`--no-header` 隐藏列头。`--pretty` 美化 JSON。

## 错误处理

所有命令失败时返回相同结构的 JSON 到 stdout，stderr 输出纯文本错误。

```json
{"success":false,"error":"SQLException: Table 'T_USERX' doesn't exist","errorCode":"SQL_ERROR","timeMs":2}
```

错误码：`CONFIG_ERROR`, `DRIVER_ERROR`, `CONNECTION_ERROR`, `SQL_ERROR`, `PARAM_ERROR`, `NOT_FOUND`, `AMBIGUOUS`, `UNKNOWN_ERROR`

退出码：0=成功, 1=失败。

## 详细参考

每个子命令的详细文档见 `reference/` 目录。
