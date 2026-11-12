package org.xht.roudan.cli.command;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.output.ResultWriter;
import org.xht.rd.RD;
import org.xht.xdb.sql.ResultQuery;
import org.xht.xdb.util.BlobUtil;
import org.xht.xdb.util.ClobUtil;
import org.xht.xdb.util.MapUtil;
import picocli.CommandLine;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
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

    @CommandLine.Option(names = {"-o", "--output"}, description = "Output format: json, json-pretty, csv, table")
    private String outputFormat;

    @Override
    public Integer call() throws Exception {
        long start = System.currentTimeMillis();
        try {
            String resolvedSql = SqlInput.read(sql, sqlFile);
            Main.init(
                    main.getConfigFile(), main.getJdbcUrl(), main.getUser(),
                    main.getPassword(), main.getDriverClass(), main.getDriverJar(),
                    main.getDatasourceName(), main.isShowSql(), main.getConnectTimeout(), main.getSavedName()
            );

            if (main.isDryRun()) {
                ResultWriter.printResult(r -> {
                    r.put("success", true);
                    r.put("dryRun", true);
                    r.put("sql", resolvedSql);
                    r.put("timeMs", 0);
                }, main.isPretty());
                return 0;
            }

            Connection txConn = Main.getTxConnection();
            if (txConn != null) {
                RD.setConnection(txConn);
            }

            ResultQuery result;
            if (named) {
                MapUtil<Object> mapArgs = toMapUtil(argsJson);
                result = RD.namedQuery().sql(resolvedSql).args(mapArgs).executeQuery();
            } else {
                Object[] parsedArgs = parseJsonArray(argsJson);
                result = RD.query().sql(resolvedSql).args(parsedArgs).executeQuery();
            }

            List<String> cols = new ArrayList<>();
            ResultSetMetaData metadata = result.resultSet().getMetaData();
            for (int i = 1; i <= metadata.getColumnCount(); i++) {
                String label = metadata.getColumnLabel(i);
                cols.add(label == null || label.isEmpty() ? metadata.getColumnName(i) : label);
            }
            List<Object[]> rows = result.resultTo((rs, count, meta) -> {
                Object[] values = new Object[cols.size()];
                for (int i = 0; i < values.length; i++) {
                    Object value = rs.getObject(i + 1);
                    if (value instanceof Clob) {
                        value = ClobUtil.toString((Clob) value);
                    } else if (value instanceof Blob) {
                        value = BlobUtil.toBinaryString((Blob) value);
                    }
                    values[i] = value;
                }
                return values;
            });
            String format = outputFormat != null ? outputFormat : main.getOutputFormat();
            ResultWriter.printQueryResult(cols, applyLimit(rows), rows.size(), System.currentTimeMillis() - start,
                    format, main.isPretty(), main.isNoHeader());
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

    private List<Object[]> applyLimit(List<Object[]> rows) {
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
}
