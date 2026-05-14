package org.xht.roudan.cli.command;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.output.ResultWriter;
import org.xht.rd.RD;
import org.xht.xdb.sql.ResultQuery;
import picocli.CommandLine;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "query",
        description = "Execute SELECT query"
)
public class QueryCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private Main main;

    @CommandLine.Option(names = {"-s", "--sql"}, description = "SQL statement")
    private String sql;

    @CommandLine.Option(names = {"-f", "--sql-file"}, description = "SQL file path")
    private String sqlFile;

    @CommandLine.Option(names = "--named", description = "Use named parameters (:paramName)")
    private boolean named;

    @CommandLine.Option(names = {"-a", "--args"}, description = "Query args as JSON array or object")
    private String argsJson;

    @CommandLine.Option(names = "--limit", description = "Max rows to return")
    private Integer limit;

    @CommandLine.Option(names = "--page", description = "Page number (1-based, requires --size)")
    private Integer page;

    @CommandLine.Option(names = "--size", description = "Page size (requires --page)")
    private Integer size;

    @Override
    public Integer call() throws Exception {
        long start = System.currentTimeMillis();
        try {
            Main.init(main.getConfigFile(), main.getJdbcUrl(), main.getUser(),
                    main.getPassword(), main.getDriverClass(), main.getDriverJar(),
                    main.getDatasourceName(), main.isShowSql());

            String resolvedSql = resolveSql();
            List<Map<String, Object>> rows = executeQuery(resolvedSql);
            List<Map<String, Object>> limited = applyLimit(rows);
            buildAndPrintResult(limited, rows.size(), start);

            return 0;
        } catch (Exception e) {
            ResultWriter.printError(e, System.currentTimeMillis() - start);
            return 1;
        }
    }

    private String resolveSql() throws Exception {
        if (sql != null) return sql;
        if (sqlFile != null) return cn.hutool.core.io.FileUtil.readUtf8String(sqlFile);
        throw new IllegalArgumentException("Either -s/--sql or -f/--sql-file is required");
    }

    private List<Map<String, Object>> executeQuery(String resolvedSql) {
        if (named) {
            JSONObject jsonArgs = argsJson != null ? JSONUtil.parseObj(argsJson) : new JSONObject();
            return RD.namedQuery().sql(resolvedSql).args(toMapUtil(jsonArgs)).executeQuery().result();
        } else {
            Object[] parsedArgs = argsJson != null ? parseJsonArray(argsJson) : new Object[0];
            return RD.query().sql(resolvedSql).args(parsedArgs).executeQuery().result();
        }
    }

    private Object[] parseJsonArray(String json) {
        JSONArray arr = JSONUtil.parseArray(json);
        return arr.toArray();
    }

    private cn.hutool.core.map.MapUtil<Object> toMapUtil(JSONObject obj) {
        cn.hutool.core.map.MapUtil<Object> mapUtil = cn.hutool.core.map.MapUtil.create();
        for (Map.Entry<String, Object> entry : obj) {
            mapUtil.put(entry.getKey(), entry.getValue());
        }
        return (cn.hutool.core.map.MapUtil<Object>) mapUtil;
    }

    private List<Map<String, Object>> applyLimit(List<Map<String, Object>> rows) {
        if (page != null && size != null) {
            int from = (page - 1) * size;
            int to = Math.min(from + size, rows.size());
            if (from >= rows.size()) return java.util.Collections.emptyList();
            return rows.subList(from, to);
        }
        if (limit != null && limit < rows.size()) {
            return rows.subList(0, limit);
        }
        return rows;
    }

    private void buildAndPrintResult(List<Map<String, Object>> rows, int totalCount, long start) {
        long elapsed = System.currentTimeMillis() - start;
        if (rows.isEmpty()) {
            ResultWriter.printResult(r -> {
                r.put("success", true);
                r.put("rowCount", 0);
                r.put("cols", java.util.Collections.emptyList());
                r.put("rows", java.util.Collections.emptyList());
                r.put("timeMs", elapsed);
            }, main.isPretty());
            return;
        }

        Map<String, Object> first = rows.get(0);
        String[] cols = first.keySet().toArray(new String[0]);

        java.util.List<Object[]> rowData = new java.util.ArrayList<>();
        for (Map<String, Object> row : rows) {
            Object[] values = new Object[cols.length];
            for (int i = 0; i < cols.length; i++) {
                values[i] = row.get(cols[i]);
            }
            rowData.add(values);
        }

        java.util.List<String> colList = java.util.Arrays.asList(cols);
        ResultWriter.printResult(r -> {
            r.put("success", true);
            r.put("rowCount", totalCount);
            r.put("cols", colList);
            r.put("rows", rowData);
            r.put("timeMs", elapsed);
        }, main.isPretty());
    }
}
