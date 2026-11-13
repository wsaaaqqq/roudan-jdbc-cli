# 连接选择、凭据与驱动（仅需要时读取）

示例中的 `dev`、URL、用户、文件路径均须替换为已确认的目标，不是可直接使用的默认值。

## 选择目标，避免隐式切换

1. 从用户请求和当前任务上下文确认主机/端口、数据库/schema、用户。不仅比较 IP 或 JDBC URL，也不要按连接名猜账号权限。
2. 已知保存连接名且不存在配置冲突时，直接单次指定；不要每次列连接或调用 test：

   ```text
   roudan --name dev describe -t T_USER
   ```

3. 未知保存连接名时只执行一次 `roudan connections`，筛选 `connections` 内的 `name/url/user/current`；`roudan ls` 则直接返回数组。两者不输出密码，无须读取整个凭据文件。
4. 新连接或连接故障时，可用 `roudan --name dev test` 验证连通性。`test` 只返回连接状态和数据库产品，不证明目标用户/schema 正确；必要时按已知数据库方言做最小只读身份查询。
5. 将已确认的调用前缀和目标保留在当前任务上下文。只有配置、进程环境或目标改变时才重新核对，不把凭据写进任务笔记。

`use` 会改变当前默认连接；`login` 会保存凭据并设为当前连接。除非用户要求或已明确授权，不执行这些状态变更，不覆盖已有同名连接。连接不明确时先询问，不尝试所有数据库。

## 0.5.6 的配置冲突边界

不要假设 `--name` 总能覆盖其他配置：普通命令中，CLI 字段覆盖 YAML；环境变量仅填补空字段；**保存连接只在 URL 尚未解析时参与**。显式 URL、环境变量 URL、显式/自动发现的 YAML URL 都可能让 `--name` 不生效，缺失字段还可能来自其他来源。

仅在选择连接前或发现目标不符时定向检查这些来源，屏蔽密码；不要把多套配置混在一起后继续写操作：

- CLI 的 `-u/-n/-p/-d/-j`、`-c/--config`、`--datasource`。
- `ROUDAN_JDBC_URL`、`ROUDAN_JDBC_USER`、`ROUDAN_JDBC_PASSWORD`、`ROUDAN_JDBC_DRIVER`、`ROUDAN_JDBC_DRIVER_JAR`；另有 `ROUDAN_JDBC_CONFIG` 可指向配置文件。
- 当前目录 `roudan-config.yaml`，以及 JVM 用户主目录下 `.roudan/config.yaml`。

选择单一、明确的配置来源；若必须临时隔离冲突，应只影响本次进程/经授权的工作目录，不擅自清除用户全局环境或配置。此行为以当前代码为准，不套用笼统的“CLI > 环境变量 > 保存连接 > YAML”排序。

## 新连接与保存

连接参数放在子命令前。`-c` 接受 CLI 配置格式的 YAML，例如：

```yaml
datasources:
  default:
    url: jdbc:postgresql://db.example:5432/app
    user: app_user
    password: "<provided-locally>"
    driver: org.postgresql.Driver
    driverJar: path/to/postgresql.jar
```

```text
roudan -c datasource.yml test
roudan -c datasource.yml query -s "SELECT ID FROM T_USER WHERE ID=1" --limit 10
```

用户明确要求保存该连接时再用：

```text
roudan login -f datasource.yml --name dev
```

`login -f` 还支持扫描应用 YAML 的数据源配置，不能假设普通命令的 `-c` 也接受任意 Spring 配置结构。保存位置是 JVM `user.home` 下的 `.roudan/connections.json`；密码是本地明文，权限应限于当前用户。既有环境变量/受控配置可避免密码进入命令行，但不等于加密。不要在回答、日志、版本库或 skill 中暴露凭据，不默认推荐明文 `-p` 示例。

## 驱动：失败时再排查

- 已有经过验证的连接/驱动就复用。CLI 可根据 URL 自动解析部分数据库驱动，使用缓存或尝试下载；是否允许联网应遵守当前环境权限，离线时提供本地 JAR。
- 驱动缺失、类加载失败时，优先检查已知路径；再按宿主文件查找规则，在项目、JDBC 缓存或已知 Maven 仓库内限范围查找。不要绑定某个搜索工具、全盘扫描或每次重新搜索。
- 选择匹配当前 Java 和数据库版本的驱动；不要机械选版本号最大的 JAR。路径必须能被实际 JVM 访问，不假设 Windows 与 WSL 共享主目录。
- 需要显式指定时使用 `roudan -u "<jdbc-url>" -n "<user>" -d "<driver-class>" -j "path/to/driver.jar" test`，凭据由用户批准的来源提供。
- 网络不可达、认证失败、权限不足不等于驱动错误；结合 stderr、异常原因和 `errorCode` 判断，不盲目换 JAR/账户重试。驱动仅从数据库厂商、官方项目或可信制品仓库获取；找不到则询问用户。
