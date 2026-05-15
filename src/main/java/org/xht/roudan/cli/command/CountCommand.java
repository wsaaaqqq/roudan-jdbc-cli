package org.xht.roudan.cli.command;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.output.ResultWriter;
import org.xht.rd.RD;
import org.xht.xdb.util.MapUtil;
import picocli.CommandLine;

import java.sql.Connection;
import java.util.Map;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "count",
        description = "Execute COUNT query"
)
public class CountCommand implements Callable<Integer> {

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

    @Override
    public Integer call() throws Exception {
        long start = System.currentTimeMillis();
        try {
            Main.init(
                    main.getConfigFile(), main.getJdbcUrl(), main.getUser(),
                    main.getPassword(), main.getDriverClass(), main.getDriverJar(),
                    main.getDatasourceName(), main.isShowSql(), main.getConnectTimeout(), main.getSavedName()
            );

            String resolvedSql = sql != null ? sql : cn.hutool.core.io.FileUtil.readUtf8String(sqlFile);
            if (resolvedSql == null)
                throw new IllegalArgumentException("Either -s/--sql or -f/--sql-file is required");

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

            long count;
            if (named) {
                MapUtil<Object> mapArgs = toMapUtil(argsJson);
                count = RD.namedQuery().sql(resolvedSql).args(mapArgs).executeCount();
            } else {
                Object[] parsedArgs = argsJson != null ? JSONUtil.parseArray(argsJson).toArray() : new Object[0];
                count = RD.query().sql(resolvedSql).args(parsedArgs).executeCount();
            }

            long elapsed = System.currentTimeMillis() - start;
            ResultWriter.printResult(r -> {
                r.put("success", true);
                r.put("count", count);
                r.put("timeMs", elapsed);
            }, main.isPretty());
            return 0;
        } catch (Exception e) {
            ResultWriter.printError(e, System.currentTimeMillis() - start);
            return 1;
        }
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
}
