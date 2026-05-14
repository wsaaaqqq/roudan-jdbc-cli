# count — 计数

## 语法

```bash
roudan-jdbc-cli count -s <sql> [选项]
```

## 选项

| 选项 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `-s, --sql` | String | - | SQL 语句 |
| `--named` | boolean | false | 命名参数模式 |
| `-a, --args` | String | - | 参数 |

## 实现

框架自动将 SQL 包装为 `SELECT COUNT(1) FROM ( 原始SQL )`，不需要手动写 COUNT。

## 输出

```json
{
  "success": true,
  "count": 1503,
  "timeMs": 8
}
```

## 示例

```bash
roudan-jdbc-cli count -s "SELECT * FROM T_USER WHERE age > ?" -a '[18]'
roudan-jdbc-cli count -s "SELECT * FROM T_USER WHERE status = :st" --named -a '{"st":"ACTIVE"}'
```
