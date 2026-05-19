package org.xht.roudan.cli.command;

import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.config.CliConfig;
import org.xht.roudan.cli.config.ConnectionStore;
import org.xht.roudan.cli.output.ResultWriter;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "ls",
        description = "List saved connection names"
)
public class LsCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private Main main;

    @Override
    public Integer call() {
        Map<String, CliConfig> all = ConnectionStore.list();
        if (all.isEmpty()) {
            ResultWriter.printResult(r -> {
                r.put("success", true);
                r.put("connections", new ArrayList<>());
                r.put("message", "no saved connections");
            }, true);
            return 0;
        }

        List<String> names = new ArrayList<>(all.keySet());

        ResultWriter.printResult(r -> {
            r.put("success", true);
            r.put("connections", names);
        }, true);
        return 0;
    }
}
