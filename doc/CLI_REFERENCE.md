# roudan-jdbc-cli CLI 使用参考

## 总览

```
roudan-jdbc-cli [连接选项] <子命令> [子命令选项]
```

---

## 连接选项

| 短名 | 长名 | 说明 |
|------|------|------|
| `-c` | `--config <file>` | YAML 配置文件路径 |
| `-u` | `--url <jdbc-url>` | JDBC URL |
| `-n` | `--user <username>` | 数据库用户名 |
| `-p` | `--password <password>` | 数据库密码 |
| `-d` | `--driver <class>` | JDBC 驱动类全限定名 |
| `-j` | `--driver-jar <path>` | 驱动 JAR 路径 |
| | `--datasource <name>` | 多数据源时指定名称，默认 `default` |
| `-o` | `--output <fmt>` | 输出格式：`json`(默认)、`json-pretty`、`csv`、`table` |
| | `--no-header` | 查询结果不输出列头 |
| | `--pretty` | 等价于 `--output json-pretty` |

### 配置优先级

```
命令行 -u/-n/-p/-d/-j > 环境变量 > 配置文件
```

### YAML 配置格式

```yaml
datasources:
  default:
    url: jdbc:mysql://localhost:3306/test
    user: root
    password: "123456"
    driver: com.mysql.cj.jdbc.Driver
    driverJar: /path/to/mysql-connector.jar
  otherdb:
    url: jdbc:postgresql://localhost:5432/test
    user: postgres
    password: "secret"
    driver: org.postgresql.Driver
    driverJar: /path/to/postgresql.jar
settings:
  showSql: false
  autoCommit: true
```

---

## 子命令

### query — 查询

```bash
roudan-jdbc-cli query -s "SELECT * FROM T_USER WHERE age > ?" -a '[18]' --limit 10
roudan-jdbc-cli query -s "SELECT * FROM T_USER WHERE age > :age" --named -a '{"age":18}' --limit 10
roudan-jdbc-cli query -f "./sql/user_list.sql" --named -a '{"deptId":"D01"}'
```

**选项：**

| 选项 | 说明 |
|------|------|
| `-s, --sql <sql>` | SQL 语句（与 `-f` 二选一必填） |
| `-f, --sql-file <path>` | SQL 文件路径 |
| `--named` | 命名参数模式 `:paramName` |
| `-a, --args <json>` | JSON 数组（位置）或 JSON 对象（命名） |
| `--limit <n>` | 返回行数上限 |
| `--page <n>` | 分页页码（从 1 开始，需配合 `--size`） |
| `--size <n>` | 分页每页条数 |

**输出（成功）：**

```json
{
  "success": true,
  "rowCount": 42,
  "cols": ["ID", "NAME", "AGE"],
  "rows": [
    [1, "张三", 25],
    [2, "李四", 30]
  ],
  "timeMs": 15
}
```

类型：`rows` 中的值保留 JDBC 原生类型（Integer→JSON number, String→JSON string, null→JSON null）。

**输出（无结果）：**

```json
{
  "success": true,
  "rowCount": 0,
  "cols": [],
  "rows": [],
  "timeMs": 3
}
```

---

### count — 计数

```bash
roudan-jdbc-cli count -s "SELECT * FROM T_USER WHERE age > ?" -a '[18]'
roudan-jdbc-cli count -s "SELECT * FROM T_USER WHERE status = :st" --named -a '{"st":1}'
```

**选项：** 同 query（无 `--limit`/`--page`/`--size`）

框架自动包装 SQL 为 `SELECT COUNT(1) FROM ( 原始SQL )`。

**输出：**

```json
{
  "success": true,
  "count": 1503,
  "timeMs": 8
}
```

---

### modify — 写操作

