# modify — 写操作

## 语法

```bash
roudan modify -s <sql> [选项]
```

## 选项

| 选项 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `-s, --sql` | String | - | SQL 语句 |
| `--named` | boolean | false | 命名参数模式 |
| `-a, --args` | String | - | 参数 |

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

DDL 的 `affectedRows` 为 0。

## 示例

```bash
# INSERT - 命名参数
roudan modify -s "INSERT INTO T_USER (id, name) VALUES (:id, :name)" --named -a '{"id":"U01","name":"王五"}'

# UPDATE - 位置参数
roudan modify -s "UPDATE T_USER SET name=? WHERE id=?" -a '["新名字","U01"]'

# DELETE
roudan modify -s "DELETE FROM T_USER WHERE id=?" -a '["U01"]'

# DDL
roudan modify -s "CREATE TABLE T_LOG (id VARCHAR(32), msg TEXT)"
roudan modify -s "ALTER TABLE T_USER ADD COLUMN email VARCHAR(128)"
```
