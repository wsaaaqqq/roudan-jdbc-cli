# modify — 写操作

## 语法

```bash
roudan modify -s <sql> [选项]
roudan modify -f <sql-file> [选项]
```

## 选项

| 选项 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `-s, --sql` | String | - | SQL 语句（与 `-f` 任选其一） |
| `-f, --sql-file` | String | - | UTF-8 SQL 文件路径 |
| `--named` | boolean | false | 命名参数模式 |
| `-a, --args` | String | - | JSON 对象参数，建议配合 `--named` |

缺少 SQL 或 SQL 为空白时，在连接初始化前返回 `PARAM_ERROR`。0.5.6 的 modify 参数按 JSON 对象解析，不要套用 query 的 JSON 数组位置参数示例；参数化写入使用 `--named`。

## 适用场景

- INSERT
- UPDATE
- DELETE
- DDL（CREATE TABLE, DROP TABLE, ALTER TABLE 等）

## 输出

```json
{
  "success": true,
  "affectedRows": 1,
  "timeMs": 12
}
```

DDL 通常返回 `affectedRows:0`，不代表失败。UPDATE/DELETE 的 0 行也不会自动失败，需与业务预期核对。

先确认目标和写入授权；批量事务改用一次 [exec](exec.md)，不要把多个独立 CLI 调用当作同一事务。DDL 是否隐式提交以数据库为准；失败后先核实状态，不自动重试。

## 示例

```bash
# INSERT - 命名参数
roudan modify -s "INSERT INTO T_USER (id, name) VALUES (:id, :name)" --named -a '{"id":"U01","name":"王五"}'

# UPDATE - 命名参数
roudan modify -s "UPDATE T_USER SET name=:name WHERE id=:id" --named -a '{"name":"新名字","id":"U01"}'

# DELETE
roudan modify -s "DELETE FROM T_USER WHERE id=:id" --named -a '{"id":"U01"}'

# DDL
roudan modify -s "CREATE TABLE T_LOG (id VARCHAR(32), msg TEXT)"
roudan modify -s "ALTER TABLE T_USER ADD COLUMN email VARCHAR(128)"
```

JSON 单引号示例适用于 Bash/zsh；其他 Shell 的原生命令传参见 [setup.md](setup.md)。所有写入示例须替换目标、确认授权，不可直接用于未知默认连接。
