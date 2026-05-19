package org.xht.roudan.cli;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.xht.roudan.cli.command.*;
import org.xht.roudan.cli.config.CliConfig;
import org.xht.roudan.cli.config.ConfigLoader;
import org.xht.roudan.cli.config.ConnectionStore;
import org.xht.roudan.cli.datasource.DataSourceFactory;
import org.xht.roudan.cli.driver.DriverDownloader;
import org.xht.roudan.cli.driver.DriverLoader;
import org.xht.roudan.cli.driver.DriverRegistry;
import org.xht.roudan.cli.output.ResultWriter;
import org.xht.rd.RD;
import org.xht.rd.RDConfig;
import picocli.CommandLine;

import java.sql.Connection;
import java.sql.Driver;
import java.util.List;
import java.util.concurrent.Callable;

@Slf4j
@Getter
@Setter
@CommandLine.Command(
        name = "roudan",
        description = "JDBC CLI tool for AI agents",
        mixinStandardHelpOptions = true,
        version = "0.5.1",
        subcommands = {
                QueryCommand.class,
                CountCommand.class,
                ModifyCommand.class,
                TablesCommand.class,
                DescribeCommand.class,
                TestCommand.class,
                BeginCommand.class,
                CommitCommand.class,
                RollbackCommand.class,
                LoginCommand.class,
                LogoutCommand.class,
                UseCommand.class,
                ConnectionsCommand.class,
                DemoCommand.class,
                ExecCommand.class,
                ImportCommand.class,
                ExportCommand.class,
                GenCommand.class,
                TailCommand.class,
                CommandLine.HelpCommand.class,
                UpdateCommand.class,
                DrvCommand.class,
                EnvCommand.class
        }
)
public class Main implements Callable<Integer> {

    @CommandLine.Option(names = {"-c", "--config"}, description = "YAML config file path")
    private String configFile;

    @CommandLine.Option(names = {"-u", "--url"}, description = "JDBC URL")
    private String jdbcUrl;

    @CommandLine.Option(names = {"-n", "--user"}, description = "Database username")
    private String user;

    @CommandLine.Option(names = {"-p", "--password"}, description = "Database password")
    private String password;

    @CommandLine.Option(names = {"-d", "--driver"}, description = "JDBC driver class name")
    private String driverClass;

    @CommandLine.Option(names = {"-j", "--driver-jar"}, description = "JDBC driver JAR path")
    private String driverJar;

    @CommandLine.Option(names = "--datasource", defaultValue = "default", description = "Datasource name")
    private String datasourceName;

    @CommandLine.Option(names = "--name", description = "Use a saved connection by name (overrides current)")
    private String savedName;

    @CommandLine.Option(names = {"-o", "--output"}, defaultValue = "json", description = "Output format: json, json-pretty, csv, table")
    private String outputFormat;

    @CommandLine.Option(names = "--no-header", description = "Omit column headers in csv/table output")
    private boolean noHeader;

    @CommandLine.Option(names = "--pretty", description = "Pretty-print JSON output")
    private boolean pretty;

    @CommandLine.Option(names = {"--show-sql"}, description = "Print SQL to stderr for debugging")
    private boolean showSql;

    @CommandLine.Option(names = "--connect-timeout", defaultValue = "30000", description = "JDBC connection timeout in milliseconds")
    private int connectTimeout = 30000;

    @CommandLine.Option(names = "--dry-run", description = "Print SQL without executing")
    private boolean dryRun;

    @CommandLine.Option(names = "--gen-completion", description = "Generate shell completion script (output to stdout)")
    private boolean genCompletion;

    private static final ThreadLocal<Connection> TX_CONNECTION = new ThreadLocal<>();

    public static Connection getTxConnection() {
        return TX_CONNECTION.get();
    }

    public static void setTxConnection(Connection conn) {
        TX_CONNECTION.set(conn);
    }

    public static void clearTxConnection() {
        TX_CONNECTION.remove();
    }

    private static final ThreadLocal<Driver> LOADED_DRIVER = new ThreadLocal<>();
    private static final ThreadLocal<String> RESOLVED_URL = new ThreadLocal<>();
    private static final ThreadLocal<String> RESOLVED_USER = new ThreadLocal<>();
    private static final ThreadLocal<String> RESOLVED_PASSWORD = new ThreadLocal<>();

