package org.xht.roudan.cli.command;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.output.ResultWriter;
import org.xht.rd.RD;
import picocli.CommandLine;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "export",
        description = "Export query results to CSV or JSON file"
)
public class ExportCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private Main main;

    @CommandLine.Option(names = {"-s", "--sql"}, required = true, description = "SELECT SQL")
    private String sql;

    @CommandLine.Option(names = {"-o", "--output"}, description = "Output file path (default: stdout)")
    private String outputFile;

    @CommandLine.Option(names = "--format", description = "Output format: csv (default) or json")
    private String format;

    @CommandLine.Option(names = "--limit", description = "Max rows")
    private Integer limit;

    @Override
    public Integer call() throws Exception {
        long start = System.currentTimeMillis();
        try {
            Main.init(
                    main.getConfigFile(), main.getJdbcUrl(), main.getUser(),
                    main.getPassword(), main.getDriverClass(), main.getDriverJar(),
                    main.getDatasourceName(), main.isShowSql(), main.getConnectTimeout(), main.getSavedName()
            );

            List<Map<String, Object>> rows = RD.query().sql(sql).executeQuery().result();
            if (limit != null && limit < rows.size()) {
                rows = rows.subList(0, limit);
            }

            List<String> cols = rows.isEmpty() ? new ArrayList<String>() : new ArrayList<>(rows.get(0).keySet());

            boolean isJson = "json".equalsIgnoreCase(format);
            StringBuilder content = new StringBuilder();

            if (isJson) {
                JSONObject json = new JSONObject();
                JSONArray colsArr = new JSONArray();
                for (String c : cols) colsArr.add(c);
                JSONArray rowsArr = new JSONArray();
                for (Map<String, Object> row : rows) {
                    JSONArray rowArr = new JSONArray();
                    for (String c : cols) {
                        rowArr.add(row.get(c));
                    }
                    rowsArr.add(rowArr);
                }
                json.set("cols", colsArr);
                json.set("rows", rowsArr);
                json.set("rowCount", rows.size());
                content.append(json.toStringPretty());
            } else {
                for (int i = 0; i < cols.size(); i++) {
                    if (i > 0) content.append(',');
                    content.append(escapeCsv(cols.get(i)));
                }
                content.append('\n');
                for (Map<String, Object> row : rows) {
                    for (int i = 0; i < cols.size(); i++) {
                        if (i > 0) content.append(',');
                        Object val = row.get(cols.get(i));
                        content.append(escapeCsv(val != null ? val.toString() : ""));
                    }
                    content.append('\n');
                }
            }

            final int finalRowCount = rows.size();
            if (outputFile != null) {
                FileUtil.writeUtf8String(content.toString(), outputFile);
                long elapsed = System.currentTimeMillis() - start;
                final boolean finalIsJson = isJson;
                ResultWriter.printResult(r -> {
                    r.put("success", true);
                    r.put("rowCount", finalRowCount);
                    r.put("file", outputFile);
                    r.put("timeMs", elapsed);
                }, main.isPretty());
            } else {
                System.out.print(content.toString());
                long elapsed = System.currentTimeMillis() - start;
                final boolean finalIsJson = isJson;
                ResultWriter.printResult(r -> {
                    r.put("success", true);
                    r.put("rowCount", finalRowCount);
                    r.put("format", finalIsJson ? "json" : "csv");
                    r.put("timeMs", elapsed);
                }, main.isPretty());
            }
            return 0;
        } catch (Exception e) {
            ResultWriter.printError(e, System.currentTimeMillis() - start);
            return 1;
        }
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
