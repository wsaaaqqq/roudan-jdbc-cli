# query — 查询

## 语法

```bash
roudan query -s <sql> [选项]
roudan query -f <sql-file> [选项]
```

## 选项

| 选项 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `-s, --sql` | String | - | SQL 语句（与 `-f` 二选一必填） |
| `-f, --sql-file` | String | - | SQL 文件路径 |
| `--named` | boolean | false | 启用命名参数模式 `:paramName` |
| `-a, --args` | String | - | 参数：JSON 数组（位置）或 JSON 对象（命名） |
| `--limit` | int | - | 返回行数上限 |
| `--page` | int | - | 分页页码（从 1 开始，需配合 `--size`） |
| `--size` | int | - | 分页每页条数 |
| `-o, --output` | String | `json`（或全局设置） | `json`、`json-pretty`、`csv`、`table` |

`-s/-f` 至少提供一个非空 SQL；缺失或空白返回 `PARAM_ERROR`，在连接初始化前拒绝。连接选项放在子命令前；0.5.6 支持 `query` 后的 `-o`、`--no-header`、`--pretty`。

## 参数格式

### 位置参数（默认）
SQL: `SELECT id, name FROM t WHERE age > ? AND name = ?`
`-a` 值：`[18, "张三"]`
数组元素按顺序匹配 `?`。

### 命名参数
SQL: `SELECT id, name FROM t WHERE age > :minAge AND name = :userName`
`-a` 值：`{"minAge": 18, "userName": "张三"}`
对象 key 匹配 `:key` 占位符。

下面命令的 JSON 单引号示例按 Bash/zsh 编写。cmd/PowerShell 或包装器不一定同样传参；遇到引号问题才读 [setup.md](setup.md)，不要把转义错误当数据库 SQL 错误反复重试。

## 输出

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

- `rowCount`: 匹配的总行数（非当前页行数）
- `cols`: 严格按 SELECT 顺序的列标签（含别名）；空结果仍保留元数据
- `rows`: 按列位置排列的值数组；NULL 保留原位置，重复列标签也不丢值
- 值类型保留 JDBC 原生类型：number、string、boolean、null

table 空结果输出 `0 rows`。默认紧凑 JSON；table 用于人工阅读，不为了程序解析默认开启 `--pretty`。

`--limit`/`--page`/`--size` 在全量查询后内存截取，`rowCount` 仍是截取前总行数，不限制数据库扫描。大表应在 SQL 中用 WHERE、聚合及对应数据库支持的限行语法；不要把 `--limit 10` 当成只读取 10 行的保证。

## 示例

```bash
# 基础查询
roudan query -s "SELECT ID, NAME FROM T_USER WHERE STATUS='ACTIVE'" --limit 10

# 带参数
roudan query -s "SELECT ID, NAME FROM T_USER WHERE id=?" -a '["U01"]'

# 命名参数
roudan query -s "SELECT ID, NAME FROM T_USER WHERE age > :age" --named -a '{"age":18}' --limit 5

# 分页
roudan query -s "SELECT ID, NAME FROM T_USER ORDER BY ID" --page 1 --size 20

# 人工核对列顺序
roudan query -s "SELECT NAME, ID FROM T_USER WHERE ID=1" -o table

# 复杂 SQL 文件
roudan query -f query.sql --limit 10
```

示例假定目标连接已确认；不要仅因当前默认连接存在就使用它。实际可在子命令前加 `--name <已确认连接名>`。

大结果可用 `export -s "..." --format json -o result.json` 写文件；export 的 `-o` 是路径，扩展名不决定格式。不指定文件时 stdout 会包含数据及执行摘要，不应按单个 JSON 解析。query 的列顺序/NULL 保证不能未经验证套用到 export。
