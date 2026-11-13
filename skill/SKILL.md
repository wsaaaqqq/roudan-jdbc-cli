---
name: roudan-jdbc
description: Execute SQL and inspect database schemas with the roudan JDBC CLI. Use for database queries, writes, SQL scripts, connection troubleshooting, and data import/export.
---

# roudan-jdbc

使用当前环境的 `roudan` 操作数据库；不依赖特定 AI 客户端或 Windows/WSL 安装路径。以下常用语法以 CLI 0.5.6 为基线。

## 开始：只做必要检查

1. 当前任务已确认命令、版本和连接时直接复用，不重复检查环境、读取帮助或列连接。
2. 首次使用且版本未知时执行一次 `roudan --version`；命令缺失、路径或版本异常才读 [setup.md](reference/setup.md)。
3. 确认目标主机、数据库/schema、用户；不因存在默认连接就直接使用。已知连接名时优先 `--name`，不要自动 `use`、`login` 或保存密码。排除环境变量/YAML 覆盖目标的冲突，详见 [connections.md](reference/connections.md)。
4. 常用命令直接使用；参数不确定或不兼容时只查该子命令 `--help`，不要预先读取所有参考。

## 常用命令速查

`dev` 是示例连接名，须替换为已确认的目标；表名、字段和 SQL 同样是示例。连接选项放在子命令前。

| 任务 | 命令 |
|---|---|
| 小范围查询 | `roudan --name dev query -s "SELECT ID, NAME FROM T_USER WHERE ID=1" --limit 10` |
| 查表结构 | `roudan --name dev describe -t T_USER` |
| 查表名 | `roudan --name dev tables --pattern "T_USER%"` |
| 计数 | `roudan --name dev count -s "SELECT ID FROM T_USER WHERE STATUS='ACTIVE'"` |
| 单条写入 | `roudan --name dev modify -s "UPDATE T_USER SET NAME='example' WHERE ID=1"` |
| 批量事务 | `roudan --name dev exec -f migration.sql` |
| 导出到文件 | `roudan --name dev export -s "SELECT ID, NAME FROM T_USER" --format json -o result.json` |

`query/modify/count/exec` 的 SQL 用 `-s` 或 `-f`，不要当位置参数。复杂 SQL 使用 UTF-8 文件；涉及 JSON 参数和 Shell 转义时按需读 [query.md](reference/query.md) / [setup.md](reference/setup.md)。`import`、`gen`、`tail` 的特殊用法只查对应 `--help`。

## 节省返回量

- 默认紧凑 JSON，不默认加 `--pretty`；用户需要人工阅读时再用 table。`query`/`describe` 可在子命令后加 `-o table`；**`export -o` 是文件路径**。
- 指定字段、WHERE 条件和小行数，避免无界 `SELECT *`。`--limit`/分页是内存截取，不限制数据库扫描；大数据用对应数据库的 SQL 限行或聚合。
- 已知连接不重复列出；未知连接只列一次并筛选。大结果写文件，返回必要摘要，不把完整数据、帮助或脚本重复回显。

## 写入安全

- 只执行用户授权范围内的变更；缺少目标、条件或授权时先澄清。关联写入与验证串行执行，验证影响行数和关键数据。
- 批量事务使用一次 `exec`；**多次 CLI 调用不共享事务**，不要串联独立的 `begin`、`modify`、`commit` 进程。
- DDL/显式事务控制可能破坏回滚边界；auto 模式可能部分提交。`--dry-run` 仅拆分脚本，不执行 SQL，不预检语法或影响行数。复杂脚本先读 [exec.md](reference/exec.md)。
- 写入失败或结果不确定时先核实数据库状态，不盲目重试；`affectedRows:0` 不自动等于失败。

## 判断结果

- 同时检查退出码和结果：CLI 0=成功、1=失败；若与 JSON 的 `success` 矛盾，先排查启动器，不重试写入。结果读 stdout，诊断读 stderr。
- `query` 的 `cols` 按 SELECT 顺序，`rows` 按位置对应并保留 NULL；`rowCount` 是截取前总行数，table 空结果为 `0 rows`。
- 不假设所有命令同一结构：`exec` 返回逐语句摘要（SELECT 仅行数）；失败时检查 `results` 或 `partialResults`。`ls` 是数组；导出应指定文件，避免 stdout 混入数据和摘要。

## 按需参考，不要全部加载

| 遇到的问题 | 只读取 |
|---|---|
| 安装、版本、跨环境路径、Shell 转义 | [setup.md](reference/setup.md) |
| 连接选择、配置冲突、驱动、凭据 | [connections.md](reference/connections.md) |
| 多语句、事务、部分失败 | [exec.md](reference/exec.md) |
| 参数绑定、分页、查询输出 | [query.md](reference/query.md) |
| 单条写入参数 | [modify.md](reference/modify.md) |
| 其他特定用法 | [count.md](reference/count.md)、[tables.md](reference/tables.md)、[describe.md](reference/describe.md)、[test.md](reference/test.md) 中对应的一份 |
