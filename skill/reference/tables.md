# tables — 查看表/视图列表

## 语法

```bash
roudan-jdbc-cli tables [选项]
```

## 选项

| 选项 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `--pattern` | String | `%` | 表名过滤模式（SQL LIKE 语法） |

## 输出

```json
{
  "success": true,
  "tables": [
    {"name": "T_USER", "type": "TABLE"},
    {"name": "T_ORDER", "type": "TABLE"},
    {"name": "V_USER_ORDER", "type": "VIEW"}
  ],
  "timeMs": 5
}
```

## 示例

```bash
# 列出所有表
roudan-jdbc-cli tables

# 按模式过滤
roudan-jdbc-cli tables --pattern "T_%"
```
