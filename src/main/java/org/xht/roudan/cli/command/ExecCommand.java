package org.xht.roudan.cli.command;

import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.output.ResultWriter;
import org.xht.rd.RD;
import org.xht.rd.RDConfig;
import picocli.CommandLine;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "exec",
        description = "Execute SQL statements in a single connection"
)
public class ExecCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private Main main;

    @CommandLine.Option(names = {"-f", "--file"}, description = "SQL file path")
    private String file;

    @CommandLine.Option(names = {"-s", "--sql"}, description = "Inline SQL statements (separated by ;)")
    private String sql;

    @CommandLine.Option(names = "--mode", defaultValue = "transaction", description = "Execution mode: transaction (default, all-or-nothing) or auto (each stmt auto-committed)")
    private String mode;

    @CommandLine.Option(names = "--dry-run", description = "Parse SQL without executing")
    private boolean dryRun;

    @Override
    public Integer call() throws Exception {
        long start = System.currentTimeMillis();
        try {
            String rawSql = SqlInput.read(sql, file);
            Main.init(
                    main.getConfigFile(), main.getJdbcUrl(), main.getUser(),
                    main.getPassword(), main.getDriverClass(), main.getDriverJar(),
                    main.getDatasourceName(), main.isShowSql(), main.getConnectTimeout(), main.getSavedName()
            );

            List<String> stmts = splitStatements(rawSql);

            if (dryRun) {
                ResultWriter.printResult(r -> {
                    r.put("success", true);
                    r.put("dryRun", true);
                    r.put("statementCount", stmts.size());
                    r.put("statements", stmts);
                    r.put("timeMs", System.currentTimeMillis() - start);
                }, main.isPretty());
                return 0;
            }

            boolean isTransaction = "transaction".equalsIgnoreCase(mode);
            Map<String, Object> report;
            try (Connection conn = RD.getConnection()) {
                conn.setAutoCommit(!isTransaction);
                try {
                    report = executeStatements(conn, stmts, isTransaction, start);
                } catch (Exception e) {
                    if (isTransaction) {
                        try {
                            conn.rollback();
                        } catch (Exception rollbackError) {
                            e.addSuppressed(rollbackError);
                        }
                    }
                    throw e;
                }
            }
            ResultWriter.printResult(r -> r.putAll(report), main.isPretty());
            return Boolean.TRUE.equals(report.get("success")) ? 0 : 1;
        } catch (Exception e) {
            ResultWriter.printError(e, System.currentTimeMillis() - start);
            return 1;
        }
    }

    private Map<String, Object> executeStatements(Connection conn, List<String> stmts,
                                                  boolean isTransaction, long start) throws Exception {
        List<Map<String, Object>> results = new ArrayList<>();
        boolean allSucceeded = true;
        int failedIndex = -1;
        String failedError = null;

        for (int i = 0; i < stmts.size(); i++) {
            String stmt = stmts.get(i);
            Map<String, Object> result = executeSingle(conn, stmt);
            result.put("statementIndex", i);
            result.put("sql", stmt);
            results.add(result);

            if (!Boolean.TRUE.equals(result.get("success")) && allSucceeded) {
                allSucceeded = false;
                failedIndex = i;
                failedError = (String) result.get("error");
                if (isTransaction) {
                    break;
                }
            }
        }

        if (isTransaction) {
            if (allSucceeded) {
                conn.commit();
            } else {
                conn.rollback();
            }
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("success", allSucceeded);
        if (!allSucceeded && isTransaction) {
            report.put("statementIndex", failedIndex);
            report.put("rolledBack", true);
            report.put("error", failedError);
            report.put("errorCode", "SQL_ERROR");
            report.put("partialResults", results);
        } else {
            report.put("mode", mode);
            report.put("statementCount", stmts.size());
            report.put("results", results);
        }
        report.put("timeMs", System.currentTimeMillis() - start);
        return report;
    }

    private Map<String, Object> executeSingle(Connection conn, String sql) {
        Map<String, Object> result = new LinkedHashMap<>();
        try (Statement statement = conn.createStatement()) {
            if (RDConfig.isShowSql()) {
                System.err.println(sql);
            }
            if (statement.execute(sql)) {
                int rowCount = 0;
                try (ResultSet rows = statement.getResultSet()) {
                    while (rows.next()) rowCount++;
                }
                result.put("success", true);
                result.put("type", "query");
                result.put("rowCount", rowCount);
            } else {
                int affected = statement.getUpdateCount();
                result.put("success", true);
                result.put("type", "update");
                result.put("affectedRows", affected);
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("type", "error");
            result.put("error", e.getMessage());
            result.put("errorCode", "SQL_ERROR");
        }
        return result;
    }

    static List<String> splitStatements(String raw) {
        List<String> stmts = new ArrayList<>();
        if (raw == null) return stmts;
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            char n = i + 1 < raw.length() ? raw.charAt(i + 1) : 0;

            if (inLineComment) {
                if (c == '\n') inLineComment = false;
                continue;
            }
            if (inBlockComment) {
                if (c == '*' && n == '/') { inBlockComment = false; i++; }
                continue;
            }
            if (inSingleQuote) {
                current.append(c);
                if (c == '\'') inSingleQuote = false;
                continue;
            }
            if (inDoubleQuote) {
                current.append(c);
                if (c == '"') inDoubleQuote = false;
                continue;
            }

            if (c == '-' && n == '-') { inLineComment = true; i++; continue; }
            if (c == '/' && n == '*') { inBlockComment = true; i++; continue; }
            if (c == '\'') { inSingleQuote = true; current.append(c); continue; }
            if (c == '"') { inDoubleQuote = true; current.append(c); continue; }

            if (c == ';') {
                String stmt = current.toString().trim();
                if (!stmt.isEmpty()) stmts.add(stmt);
                current = new StringBuilder();
                continue;
            }
            current.append(c);
        }
        String remaining = current.toString().trim();
        if (!remaining.isEmpty()) stmts.add(remaining);
        return stmts;
    }
}
