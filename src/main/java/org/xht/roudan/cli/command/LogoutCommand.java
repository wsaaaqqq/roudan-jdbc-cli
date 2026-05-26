package org.xht.roudan.cli.command;

import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.config.ConnectionStore;
import org.xht.roudan.cli.output.ResultWriter;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "logout",
        description = "Remove a saved connection"
)
public class LogoutCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private Main main;

    @CommandLine.Option(names = "--name", description = "Connection name to remove (default: current)")
    private String name;

    @Override
    public Integer call() {
        String target = name != null ? name : ConnectionStore.getCurrentName();
        if (target == null) {
            ResultWriter.printResult(r -> {
                r.put("success", false);
                r.put("error", "no connection specified and no active connection");
                r.put("errorCode", "NOT_FOUND");
            }, true);
            return 1;
        }

        if (ConnectionStore.getByName(target) == null) {
            ResultWriter.printResult(r -> {
                r.put("success", false);
                r.put("error", "connection '" + target + "' not found");
                r.put("errorCode", "NOT_FOUND");
            }, true);
            return 1;
        }

        ConnectionStore.remove(target);

        ResultWriter.printResult(r -> {
            r.put("success", true);
            r.put("message", "removed connection '" + target + "'");
            r.put("name", target);
        }, true);
        return 0;
    }
}
