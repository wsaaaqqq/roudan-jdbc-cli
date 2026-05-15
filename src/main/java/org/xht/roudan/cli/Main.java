package org.xht.roudan.cli;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.xht.roudan.cli.command.*;
import org.xht.roudan.cli.config.CliConfig;
import org.xht.roudan.cli.config.ConfigLoader;
import org.xht.roudan.cli.datasource.DataSourceFactory;
import org.xht.roudan.cli.driver.DriverLoader;
import org.xht.roudan.cli.output.ResultWriter;
import org.xht.rd.RD;
import org.xht.rd.RDConfig;
import picocli.CommandLine;

import java.sql.Connection;
import java.sql.Driver;
import java.util.concurrent.Callable;

@Slf4j
@Getter
@Setter
@CommandLine.Command(
        name = "rd",
        aliases = {"roudan-jdbc-cli"},
        description = "JDBC CLI tool for AI agents",
        mixinStandardHelpOptions = true,
        version = "0.0.1",
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
                ConnectionsCommand.class
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
    public Integer call() {
        CommandLine.usage(this, System.err);
        return 0;
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
            ResultWriter.printParamError(ex.getMessage());
            return 1;
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
            if (config.getUrl() == null || config.getDriverClass() == null || config.getDriverJar() == null) {
                throw new IllegalArgumentException("JDBC URL, driver class, and driver JAR are required.");
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
