package org.xht.roudan.cli.command;

import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.output.ResultWriter;
import org.xht.rd.RD;
import picocli.CommandLine;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "exec",
        description = "Execute SQL file as a single transaction (auto commit/rollback)"
)
public class ExecCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private Main main;

    @CommandLine.Option(names = {"-f", "--file"}, description = "SQL file path")
    private String file;

    @CommandLine.Option(names = {"-s", "--sql"}, description = "Inline SQL statements (separated by ;)")
    private String sql;

    @CommandLine.Option(names = "--dry-run", description = "Parse SQL without executing")
    private boolean dryRun;

    @Override
    public Integer call() throws Exception {
        long start = System.currentTimeMillis();
        try {
            Main.init(
                    main.getConfigFile(), main.getJdbcUrl(), main.getUser(),
                    main.getPassword(), main.getDriverClass(), main.getDriverJar(),
                    main.getDatasourceName(), main.isShowSql(), main.getConnectTimeout(), main.getSavedName()
            );

            String rawSql = sql != null ? sql : cn.hutool.core.io.FileUtil.readUtf8String(file);
            if (rawSql == null)
                throw new IllegalArgumentException("Either -s/--sql or -f/--file is required");

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

            long execStart = System.currentTimeMillis();
            Connection conn = RD.getConnection();
            boolean originalAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                List<Map<String, Object>> results = new ArrayList<>();
                for (int i = 0; i < stmts.size(); i++) {
                    final int stmtIdx = i;
                    String stmt = stmts.get(i);
                    Map<String, Object> result = executeSingle(stmt);
                    result.put("statementIndex", stmtIdx);
                    result.put("sql", stmt);
                    results.add(result);
                    if (!Boolean.TRUE.equals(result.get("success"))) {
                        conn.rollback();
                        conn.setAutoCommit(originalAutoCommit);
                        long elapsed = System.currentTimeMillis() - start;
                        ResultWriter.printResult(r -> {
                            r.put("success", false);
                            r.put("statementIndex", stmtIdx);
                            r.put("rolledBack", true);
                            r.put("error", result.get("error"));
                            r.put("errorCode", result.get("errorCode"));
                            r.put("partialResults", results);
                            r.put("timeMs", elapsed);
                        }, main.isPretty());
                        return 1;
                    }
                }
                conn.commit();
                conn.setAutoCommit(originalAutoCommit);
                long elapsed = System.currentTimeMillis() - start;
                ResultWriter.printResult(r -> {
                    r.put("success", true);
                    r.put("statementCount", stmts.size());
                    r.put("results", results);
                    r.put("timeMs", elapsed);
                }, main.isPretty());
                return 0;
            } catch (Exception e) {
                try { conn.rollback(); } catch (Exception ignored) {}
                conn.setAutoCommit(originalAutoCommit);
                throw e;
            }
        } catch (Exception e) {
            ResultWriter.printError(e, System.currentTimeMillis() - start);
            return 1;
        }
    }

    private Map<String, Object> executeSingle(String sql) {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            String trimmed = sql.trim().toUpperCase();
            if (trimmed.startsWith("SELECT") || trimmed.startsWith("WITH") || trimmed.startsWith("SHOW") || trimmed.startsWith("DESC")) {
                List<Map<String, Object>> rows = RD.query().sql(sql).executeQuery().result();
                result.put("success", true);
                result.put("type", "query");
                result.put("rowCount", rows.size());
            } else {
                int affected = RD.modify().sql(sql).execute();
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
