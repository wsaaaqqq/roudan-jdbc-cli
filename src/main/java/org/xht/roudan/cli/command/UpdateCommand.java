package org.xht.roudan.cli.command;

import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.output.ResultWriter;
import picocli.CommandLine;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "update",
        description = "Update roudan-jdbc-cli to the latest version"
)
public class UpdateCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private Main main;

    @CommandLine.Option(names = "--version", description = "Specific version to install (default: latest)")
    private String targetVersion;

    @Override
    public Integer call() throws Exception {
        long start = System.currentTimeMillis();
        String ver = targetVersion != null ? targetVersion : "latest";
        String url = ver.equals("latest")
                ? "https://github.com/wsaaaqqq/roudan-jdbc-cli/releases/latest/download/roudan-jdbc-cli.jar"
                : "https://github.com/wsaaaqqq/roudan-jdbc-cli/releases/download/v" + ver + "/roudan-jdbc-cli.jar";

        String home = System.getProperty("user.home");
        Path jarPath = Paths.get(home, ".roudan-cli", "lib", "roudan-jdbc-cli.jar");
        Path backupPath = Paths.get(home, ".roudan-cli", "lib", "roudan-jdbc-cli.jar.bak");

        System.err.print("[roudan] Downloading " + ver + "... ");
        System.err.flush();

        try (InputStream in = new URL(url).openStream()) {
            Files.createDirectories(jarPath.getParent());

            // Backup old jar
            if (Files.exists(jarPath)) {
                Files.copy(jarPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            }

            Files.copy(in, jarPath, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(backupPath);
        } catch (Exception e) {
            // Restore backup
            if (Files.exists(backupPath)) {
                Files.move(backupPath, jarPath, StandardCopyOption.REPLACE_EXISTING);
            }
            ResultWriter.printResult(r -> {
                r.put("success", false);
                r.put("error", "Update failed: " + e.getMessage());
                r.put("errorCode", "UPDATE_ERROR");
                r.put("timeMs", System.currentTimeMillis() - start);
            }, main.isPretty());
            return 1;
        }

        long elapsed = System.currentTimeMillis() - start;
        ResultWriter.printResult(r -> {
            r.put("success", true);
            r.put("message", "updated to " + ver);
            r.put("version", ver);
            r.put("timeMs", elapsed);
        }, main.isPretty());
        return 0;
    }
}
