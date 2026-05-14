package org.xht.roudan.cli.command;

import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.output.ResultWriter;
import picocli.CommandLine;

import java.sql.Connection;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "commit",
        description = "Commit the current transaction"
)
public class CommitCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private Main main;

    @Override
    public Integer call() throws Exception {
        long start = System.currentTimeMillis();
        try {
            Connection conn = Main.getTxConnection();
            if (conn == null) {
                ResultWriter.printResult(r -> {
                    r.put("success", false);
                    r.put("error", "No active transaction");
                    r.put("errorCode", "TX_ERROR");
                    r.put("timeMs", System.currentTimeMillis() - start);
                }, main.isPretty());
                return 1;
            }

            conn.commit();
            conn.close();
            Main.clearTxConnection();

            long elapsed = System.currentTimeMillis() - start;
            ResultWriter.printResult(r -> {
                r.put("success", true);
                r.put("message", "Transaction committed");
                r.put("timeMs", elapsed);
            }, main.isPretty());
            return 0;
        } catch (Exception e) {
            Main.clearTxConnection();
            ResultWriter.printError(e, System.currentTimeMillis() - start);
            return 1;
        }
    }
}
