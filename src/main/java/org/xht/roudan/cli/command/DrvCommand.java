package org.xht.roudan.cli.command;

import cn.hutool.core.io.FileUtil;
import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.driver.DriverRegistry;
import org.xht.roudan.cli.output.ResultWriter;
import picocli.CommandLine;

import java.io.File;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "drv",
        description = "Manage cached JDBC drivers"
)
public class DrvCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private Main main;

    @Override
    public Integer call() {
        String home = System.getProperty("user.home");
        File dir = new File(home + "/.roudan-cli/drivers");
        File[] jars = dir.exists() ? dir.listFiles((d, n) -> n.endsWith(".jar")) : new File[0];

        if (jars == null || jars.length == 0) {
            ResultWriter.printResult(r -> {
                r.put("success", true);
                r.put("drivers", java.util.Collections.emptyList());
                r.put("message", "no cached drivers");
                r.put("cacheDir", dir.getAbsolutePath());
            }, main.isPretty());
            return 0;
        }

        java.util.List<java.util.Map<String, Object>> list = new java.util.ArrayList<>();
        for (File f : jars) {
            java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
            m.put("file", f.getName());
            m.put("size", FileUtil.readableFileSize(f));
            m.put("path", f.getAbsolutePath());
            list.add(m);
        }

        ResultWriter.printResult(r -> {
            r.put("success", true);
            r.put("drivers", list);
            r.put("count", list.size());
            r.put("cacheDir", dir.getAbsolutePath());
        }, main.isPretty());
        return 0;
    }
}
