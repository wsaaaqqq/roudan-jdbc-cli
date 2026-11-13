# exec — 多语句与事务

仅复杂脚本、事务或部分失败时读取。以下以 0.5.6 为基线；示例连接名、表名和文件须替换，写入需有用户授权。

## 语法

```text
roudan --name dev exec -f migration.sql
roudan --name dev exec -s "UPDATE T_USER SET STATUS='ACTIVE' WHERE ID=1; SELECT ID FROM T_USER WHERE ID=1"
roudan --name dev exec -f migration.sql --dry-run
```

`-s/--sql` 或 `-f/--file` 提供非空 SQL；`--mode` 只使用 `transaction`（默认）或 `auto`。SQL 文件按 UTF-8 读取。

## 执行模式

| 模式 | 失败处理 | 数据状态 |
|---|---|---|
| transaction | 同一 JDBC 连接逐条执行；首个失败立即停止并尝试回滚 | 全成功才提交；受数据库事务支持的 DML 可整体回滚 |
| auto | 每条语句自动提交；失败后继续后续语句 | 可能部分成功，不能整体回滚 |

两种模式只要有语句失败，顶层 `success:false`、退出码 1；全部成功退出 0。不要把 auto 模式的后续成功或最后一条成功当作整批成功。

**事务边界**：DDL（含部分数据库的 TRUNCATE）、非事务表、脚本中的 COMMIT/ROLLBACK/自动提交设置、序列及外部副作用不保证回滚。即使输出 `rolledBack:true`，也不能据此宣称这类副作用全部撤销。需要全有全无时先确认脚本和数据库支持，不能把 DDL 与 DML 混在一起后承诺安全。

独立 CLI 调用不共享连接/事务；不要分多次调用 `begin`、`modify`、`commit` 来组织批量事务。也不要通过自动追加 COMMIT 来“修复”脚本失败。

## dry-run 不是什么

`--dry-run` 仅拆分并输出语句，不执行 SQL、校验数据库语法或返回实际影响行数。当前仍会初始化连接配置/驱动，可能涉及联网，不是离线 SQL 校验器。

它不是事务内执行后回滚的预检。不要使用未在当前版本帮助中出现的选项。脚本拆分也不是完整数据库客户端解释器：存储过程块、`GO`、`DELIMITER` 等需另行确认支持，不能直接套用普通分号脚本的处理方式。

## 读取结果

- 成功或 auto 模式报告：`mode`、`statementCount`、`results`。
- transaction 语句失败报告：`statementIndex`、`error`、`errorCode`、`partialResults`；回滚成功后带 `rolledBack:true`。其他异常可能只有错误 JSON，不假设失败时总有 partialResults 或回滚标记。
- 逐条结果含 `statementIndex`（**从 0 开始**）、原始 `sql`、`success`；DML 为 `type:update` 和 `affectedRows`，SELECT 为 `type:query` 和 `rowCount`，失败为 `type:error`。
- SELECT 只返回行数，不返回查询数据；需要关键数据时另用有条件的 `query`。

transaction 失败的摘要示例（省略 SQL、耗时及部分字段）：

```json
{"success":false,"statementIndex":1,"rolledBack":true,"errorCode":"SQL_ERROR","partialResults":[{"success":true,"type":"update","affectedRows":1,"statementIndex":0},{"success":false,"type":"error","statementIndex":1}]}
```

`partialResults` 中某条执行成功不代表已提交；它可能随后随事务回滚。UPDATE/DELETE 的 0 行不会自动触发失败，应与业务预期比对。

## 验证与节省输出

1. 写入前确认目标、条件及预期行数；需要时备份受影响数据，准备恢复办法。
2. 执行和后续验证必须串行，不能一边写入一边查询后把竞态结果当验证。
3. 失败先核实已提交/回滚状态，尤其是 auto、DDL、超时和连接中断；不直接重放整份脚本。
4. `exec` 输出会包含原 SQL。大脚本结果可写入受控文件，只提取成功状态、失败序号、影响行数和必要错误；不把敏感 SQL 或完整报告全部回显。
