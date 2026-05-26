package org.xht.roudan.cli.command;

import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.config.ConnectionStore;
import org.xht.roudan.cli.output.ResultWriter;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "rename",
        description = "Rename a saved connection"
)
public class RenameCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private Main main;

    @CommandLine.Parameters(index = "0", arity = "1", description = "Current connection name")
    private String oldName;

    @CommandLine.Parameters(index = "1", arity = "1", description = "New connection name")
    private String newName;

    @Override
    public Integer call() {
        if (ConnectionStore.getByName(oldName) == null) {
            ResultWriter.printResult(r -> {
                r.put("success", false);
                r.put("error", "connection '" + oldName + "' not found");
                r.put("errorCode", "NOT_FOUND");
            }, true);
            return 1;
        }
        if (ConnectionStore.getByName(newName) != null) {
            ResultWriter.printResult(r -> {
                r.put("success", false);
                r.put("error", "connection '" + newName + "' already exists");
                r.put("errorCode", "CONFLICT");
            }, true);
            return 1;
        }

        ConnectionStore.rename(oldName, newName);

        ResultWriter.printResult(r -> {
            r.put("success", true);
            r.put("message", "renamed '" + oldName + "' to '" + newName + "'");
            r.put("oldName", oldName);
            r.put("newName", newName);
        }, true);
        return 0;
    }
}
