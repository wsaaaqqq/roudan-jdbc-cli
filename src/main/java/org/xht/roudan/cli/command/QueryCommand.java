package org.xht.roudan.cli.command;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.output.ResultWriter;
import org.xht.rd.RD;
import org.xht.xdb.util.MapUtil;
import picocli.CommandLine;

import java.util.Collections;
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
            Main.init(
                    main.getConfigFile(), main.getJdbcUrl(), main.getUser(),
                    main.getPassword(), main.getDriverClass(), main.getDriverJar(),
                    main.getDatasourceName(), main.isShowSql()
            );

            String resolvedSql = sql != null ? sql : cn.hutool.core.io.FileUtil.readUtf8String(sqlFile);
            if (resolvedSql == null)
                throw new IllegalArgumentException("Either -s/--sql or -f/--sql-file is required");

            List<Map<String, Object>> rows;
            if (named) {
                MapUtil<Object> mapArgs = toMapUtil(argsJson);
                rows = RD.namedQuery().sql(resolvedSql).args(mapArgs).executeQuery().result();
            } else {
                Object[] parsedArgs = parseJsonArray(argsJson);
                rows = RD.query().sql(resolvedSql).args(parsedArgs).executeQuery().result();
            }

            List<Map<String, Object>> limited = applyLimit(rows);
            buildAndPrintResult(limited, rows.size(), start);
            return 0;
        } catch (Exception e) {
            ResultWriter.printError(e, System.currentTimeMillis() - start);
            return 1;
        }
    }

    private Object[] parseJsonArray(String json) {
        if (json == null) return new Object[0];
        JSONArray arr = JSONUtil.parseArray(json);
        return arr.toArray();
    }

    private MapUtil<Object> toMapUtil(String json) {
        MapUtil<Object> mapUtil = MapUtil.init();
        if (json != null) {
            JSONObject obj = JSONUtil.parseObj(json);
            for (Map.Entry<String, Object> entry : obj) {
                mapUtil.add(entry.getKey(), entry.getValue());
            }
        }
        return mapUtil;
    }

    private List<Map<String, Object>> applyLimit(List<Map<String, Object>> rows) {
        if (page != null && size != null) {
            int from = (page - 1) * size;
            int to = Math.min(from + size, rows.size());
            if (from >= rows.size()) return Collections.emptyList();
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
            ResultWriter.printQueryResult(
                    java.util.Collections.emptyList(), java.util.Collections.emptyList(),
                    0, elapsed, main.getOutputFormat(), main.isPretty(), main.isNoHeader()
            );
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
        ResultWriter.printQueryResult(
                java.util.Arrays.asList(cols), rowData, totalCount, elapsed,
                main.getOutputFormat(), main.isPretty(), main.isNoHeader()
        );
    }
}
