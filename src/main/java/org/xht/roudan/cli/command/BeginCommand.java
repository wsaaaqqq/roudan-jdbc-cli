package org.xht.roudan.cli.command;

import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.output.ResultWriter;
import org.xht.rd.RD;
import org.xht.rd.RDConfig;
import picocli.CommandLine;

import java.sql.Connection;
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
                    main.getDatasourceName(), main.isShowSql(), main.getConnectTimeout(), main.getSavedName()
            );

            RDConfig.setAutoClose(false);
            Connection conn = RD.getConnection();
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
