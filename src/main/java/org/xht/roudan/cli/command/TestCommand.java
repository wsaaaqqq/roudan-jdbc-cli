package org.xht.roudan.cli.command;

import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.output.ResultWriter;
import org.xht.rd.RD;
import picocli.CommandLine;

import java.sql.Connection;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "test",
        description = "Test database connection"
)
public class TestCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private Main main;

    @Override
    public Integer call() throws Exception {
        long start = System.currentTimeMillis();
        try {
            Main.init(main.getConfigFile(), main.getJdbcUrl(), main.getUser(),
                    main.getPassword(), main.getDriverClass(), main.getDriverJar(),
                    main.getDatasourceName(), main.isShowSql(), main.getConnectTimeout());

            String dbProduct;
            try (Connection conn = RD.getConnection()) {
                dbProduct = conn.getMetaData().getDatabaseProductName()
                        + " " + conn.getMetaData().getDatabaseProductVersion();
            }

            long elapsed = System.currentTimeMillis() - start;
            ResultWriter.printResult(r -> {
                r.put("success", true);
                r.put("message", "connection ok");
                r.put("dbProduct", dbProduct);
                r.put("timeMs", elapsed);
            }, main.isPretty());
            return 0;
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            ResultWriter.printResult(r -> {
                r.put("success", false);
                r.put("message", "connection failed");
                r.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
                r.put("errorCode", "CONNECTION_ERROR");
                r.put("timeMs", elapsed);
            }, main.isPretty());
            return 1;
        }
    }
}
