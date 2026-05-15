package org.xht.roudan.cli.command;

import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.config.ConnectionStore;
import org.xht.roudan.cli.output.ResultWriter;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "demo",
        description = "Start a demo H2 in-memory database for testing"
)
public class DemoCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private Main main;

    @Override
    public Integer call() throws Exception {
        long start = System.currentTimeMillis();

        String url = "jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1";
        String user = "sa";
        String password = "";
        String driverClass = "org.h2.Driver";

        // Use H2 from Maven cache if available, otherwise from classpath
        String h2Cache = System.getProperty("user.home")
                + "/.m2/repository/com/h2database/h2/2.2.224/h2-2.2.224.jar";
        String jar;
        if (new java.io.File(h2Cache).exists()) {
            jar = h2Cache;
        } else {
            // Try classpath (H2 may be bundled)
            try {
                Class.forName("org.h2.Driver");
                jar = null; // DriverLoader will handle null
            } catch (ClassNotFoundException e) {
                // Download from Maven Central
                org.xht.roudan.cli.driver.DriverRegistry.DriverInfo info =
                        org.xht.roudan.cli.driver.DriverRegistry.resolve(url);
                if (info != null) {
                    try {
                        jar = org.xht.roudan.cli.driver.DriverDownloader.download(info);
                    } catch (Exception ex) {
                        ResultWriter.printResult(r -> {
                            r.put("success", false);
                            r.put("error", "Cannot download H2 driver: " + ex.getMessage());
                            r.put("errorCode", "DOWNLOAD_ERROR");
                            r.put("timeMs", System.currentTimeMillis() - start);
                        }, true);
                        return 1;
                    }
                } else {
                    throw new RuntimeException("H2 driver not found. Install H2 to Maven repo or provide -j.");
                }
            }
        }

        Main.init(null, url, user, password, driverClass, jar, "default", false, 30000, null);

        // Create sample schema and data
        String[] setup = {
            "CREATE TABLE IF NOT EXISTS t_demo (id INT PRIMARY KEY, name VARCHAR(100), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            "INSERT INTO t_demo VALUES (1, 'Alice', NOW())",
            "INSERT INTO t_demo VALUES (2, 'Bob', NOW())",
            "INSERT INTO t_demo VALUES (3, 'Charlie', NOW())"
        };
        for (String sql : setup) {
            org.xht.rd.RD.modify().sql(sql).execute();
        }

        // Save as current connection
        ConnectionStore.save("default", url, user, password, driverClass, jar);

        long elapsed = System.currentTimeMillis() - start;
        ResultWriter.printResult(r -> {
            r.put("success", true);
            r.put("message", "H2 demo database started");
            r.put("url", url);
            r.put("user", user);
            r.put("sampleTable", "t_demo");
            r.put("query", "rd query -s \"SELECT * FROM t_demo\"");
            r.put("timeMs", elapsed);
        }, main.isPretty());
        return 0;
    }
}
