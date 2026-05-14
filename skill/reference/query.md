# query — 查询

## 语法

```bash
roudan-jdbc-cli query -s <sql> [选项]
roudan-jdbc-cli query -f <sql-file> [选项]
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

## 参数格式

### 位置参数（默认）
SQL: `SELECT * FROM t WHERE age > ? AND name = ?`
`-a` 值：`[18, "张三"]`
数组元素按顺序匹配 `?`。

### 命名参数
SQL: `SELECT * FROM t WHERE age > :minAge AND name = :userName`
`-a` 值：`{"minAge": 18, "userName": "张三"}`
对象 key 匹配 `:key` 占位符。

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
- `cols`: 列名数组
- `rows`: 数据行数组，每个元素是对应 `cols` 顺序的值数组
- 值类型保留 JDBC 原生类型：number、string、boolean、null

## 示例

```bash
# 基础查询
roudan-jdbc-cli query -s "SELECT * FROM T_USER" --limit 10

# 带参数
roudan-jdbc-cli query -s "SELECT * FROM T_USER WHERE id=?" -a '["U01"]'

# 命名参数
roudan-jdbc-cli query -s "SELECT * FROM T_USER WHERE age > :age" --named -a '{"age":18}' --limit 5

# 分页
roudan-jdbc-cli query -s "SELECT * FROM T_USER ORDER BY ID" --page 1 --size 20
```
