package org.xht.roudan.cli.command;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.output.ResultWriter;
import org.xht.rd.RD;
import picocli.CommandLine;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "import",
        description = "Import data from CSV or JSON file into a table"
)
public class ImportCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private Main main;

    @CommandLine.Option(names = {"-t", "--table"}, required = true, description = "Target table name")
    private String table;

    @CommandLine.Option(names = {"-f", "--file"}, required = true, description = "Input file path (CSV or JSON)")
    private String file;

    @CommandLine.Option(names = "--format", description = "Input format: csv (default) or json")
    private String format;

    @CommandLine.Option(names = "--batch", defaultValue = "100", description = "Batch size for insert (default: 100)")
    private int batchSize;

    @CommandLine.Option(names = "--delimiter", defaultValue = ",", description = "CSV delimiter (default: comma)")
    private String delimiter;

    @CommandLine.Option(names = "--dry-run", description = "Parse input without inserting")
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

            String content = FileUtil.readUtf8String(file);
            boolean isJson = "json".equalsIgnoreCase(format) || content.trim().startsWith("[") || content.trim().startsWith("{");
            List<String> columns;
            List<List<Object>> rows;

            if (isJson) {
                if (content.trim().startsWith("{")) {
                    JSONObject obj = JSONUtil.parseObj(content);
                    JSONArray colsJson = obj.getJSONArray("cols");
                    columns = new ArrayList<>();
                    for (int i = 0; i < colsJson.size(); i++) {
                        columns.add(colsJson.getStr(i));
                    }
                    JSONArray rowsJson = obj.getJSONArray("rows");
                    rows = new ArrayList<>();
                    for (int i = 0; i < rowsJson.size(); i++) {
                        JSONArray row = rowsJson.getJSONArray(i);
                        List<Object> rowList = new ArrayList<>();
                        for (int j = 0; j < row.size(); j++) {
                            rowList.add(row.get(j));
                        }
                        rows.add(rowList);
                    }
                } else {
                    JSONArray arr = JSONUtil.parseArray(content);
                    if (arr.isEmpty()) {
                        columns = new ArrayList<>();
                        rows = new ArrayList<>();
                    } else {
                        JSONObject first = arr.getJSONObject(0);
                        columns = new ArrayList<>(first.keySet());
                        rows = new ArrayList<>();
                        for (int i = 0; i < arr.size(); i++) {
                            JSONObject obj = arr.getJSONObject(i);
                            List<Object> row = new ArrayList<>();
                            for (String col : columns) {
                                row.add(obj.get(col));
                            }
                            rows.add(row);
                        }
                    }
                }
            } else {
                String delim = delimiter.equals("\\t") ? "\t" : delimiter;
                String[] lines = content.split("\\r?\\n");
                if (lines.length == 0) {
                    columns = new ArrayList<>();
                    rows = new ArrayList<>();
                } else {
                    columns = splitCsvLine(lines[0], delim);
                    rows = new ArrayList<>();
                    for (int i = 1; i < lines.length; i++) {
                        String line = lines[i].trim();
                        if (!line.isEmpty()) {
                            rows.add(parseCsvValues(line, delim));
                        }
                    }
                }
            }

            if (dryRun) {
                ResultWriter.printResult(r -> {
                    r.put("success", true);
                    r.put("dryRun", true);
                    r.put("table", table);
                    r.put("columns", columns);
                    r.put("rowCount", rows.size());
                    r.put("sampleRows", rows.size() > 5 ? rows.subList(0, 5) : rows);
                    r.put("timeMs", System.currentTimeMillis() - start);
                }, main.isPretty());
                return 0;
            }

            String insertSql = buildInsertSql(table, columns);
            Connection conn = RD.getConnection();
            boolean origAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);

            try {
                int totalInserted = 0;
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    for (int i = 0; i < rows.size(); i++) {
                        List<Object> row = rows.get(i);
                        for (int j = 0; j < Math.min(columns.size(), row.size()); j++) {
                            Object val = row.get(j);
                            if (val == null || val instanceof cn.hutool.json.JSONNull) {
                                ps.setNull(j + 1, java.sql.Types.VARCHAR);
                            } else if (val instanceof Number) {
                                ps.setObject(j + 1, val);
                            } else {
                                ps.setString(j + 1, val.toString());
                            }
                        }
                        ps.addBatch();
                        totalInserted++;

                        if (totalInserted % batchSize == 0) {
                            ps.executeBatch();
                        }
                    }
                    ps.executeBatch();
                }
                conn.commit();
                conn.setAutoCommit(origAutoCommit);

                long elapsed = System.currentTimeMillis() - start;
                final int finalInsertedRows = totalInserted;
                ResultWriter.printResult(r -> {
                    r.put("success", true);
                    r.put("table", table);
                    r.put("insertedRows", finalInsertedRows);
                    r.put("timeMs", elapsed);
                }, main.isPretty());
                return 0;
            } catch (Exception e) {
                try { conn.rollback(); } catch (Exception ignored) {}
                conn.setAutoCommit(origAutoCommit);
                throw e;
            }
        } catch (Exception e) {
            ResultWriter.printError(e, System.currentTimeMillis() - start);
            return 1;
        }
    }

    private String buildInsertSql(String table, List<String> columns) {
        StringBuilder sql = new StringBuilder("INSERT INTO ");
        sql.append(table).append(" (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(columns.get(i));
        }
        sql.append(") VALUES (");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
        }
        sql.append(")");
        return sql.toString();
    }

    private List<String> splitCsvLine(String line, String delim) {
        List<String> parts = new ArrayList<>();
        boolean inQuote = false;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
            } else if (!inQuote && line.startsWith(delim, i)) {
                parts.add(current.toString().trim());
                current = new StringBuilder();
                i += delim.length() - 1;
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString().trim());
        return parts;
    }

    private List<Object> parseCsvValues(String line, String delim) {
        List<Object> values = new ArrayList<>();
        for (String part : splitCsvLine(line, delim)) {
            String val = part;
            if (val.startsWith("\"") && val.endsWith("\"")) {
                val = val.substring(1, val.length() - 1);
            }
            if (val.isEmpty()) {
                values.add(null);
            } else {
                values.add(val);
            }
        }
        return values;
    }
}
