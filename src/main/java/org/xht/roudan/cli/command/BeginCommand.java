package org.xht.roudan.cli.command;

import cn.hutool.core.util.StrUtil;
import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.output.ResultWriter;
import org.xht.rd.RD;
import picocli.CommandLine;

import java.sql.Connection;
import java.sql.Driver;
import java.util.Properties;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "begin",
        description = "Begin a database transaction"
)
public class BeginCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private Main main;

    @Override
    public Integer call() throws Exception {
        long start = System.currentTimeMillis();
        try {
            Main.init(
                    main.getConfigFile(), main.getJdbcUrl(), main.getUser(),
                    main.getPassword(), main.getDriverClass(), main.getDriverJar(),
                    main.getDatasourceName(), main.isShowSql(), main.getConnectTimeout()
            );

            Driver driver = Main.getLoadedDriver();
            String url = Main.getResolvedUrl();
            String user = Main.getResolvedUser();
            String password = Main.getResolvedPassword();

            Properties props = new Properties();
            if (StrUtil.isNotBlank(user)) {
                props.setProperty("user", user);
            }
            if (password != null) {
                props.setProperty("password", password);
            }

            Connection conn = driver.connect(url, props);
            if (conn == null) {
                throw new IllegalStateException("Driver could not connect to: " + url);
            }
            conn.setAutoCommit(false);
            RD.setConnection(conn);
            Main.setTxConnection(conn);

            long elapsed = System.currentTimeMillis() - start;
            ResultWriter.printResult(r -> {
                r.put("success", true);
                r.put("message", "Transaction started");
                r.put("timeMs", elapsed);
            }, main.isPretty());
            return 0;
        } catch (Exception e) {
            ResultWriter.printError(e, System.currentTimeMillis() - start);
            return 1;
        }
    }
}
