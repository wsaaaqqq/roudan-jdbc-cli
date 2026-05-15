package org.xht.roudan.cli.command;

import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.config.ConnectionStore;
import org.xht.roudan.cli.output.ResultWriter;
import picocli.CommandLine;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "env",
        description = "Show environment and debug information"
)
public class EnvCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private Main main;

    @Override
    public Integer call() {
        long start = System.currentTimeMillis();

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("version", "0.2.0");
        info.put("javaVersion", System.getProperty("java.version"));
        info.put("javaHome", System.getProperty("java.home"));
        info.put("os", System.getProperty("os.name"));
        info.put("arch", System.getProperty("os.arch"));
        info.put("userHome", System.getProperty("user.home"));
        info.put("installDir", System.getProperty("user.home") + "/.roudan-cli");
        info.put("jarPath", EnvCommand.class.getProtectionDomain()
                .getCodeSource().getLocation().getPath());
        info.put("currentConnection", ConnectionStore.getCurrentName());
        info.put("configFile", main.getConfigFile());
        info.put("showSql", main.isShowSql());
        info.put("connectTimeout", main.getConnectTimeout());
        info.put("outputFormat", main.getOutputFormat());
        info.put("savedConnections", ConnectionStore.list().size());

        long elapsed = System.currentTimeMillis() - start;
        ResultWriter.printResult(r -> {
            r.put("success", true);
            r.putAll(info);
            r.put("timeMs", elapsed);
        }, main.isPretty());
        return 0;
    }
}
