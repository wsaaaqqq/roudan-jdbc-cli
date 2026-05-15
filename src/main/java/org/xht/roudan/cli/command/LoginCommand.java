package org.xht.roudan.cli.command;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.config.ConnectionStore;
import org.xht.roudan.cli.output.ResultWriter;
import org.yaml.snakeyaml.Yaml;
import picocli.CommandLine;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

@CommandLine.Command(
        name = "login",
        description = "Save connection for reuse from CLI args or YAML config file"
)
public class LoginCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private Main main;

    @CommandLine.Option(names = "--name", description = "Connection save name (overrides auto-detected name)")
    private String name;

    @CommandLine.Option(names = {"-f", "--file"}, description = "YAML config file path")
    private String file;

    @CommandLine.Option(names = {"-j", "--driver-jar"}, description = "JDBC driver JAR path (overrides YAML)")
    private String cliDriverJar;

    @CommandLine.Option(names = {"-u", "--url"}, description = "JDBC URL (overrides YAML)")
    private String cliUrl;

    @CommandLine.Option(names = {"-n", "--user"}, description = "Database username (overrides YAML)")
    private String cliUser;

    @CommandLine.Option(names = {"-p", "--password"}, description = "Database password (overrides YAML)")
    private String cliPassword;

    @CommandLine.Option(names = {"-d", "--driver"}, description = "JDBC driver class (overrides YAML)")
    private String cliDriver;

    @CommandLine.Parameters(index = "0", arity = "0..1", description = "Datasource name (required when YAML has multiple datasources)")
    private String dsName;

    @Override
    public Integer call() throws Exception {
        String url = null, user = null, password = null;
        String driverClass = null, driverJar = null;

        if (file != null) {
            if (!FileUtil.exist(file)) {
                ResultWriter.printResult(r -> {
                    r.put("success", false);
                    r.put("error", "Config file not found: " + file);
                    r.put("errorCode", "FILE_NOT_FOUND");
                }, true);
                return 1;
            }

            Map<String, Object> root;
            try (InputStream in = new FileInputStream(file)) {
                root = new Yaml().load(in);
            }

            List<DsEntry> found = scan(root, null);

            if (found.isEmpty()) {
                ResultWriter.printResult(r -> {
                    r.put("success", false);
                    r.put("error", "No datasource found in YAML (no map with 'driver-class-name' or 'driver')");
                    r.put("errorCode", "PARSE_ERROR");
                }, true);
                return 1;
            }

            DsEntry selected = null;
            if (found.size() == 1 && dsName == null) {
                selected = found.get(0);
                if ("datasource".equals(selected.name)) {
                    selected.name = "default";
                }
            } else if (dsName != null) {
                for (DsEntry e : found) {
                    if (e.name.equals(dsName)) {
                        selected = e;
                        break;
                    }
                }
                if (selected == null) {
                    String names = found.stream().map(e -> e.name).collect(Collectors.joining(", "));
                    ResultWriter.printResult(r -> {
                        r.put("success", false);
                        r.put("error", "Datasource '" + dsName + "' not found. Available: " + names);
                        r.put("errorCode", "NOT_FOUND");
                    }, true);
                    return 1;
                }
            } else {
                String names = found.stream().map(e -> e.name).collect(Collectors.joining(", "));
                ResultWriter.printResult(r -> {
                    r.put("success", false);
                    r.put("error", "Multiple datasources found, specify one: " + names);
                    r.put("errorCode", "AMBIGUOUS");
                }, true);
                return 1;
            }

            url = selected.url;
            user = selected.user;
            password = selected.password;
            driverClass = selected.driverClass;
            driverJar = selected.driverJar;

            if (name == null) {
                name = selected.name;
            }

            // CLI args override YAML
            if (cliUrl != null) url = cliUrl;
            if (cliUser != null) user = cliUser;
            if (cliPassword != null) password = cliPassword;
            if (cliDriver != null) driverClass = cliDriver;
            if (cliDriverJar != null) driverJar = cliDriverJar;

            // Parent CLI args as next fallback (when -f is used without local options)
            if (main.getJdbcUrl() != null) url = main.getJdbcUrl();
            if (main.getUser() != null) user = main.getUser();
            if (main.getPassword() != null) password = main.getPassword();
            if (main.getDriverClass() != null) driverClass = main.getDriverClass();
            if (main.getDriverJar() != null) driverJar = main.getDriverJar();
        } else {
            url = main.getJdbcUrl();
            user = main.getUser();
            password = main.getPassword();
            driverClass = main.getDriverClass();
            driverJar = main.getDriverJar();

            // LoginCommand-local options also work without -f
            if (cliUrl != null) url = cliUrl;
            if (cliUser != null) user = cliUser;
            if (cliPassword != null) password = cliPassword;
            if (cliDriver != null) driverClass = cliDriver;
            if (cliDriverJar != null) driverJar = cliDriverJar;

            if (name == null) name = "default";
        }

        // CLI args override YAML / act as fallback
        if (main.getJdbcUrl() != null) url = main.getJdbcUrl();
        if (main.getUser() != null) user = main.getUser();
        if (main.getPassword() != null) password = main.getPassword();
        if (main.getDriverClass() != null) driverClass = main.getDriverClass();
        if (main.getDriverJar() != null) driverJar = main.getDriverJar();

        // Auto-resolve driver from URL (if missing)
        if (url != null && (driverClass == null || driverJar == null)) {
            org.xht.roudan.cli.driver.DriverRegistry.DriverInfo drvInfo = org.xht.roudan.cli.driver.DriverRegistry.resolve(url);
            if (drvInfo != null) {
                if (driverClass == null) driverClass = drvInfo.getDriverClass();
                if (driverJar == null) {
                    String cp = org.xht.roudan.cli.driver.DriverRegistry.cachePath(drvInfo);
                    if (new java.io.File(cp).exists()) {
                        driverJar = cp;
                    } else if (drvInfo.isOnMavenCentral()) {
                        try {
                            driverJar = org.xht.roudan.cli.driver.DriverDownloader.download(drvInfo);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }

        if (url == null || driverClass == null || driverJar == null) {
            ResultWriter.printResult(r -> {
                r.put("success", false);
                r.put("error", "login requires --url (or -f with YAML). For unknown databases, provide -d and -j.");
                r.put("errorCode", "PARAM_ERROR");
            }, true);
            return 1;
        }

        ConnectionStore.save(name, url, user, password, driverClass, driverJar);

        ResultWriter.printResult(r -> {
            r.put("success", true);
            r.put("message", "connection saved as '" + name + "'");
            r.put("name", name);
        }, true);
        return 0;
    }

    @SuppressWarnings("unchecked")
    private static List<DsEntry> scan(Map<String, Object> node, String parentKey) {
        List<DsEntry> result = new ArrayList<>();
        if (node == null) return result;

        for (Map.Entry<String, Object> e : node.entrySet()) {
            String key = e.getKey();
            Object val = e.getValue();
            if (!(val instanceof Map)) continue;

            Map<String, Object> child = (Map<String, Object>) val;

            boolean hasDriver = child.containsKey("driver-class-name") || child.containsKey("driver");
            if (hasDriver) {
                DsEntry entry = new DsEntry();
                entry.name = key;
                entry.driverClass = strVal(child, "driver-class-name");
                if (entry.driverClass == null) entry.driverClass = strVal(child, "driver");
                entry.url = strVal(child, "url");
                if (entry.url == null) entry.url = strVal(child, "jdbc-url");
                entry.user = strVal(child, "username");
                if (entry.user == null) entry.user = strVal(child, "user");
                entry.password = strVal(child, "password");
                entry.driverJar = strVal(child, "driverJar");
                result.add(entry);
            }

            result.addAll(scan(child, key));
        }

        return result;
    }

    private static String strVal(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null && StrUtil.isNotBlank(val.toString()) ? val.toString().trim() : null;
    }

    private static class DsEntry {
        String name, url, user, password, driverClass, driverJar;
    }
}
