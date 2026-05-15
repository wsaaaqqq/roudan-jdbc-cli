package org.xht.roudan.cli.command;

import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.config.ConnectionStore;
import org.xht.roudan.cli.output.ResultWriter;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "use",
        description = "Switch to a saved connection"
)
public class UseCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private Main main;

    @CommandLine.Parameters(index = "0", arity = "1", description = "Connection name")
    private String name;

    @Override
    public Integer call() {
        if (ConnectionStore.getByName(name) == null) {
            ResultWriter.printResult(r -> {
                r.put("success", false);
                r.put("error", "connection '" + name + "' not found");
                r.put("errorCode", "NOT_FOUND");
            }, true);
            return 1;
        }

        ConnectionStore.setCurrent(name);

        ResultWriter.printResult(r -> {
            r.put("success", true);
            r.put("message", "switched to '" + name + "'");
            r.put("name", name);
        }, true);
        return 0;
    }
}
