package org.xht.roudan.cli.command;

import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.config.ConnectionStore;
import org.xht.roudan.cli.output.ResultWriter;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "logout",
        description = "Clear the current saved connection"
)
public class LogoutCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private Main main;

    @Override
    public Integer call() {
        String current = ConnectionStore.getCurrentName();
        if (current == null) {
            ResultWriter.printResult(r -> {
                r.put("success", false);
                r.put("error", "no active connection to logout");
                r.put("errorCode", "NOT_FOUND");
            }, true);
            return 1;
        }

        ConnectionStore.remove(current);

        ResultWriter.printResult(r -> {
            r.put("success", true);
            r.put("message", "logged out, removed '" + current + "'");
        }, true);
        return 0;
    }
}