```bash
roudan-jdbc-cli modify -s "INSERT INTO T_USER (id, name) VALUES (:id, :name)" --named -a '{"id":"U01","name":"王五"}'
roudan-jdbc-cli modify -s "UPDATE T_USER SET name=? WHERE id=?" -a '["新名字","U01"]'
roudan-jdbc-cli modify -s "DELETE FROM T_USER WHERE id=?" -a '["U01"]'
roudan-jdbc-cli modify -s "CREATE TABLE T_LOG (id VARCHAR(32), msg TEXT)"
```

**选项：** 同 count

**输出：**

```json
{
  "success": true,
  "affectedRows": 1,
  "timeMs": 12
}
```

DDL 的 `affectedRows` 为 0。

---

### tables — 查看表/视图列表

```bash
roudan-jdbc-cli tables
roudan-jdbc-cli tables --pattern "T_%"
```

**选项：**

| 选项 | 说明 |
|------|------|
| `--pattern <p>` | 表名过滤模式（SQL LIKE 语法），默认 `%` |

**输出：**

```json
{
  "success": true,
  "tables": [
    {"name": "T_USER", "type": "TABLE"},
    {"name": "V_USER_ORDER", "type": "VIEW"}
  ],
  "timeMs": 5
}
```

---

### describe — 查看表结构

```bash
roudan-jdbc-cli describe -t T_USER
```

**选项：**

| 选项 | 说明 |
|------|------|
| `-t, --table <name>` | 表名（必填） |

**输出：**

```json
{
  "success": true,
  "table": "T_USER",
  "columns": [
    {"name": "ID", "type": "VARCHAR", "size": 32, "nullable": false, "pk": true},
    {"name": "SALARY", "type": "DECIMAL", "size": 10, "scale": 2, "nullable": true, "pk": false}
  ],
  "timeMs": 3
}
```

---

### test — 连接测试

```bash
roudan-jdbc-cli test
```

**选项：** 无

**输出（成功）：**

```json
{
  "success": true,
  "message": "connection ok",
  "dbProduct": "MySQL 8.0.33",
  "timeMs": 120
}
```

---

## 错误输出格式

所有命令失败时统一输出：

```json
{
  "success": false,
  "error": "SQLSyntaxErrorException: Table 'T_USERX' doesn't exist",
  "errorCode": "SQL_ERROR",
  "timeMs": 2
}
```

### 错误码

| errorCode | 说明 |
|-----------|------|
| `CONFIG_ERROR` | 配置加载/解析失败 |
| `DRIVER_ERROR` | JDBC 驱动加载失败 |
| `CONNECTION_ERROR` | 数据库连接失败 |
| `SQL_ERROR` | SQL 语法/执行错误 |
| `PARAM_ERROR` | CLI 参数不合法 |
| `UNKNOWN_ERROR` | 未知错误 |

### 退出码

| 退出码 | 含义 |
|--------|------|
| 0 | 成功 |
| 1 | 失败 |

---

## -a 参数 JSON 格式

### 位置参数

```json
[18, "张三"]
```
按顺序匹配 SQL 中的 `?` 占位。值类型保持：number→Integer, string→String, true→Boolean, null→null。

### 命名参数

```json
{"minAge": 18, "userName": "张三"}
```
key 匹配 SQL 中 `:key` 占位符（注意冒号前后必须留有空格）。

---

## AI agent 调用模式

```python
BASE = ["java", "-jar", "roudan-jdbc-cli.jar",
        "-u", "jdbc:mysql://host:3306/db",
        "-n", "root", "-p", "pass",
        "-d", "com.mysql.cj.jdbc.Driver", "-j", "/opt/drivers/mysql-connector.jar"]

def call(cmd):
    r = subprocess.run(BASE + cmd, capture_output=True, text=True)
    return json.loads(r.stdout)

# 使用
call(["test"])
call(["tables"])
call(["describe", "-t", "T_USER"])
call(["query", "-s", "SELECT * FROM T_USER", "--limit", "5"])
call(["query", "-s", "SELECT * FROM T_USER WHERE age > :age", "--named", "-a", '{"age":18}'])
call(["modify", "-s", "UPDATE T_USER SET name=? WHERE id=?", "-a", '["新名字","U01"]'])
```
