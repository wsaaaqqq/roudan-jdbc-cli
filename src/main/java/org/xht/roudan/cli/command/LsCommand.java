package org.xht.roudan.cli.command;

import cn.hutool.json.JSONUtil;
import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.config.CliConfig;
import org.xht.roudan.cli.config.ConnectionStore;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "ls",
        description = "List saved connections with url and user"
)
public class LsCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private Main main;

    @Override
    public Integer call() {
        Map<String, CliConfig> all = ConnectionStore.list();
        if (all.isEmpty()) {
            System.out.println("[]");
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

        System.out.println(JSONUtil.toJsonPrettyStr(list));
        return 0;
    }
}
