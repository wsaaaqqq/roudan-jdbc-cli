# count — 计数

## 语法

```bash
roudan count -s <sql> [选项]
roudan count -f <sql-file> [选项]
```

## 选项

| 选项 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `-s, --sql` | String | - | SQL 语句（与 `-f` 任选其一） |
| `-f, --sql-file` | String | - | UTF-8 SQL 文件路径 |
| `--named` | boolean | false | 命名参数模式 |
| `-a, --args` | String | - | 参数 |

## 实现

框架自动将 SQL 包装为 `SELECT COUNT(1) FROM ( 原始SQL )`，不需要手动写 COUNT。

缺少或空白 SQL 在连接初始化前返回 `PARAM_ERROR`。目标连接须先确认；连接参数放在子命令前。

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
roudan count -s "SELECT ID FROM T_USER WHERE age > ?" -a '[18]'
roudan count -s "SELECT ID FROM T_USER WHERE status = :st" --named -a '{"st":"ACTIVE"}'
```

JSON 参数的 Shell 引号问题见 [setup.md](setup.md)，不需要时不额外读取。
