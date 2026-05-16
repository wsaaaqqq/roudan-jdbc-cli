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

        String home = System.getProperty("user.home");
        new java.io.File(home, ".roudan-cli").mkdirs();
        String dbPath = home + "/.roudan-cli/demo";
        String url = "jdbc:h2:file:" + dbPath + ";AUTO_SERVER=TRUE";
        String user = "sa";
        String password = "";
        String driverClass = "org.h2.Driver";
        String jar = null;

        // Use H2 from Maven cache, classpath, or download on demand
        String localJar = home + "/.m2/repository/com/h2database/h2/2.2.224/h2-2.2.224.jar";
        if (new java.io.File(localJar).exists()) {
            jar = localJar;
        } else {
            try {
                Class.forName("org.h2.Driver");
            } catch (ClassNotFoundException e1) {
                org.xht.roudan.cli.driver.DriverRegistry.DriverInfo info =
                        org.xht.roudan.cli.driver.DriverRegistry.resolve(url);
                if (info != null) {
                    try { jar = org.xht.roudan.cli.driver.DriverDownloader.download(info); }
                    catch (Exception ex) {
                        ResultWriter.printResult(r -> { r.put("success",false); r.put("error","Download H2 driver: " + ex.getMessage()); r.put("errorCode","DOWNLOAD_ERROR"); r.put("timeMs", System.currentTimeMillis() - start); }, true);
                        return 1;
                    }
                } else throw new RuntimeException("H2 driver not found. Try providing -j.");
            }
        }

        Main.init(null, url, user, password, driverClass, jar, "default", false, 30000, null);

        String[] setup = {
            "CREATE TABLE IF NOT EXISTS t_demo (id INT PRIMARY KEY, name VARCHAR(100), created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)",
            "INSERT INTO t_demo VALUES (1, 'Alice', NOW())",
            "INSERT INTO t_demo VALUES (2, 'Bob', NOW())",
            "INSERT INTO t_demo VALUES (3, 'Charlie', NOW())"
        };
        for (String sql : setup) { org.xht.rd.RD.modify().sql(sql).execute(); }

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
