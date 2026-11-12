# describe — 查看表结构

## 语法

```bash
roudan describe -t <table-name>
```

## 选项

| 选项 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `-t, --table` | String | **必填** | 表名 |
| `-o, --output` | String | `json`（或全局设置） | `json`、`json-pretty`、`csv`、`table` |

## 输出

```json
{
  "success": true,
  "table": "T_USER",
  "columns": [
    {"name": "ID", "type": "VARCHAR", "size": 32, "nullable": false, "pk": true},
    {"name": "NAME", "type": "VARCHAR", "size": 64, "nullable": true, "pk": false},
    {"name": "AGE", "type": "INTEGER", "size": 10, "nullable": true, "pk": false},
    {"name": "SALARY", "type": "DECIMAL", "size": 10, "scale": 2, "nullable": true, "pk": false}
  ],
  "timeMs": 3
}
```

- `pk`: 是否为主键
- `scale`: 仅 DECIMAL/NUMERIC 类型有该字段
- `nullable`: 是否可为 null
- `size`: 列长度

## 示例

```bash
roudan describe -t T_USER
roudan describe -t T_USER -o table
```

table/CSV 按 `NAME`、`TYPE`、`SIZE`、`NULLABLE`、`PK` 展示；JSON 保留上面的 `table` 和 `columns` 结构。
