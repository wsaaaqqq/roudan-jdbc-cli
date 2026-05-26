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
        name = "connections",
        description = "List saved connections"
)
public class ConnectionsCommand implements Callable<Integer> {

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

        String current = ConnectionStore.getCurrentName();
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, CliConfig> e : all.entrySet()) {
            Map<String, Object> item = new LinkedHashMap<>();
            String name = e.getKey().replace(" *", "");
            item.put("name", name);
            item.put("url", e.getValue().getUrl());
            item.put("user", e.getValue().getUser());
            item.put("current", name.equals(current));
            list.add(item);
        }

        ResultWriter.printResult(r -> {
            r.put("success", true);
            r.put("connections", list);
        }, true);
        return 0;
    }
}
