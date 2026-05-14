package org.xht.roudan.cli;

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

import java.sql.Driver;
import java.util.concurrent.Callable;

@Slf4j
@CommandLine.Command(
        name = "roudan-jdbc-cli",
        description = "JDBC CLI tool for AI agents",
        mixinStandardHelpOptions = true,
        version = "0.0.1",
        subcommands = {
                QueryCommand.class,
                CountCommand.class,
                ModifyCommand.class,
                TablesCommand.class,
                DescribeCommand.class,
                TestCommand.class
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

    @CommandLine.Option(names = {"-o", "--output"}, defaultValue = "json", description = "Output format: json, json-pretty, csv, table")
    private String outputFormat;

    @CommandLine.Option(names = "--no-header", description = "Omit column headers in csv/table output")
    private boolean noHeader;

    @CommandLine.Option(names = "--pretty", description = "Pretty-print JSON output")
    private boolean pretty;

    @CommandLine.Option(names = {"--show-sql"}, description = "Print SQL to stderr for debugging")
    private boolean showSql;

    public String getOutputFormat() { return outputFormat; }
    public boolean isNoHeader() { return noHeader; }
    public boolean isPretty() { return pretty; }

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
        CliConfig config = ConfigLoader.load(configFile, jdbcUrl, user, password, driverClass, driverJar);

        RDConfig.setShowSql(showSql);

        Driver driver = DriverLoader.load(config.getDriverJar(), config.getDriverClass());
        javax.sql.DataSource ds = DataSourceFactory.create(driver, config.getUrl(), config.getUser(), config.getPassword());
        RD.dataSourceConfig(c -> c.addDataSource(ds, datasourceName));
    }
}
