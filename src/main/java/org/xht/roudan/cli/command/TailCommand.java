package org.xht.roudan.cli.command;

import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.output.ResultWriter;
import org.xht.rd.RD;
import picocli.CommandLine;

import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "tail",
        description = "Poll a table for changes at regular intervals"
)
public class TailCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private Main main;

    @CommandLine.Option(names = {"-t", "--table"}, required = true, description = "Table to monitor")
    private String table;

    @CommandLine.Option(names = "--pk", description = "Primary key column to track latest record")
    private String pkColumn;

    @CommandLine.Option(names = "--interval", defaultValue = "5", description = "Polling interval in seconds (default: 5)")
    private int interval;

    @CommandLine.Option(names = "--count", description = "Maximum polling rounds (default: unlimited)")
    private Integer maxCount;

    @CommandLine.Option(names = {"-s", "--sql"}, description = "Custom SELECT SQL (overrides table monitoring)")
    private String customSql;

    @Override
    public Integer call() throws Exception {
        long start = System.currentTimeMillis();
        try {
            Main.init(
                    main.getConfigFile(), main.getJdbcUrl(), main.getUser(),
                    main.getPassword(), main.getDriverClass(), main.getDriverJar(),
                    main.getDatasourceName(), main.isShowSql(), main.getConnectTimeout(), main.getSavedName()
            );

            if (interval < 1) interval = 1;

            Connection conn = RD.getConnection();
            String sql = customSql != null ? customSql : "SELECT COUNT(*) AS cnt FROM " + table;
            String pkSql = pkColumn != null
                    ? "SELECT MAX(" + pkColumn + ") AS max_pk FROM " + table
                    : null;

            long prevCount = -1;
            Object prevMaxPk = null;
            int rounds = 0;

            long firstPollStart = System.currentTimeMillis();
            Map<String, Object> firstResult = poll(conn, sql, pkSql, prevMaxPk);
            if (firstResult != null) {
                prevCount = ((Number) firstResult.get("count")).longValue();
                prevMaxPk = firstResult.get("maxPk");
            }
            long firstPollEnd = System.currentTimeMillis();
            long firstPollMs = firstPollEnd - firstPollStart;

            if (maxCount != null) rounds++;

            StringBuilder report = new StringBuilder();
            report.append("[").append(isoNow()).append("] poll #1: ").append(prevCount).append(" rows");
            if (pkColumn != null) report.append(", max ").append(pkColumn).append("=").append(prevMaxPk);

            while (maxCount == null || rounds < maxCount) {
                Thread.sleep(interval * 1000L);
                rounds++;

                Map<String, Object> result = poll(conn, sql, pkSql, prevMaxPk);
                if (result == null) continue;

                long newCount = ((Number) result.get("count")).longValue();
                Object newMaxPk = result.get("maxPk");

                boolean changed = newCount != prevCount;
                if (pkColumn != null && newMaxPk != null) {
                    if (prevMaxPk == null || !newMaxPk.toString().equals(prevMaxPk.toString())) {
                        changed = true;
                    }
                }

                if (changed) {
                    report.append("\n[").append(isoNow()).append("] poll #").append(rounds + 1)
                            .append(": ").append(newCount).append(" rows");
                    if (pkColumn != null) report.append(", max ").append(pkColumn).append("=").append(newMaxPk);

                    if (pkColumn != null && prevMaxPk != null && newMaxPk != null
                            && !newMaxPk.toString().equals(prevMaxPk.toString())) {
                        String newSql = customSql != null
                                ? customSql
                                : "SELECT * FROM " + table + " WHERE " + pkColumn + " > " + prevMaxPk;
                        List<Map<String, Object>> newRows = RD.query().sql(newSql).executeQuery().result();
                        report.append(" (+").append(newRows.size()).append(" new records)");
                    }

                    prevCount = newCount;
                    prevMaxPk = newMaxPk;
                }
            }

            long elapsed = System.currentTimeMillis() - start;
            final int finalRounds = rounds;
            final long finalCount = prevCount;
            String finalLog = report.toString();
            ResultWriter.printResult(r -> {
                r.put("success", true);
                r.put("table", table);
                r.put("intervalSec", interval);
                r.put("rounds", finalRounds);
                r.put("initialCount", finalCount);
                r.put("log", finalLog);
                r.put("timeMs", elapsed);
            }, main.isPretty());
            return 0;
        } catch (Exception e) {
            ResultWriter.printError(e, System.currentTimeMillis() - start);
            return 1;
        }
    }

    private Map<String, Object> poll(Connection conn, String sql, String pkSql, Object prevMaxPk) {
        try {
            List<Map<String, Object>> rows = RD.query().sql(sql).executeQuery().result();
            long count = rows.isEmpty() ? 0 : ((Number) rows.get(0).get("cnt")).longValue();
            Object maxPk = null;

            if (pkSql != null) {
                List<Map<String, Object>> pkRows = RD.query().sql(pkSql).executeQuery().result();
                if (!pkRows.isEmpty()) {
                    maxPk = pkRows.get(0).get("max_pk");
                }
            }

            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("count", count);
            result.put("maxPk", maxPk);
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    private String isoNow() {
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
    }
}
