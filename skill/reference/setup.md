# 安装与环境排查（仅需要时读取）

已能正常调用且当前任务已确认版本时，不执行本页探测。安装、升级、改 PATH 或跨系统切换应在用户授权范围内，不因普通查询自动升级。

## 1. 确认实际执行链

先用宿主已提供的环境信息。仍有命令缺失、版本不符等问题时，只检查相关命令位置：

| 当前 Shell | 查看命令位置 |
|---|---|
| Bash / zsh / WSL | `command -v roudan java npm` |
| PowerShell | `Get-Command roudan,java,npm -All -ErrorAction SilentlyContinue` |
| Windows cmd | 分别执行 `where roudan`、`where java`、`where npm` |

- 终端所在系统不等于程序运行系统：WSL 可能调用 Windows 的 npm/Java，Windows 也可能通过包装器调用 WSL。
- 只在排查时检查包装器/命令目标；不预设 `/usr/local`、盘符、发行版或 AI 客户端路径，不全盘扫描。
- 驱动、SQL 文件、相对路径、环境变量以实际 CLI/JVM 的运行环境为准。相对路径相对于该进程工作目录；`~` 不保证在任意参数中自动展开。
- 配置目录属于 JVM 的 `user.home`，Windows 与 WSL 通常各自独立。必要时定向查看 Java 的 `java.home`、`user.home`，不要倾倒整个环境或凭据。
- 发现混用时优先保持当前环境的原生工具链；不要自动调用 `wsl`、Windows 可执行文件或改另一套安装。确需跨环境时先确认路径可见性及连接配置归属，不只机械转换盘符。

## 2. 安装 CLI（两种方式选一种）

需要 Java 8+；缺失时可从 [Adoptium](https://adoptium.net/) 选择当前系统/架构的 Java 安装，不能默认使用某个包管理器或管理员权限。JDBC 驱动按 [connections.md](connections.md) 处理，不必先手工搜索全部驱动。

### npm 包

确认 npm 与目标执行环境一致后：

```text
npm install -g roudan-jdbc-cli
roudan --version
```

包内含 JAR，但不含 Java。以实际 `roudan --version` 为准，npm 与 GitHub 的发布版本可能不同；必要时才用 `npm view roudan-jdbc-cli version` 核对。WSL 中 Windows npm 的全局安装不一定会更新 WSL 的 `roudan`。

### 直接运行 JAR（不需要 Node.js）

从 [官方 GitHub Releases](https://github.com/wsaaaqqq/roudan-jdbc-cli/releases) 选择所需版本，下载 `roudan-jdbc-cli.jar` 到用户指定位置：

```text
java -jar "path/to/roudan-jdbc-cli.jar" --version
```

后续将命令前缀 `roudan` 换成上述 `java -jar ...` 即可，不必修改全局 PATH 或建立系统目录。下载后验证版本；有官方摘要时核对摘要。不要从不明地址下载驱动/程序，也不要将下载内容直接管道执行。

仅用户要求更新全局命令时才调整包装器/PATH，保留已有安装的恢复方式。退出码与 JSON 内容矛盾时，检查包装器是否原样传递参数和 Java 退出码，不要重复写入数据库。

## 3. Shell 引号与编码

- 通用示例使用单行命令；SQL 双引号包裹，SQL 字符串使用单引号。文件路径包含空格时必须作为一个参数传入。
- Bash/zsh 的 JSON 单引号示例不能直接假设适用于 cmd 或所有 PowerShell 原生命令传参模式。优先使用执行工具的独立参数数组（JSON 必须是一个参数）；否则按当前 Shell/包装器规则转义，必要时只检查参数传递，不连续盲试 SQL。
- 复杂 SQL 通过工具写入 UTF-8（建议无 BOM）文件，再用 `query/modify/count/exec -f`。不要默认终端重定向、PowerShell 的旧版文件输出或系统编码就是 UTF-8。
- SQL 文件解决的是 SQL 引号和编码问题，不会代替 `-a` 的 JSON 参数。密码、SQL 参数或文件路径不得未经转义拼接进 Shell。

## 4. 安装 skill 本身

将**完整 `skill/` 目录**复制到当前 AI 客户端实际配置的 skill 搜索目录，并命名为 `roudan-jdbc`。`SKILL.md` 与 `reference/` 必须保持相邻，不能只复制入口文件。

同一份内容适用于 Windows、WSL、Linux、macOS；不依赖某客户端特有的 metadata 或环境变量。安装路径、Java 位置等机器约定留在宿主配置中，不回写通用 skill；同步前先核对并备份已安装目录中的自定义内容。