    public static Driver getLoadedDriver() { return LOADED_DRIVER.get(); }
    public static String getResolvedUrl() { return RESOLVED_URL.get(); }
    public static String getResolvedUser() { return RESOLVED_USER.get(); }
    public static String getResolvedPassword() { return RESOLVED_PASSWORD.get(); }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main())
                .setExecutionExceptionHandler(new ErrorHandler())
                .setParameterExceptionHandler(new ParamErrorHandler())
                .execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        if (genCompletion) {
            printBashCompletion();
            return 0;
        }
        CommandLine.usage(this, System.err);
        return 0;
    }

    private void printBashCompletion() {
        System.out.println("# roudan bash completion");
        System.out.println("_roudan_completion() {");
        System.out.println("  local cur=${COMP_WORDS[COMP_CWORD]}");
        System.out.println("  local prev=${COMP_WORDS[COMP_CWORD-1]}");
        System.out.println("  if [ $COMP_CWORD -eq 1 ]; then");
        System.out.println("    COMPREPLY=($(compgen -W \"query count modify tables describe test begin commit rollback login logout use connections demo exec import export gen tail help update drv env\" -- \"$cur\"))");
        System.out.println("  fi");
        System.out.println("}");
        System.out.println("complete -F _roudan_completion roudan");
    }

    public static class ErrorHandler implements CommandLine.IExecutionExceptionHandler {
        @Override
        public int handleExecutionException(Exception ex, CommandLine cmd, CommandLine.ParseResult parseResult) {
            ResultWriter.printError(ex);
            return 1;
        }
    }

    public static class ParamErrorHandler implements CommandLine.IParameterExceptionHandler {
        @Override
        public int handleParseException(CommandLine.ParameterException ex, String[] args) {
            String msg = ex.getMessage();

            // Fuzzy match: check if an unknown argument matches a subcommand
            String unknown = null;
            if (ex instanceof CommandLine.UnmatchedArgumentException) {
                java.util.List<String> unmatched = ((CommandLine.UnmatchedArgumentException) ex).getUnmatched();
                if (unmatched != null && !unmatched.isEmpty()) {
                    unknown = unmatched.get(0);
                }
            } else if (msg != null && msg.contains("'")) {
                int s = msg.indexOf('\'');
                int e = msg.indexOf('\'', s + 1);
                if (s >= 0 && e > s) unknown = msg.substring(s + 1, e);
            }

            if (unknown != null) {
                String suggestion = null;
                int bestDist = Integer.MAX_VALUE;
                for (java.util.Map.Entry<String, CommandLine> e : ex.getCommandLine().getSubcommands().entrySet()) {
                    String name = e.getKey();
                    int dist = levenshtein(unknown.toLowerCase(), name.toLowerCase());
                    if (dist < bestDist) { bestDist = dist; suggestion = name; }
                }
                // Prefer prefix match first, then fall back to Levenshtein ≤ 2
                if (suggestion != null && unknown.length() >= 2 && suggestion.toLowerCase().startsWith(unknown.toLowerCase())) {
                    bestDist = 0;
                }
                if (suggestion != null && bestDist <= 2 && !suggestion.equals(unknown)) {
                    msg = "Unknown command '" + unknown + "'. Did you mean '" + suggestion + "'?";
                }
            }

            ResultWriter.printParamError(msg);
            return 1;
        }

        private static int levenshtein(String a, String b) {
            int[][] dp = new int[a.length() + 1][b.length() + 1];
            for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
            for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
            for (int i = 1; i <= a.length(); i++) {
                for (int j = 1; j <= b.length(); j++) {
                    int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                    dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
                }
            }
            return dp[a.length()][b.length()];
        }
    }

    public static void init(String configFile, String jdbcUrl, String user, String password,
                            String driverClass, String driverJar, String datasourceName,
                            boolean showSql) throws Exception {
        init(configFile, jdbcUrl, user, password, driverClass, driverJar, datasourceName, showSql, 30000, null);
    }

    public static void init(String configFile, String jdbcUrl, String user, String password,
                            String driverClass, String driverJar, String datasourceName,
                            boolean showSql, int connectTimeout) throws Exception {
        init(configFile, jdbcUrl, user, password, driverClass, driverJar, datasourceName, showSql, connectTimeout, null);
    }

    public static void init(String configFile, String jdbcUrl, String user, String password,
                            String driverClass, String driverJar, String datasourceName,
                            boolean showSql, int connectTimeout, String savedName) throws Exception {
        CliConfig config;

        if (configFile != null && !"default".equals(datasourceName)) {
            CliConfig.DatasourceConfig dsConfig = ConfigLoader.loadDatasource(configFile, datasourceName);
            config = new CliConfig();
            config.setUrl(dsConfig.getUrl());
            config.setUser(dsConfig.getUser());
            config.setPassword(dsConfig.getPassword());
            config.setDriverClass(dsConfig.getDriver());
            config.setDriverJar(dsConfig.getDriverJar());
            if (jdbcUrl != null) config.setUrl(jdbcUrl);
            if (user != null) config.setUser(user);
            if (password != null) config.setPassword(password);
            if (driverClass != null) config.setDriverClass(driverClass);
            if (driverJar != null) config.setDriverJar(driverJar);
            if (config.getUrl() == null) {
                throw new IllegalArgumentException("JDBC URL is required.");
            }
        } else {
            config = ConfigLoader.load(configFile, jdbcUrl, user, password, driverClass, driverJar, savedName);
        }

        CliConfig.Settings settings = config.getSettings();
        if (settings == null) {
            settings = new CliConfig.Settings();
        }
        if (showSql) settings.setShowSql(true);
        RDConfig.setShowSql(settings.isShowSql());

        // Check ~/.roudan/drivers/[name]/ for local driver JARs (unless -j explicitly given)
        if (driverJar == null) {
            String connName = savedName != null ? savedName : ConnectionStore.getCurrentName();
            if (connName != null) {
                java.io.File driverDir = new java.io.File(
                        System.getProperty("user.home") + "/.roudan/drivers/" + connName);
                if (driverDir.isDirectory()) {
                    java.io.File[] jars = driverDir.listFiles(f -> f.getName().endsWith(".jar"));
                    if (jars != null && jars.length > 0) {
                        config.setDriverJar(jars[0].getAbsolutePath());
                        System.err.println("[roudan] Using local driver: " + jars[0].getName());
                    }
                }
            }
        }

        // Auto-resolve driver class and JAR from JDBC URL
        if (config.getDriverClass() == null || config.getDriverJar() == null) {
            DriverRegistry.DriverInfo drvInfo = DriverRegistry.resolve(config.getUrl());
            if (drvInfo != null) {
                if (config.getDriverClass() == null) {
                    config.setDriverClass(drvInfo.getDriverClass());
                    System.err.println("[roudan] Auto-detected driver: " + drvInfo.getDriverClass());
                }
                if (config.getDriverJar() == null) {
                    if (DriverRegistry.isFullyCached(drvInfo)) {
                        StringBuilder cached = new StringBuilder(DriverRegistry.cachePath(drvInfo));
                        for (String[] dep : drvInfo.getExtraDependencies()) {
                            cached.append(";").append(DriverRegistry.cachePathExtra(dep[0], dep[1], dep[2]));
                        }
                        config.setDriverJar(cached.toString());
                    } else {
                        try {
                            config.setDriverJar(DriverDownloader.download(drvInfo));
                        } catch (Exception e) {
                            throw new IllegalArgumentException(
                                "Cannot auto-download driver for " + config.getUrl() + ": " + e.getMessage() +
                                ". Provide -d and -j manually.");
                        }
                    }
                }
            }
        }

        Driver driver = DriverLoader.load(config.getDriverJar(), config.getDriverClass());
        LOADED_DRIVER.set(driver);
        RESOLVED_URL.set(config.getUrl());
        RESOLVED_USER.set(config.getUser());
        RESOLVED_PASSWORD.set(config.getPassword());

        javax.sql.DataSource ds = DataSourceFactory.create(driver, config.getUrl(), config.getUser(), config.getPassword(), settings, connectTimeout, config.getDriverClass());
        RD.dataSourceConfig(c -> {
            c.addDataSource(ds, datasourceName);
            c.selectDataSourceDefault(datasourceName);
        });
    }
}
