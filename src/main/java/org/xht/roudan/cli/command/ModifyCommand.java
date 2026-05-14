package org.xht.roudan.cli.command;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.output.ResultWriter;
import org.xht.rd.RD;
import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "modify",
        description = "Execute INSERT/UPDATE/DELETE/DDL"
)
public class ModifyCommand implements Callable<Integer> {

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
            Main.init(main.getConfigFile(), main.getJdbcUrl(), main.getUser(),
                    main.getPassword(), main.getDriverClass(), main.getDriverJar(),
                    main.getDatasourceName(), main.isShowSql());

            String resolvedSql = sql != null ? sql : cn.hutool.core.io.FileUtil.readUtf8String(sqlFile);
            if (resolvedSql == null) throw new IllegalArgumentException("Either -s/--sql or -f/--sql-file is required");

            int affectedRows;
            if (named) {
                JSONObject jsonArgs = argsJson != null ? JSONUtil.parseObj(argsJson) : new JSONObject();
                affectedRows = RD.namedModify().sql(resolvedSql).args(toMapUtil(jsonArgs)).execute();
            } else {
                Object[] parsedArgs = argsJson != null ? JSONUtil.parseArray(argsJson).toArray() : new Object[0];
                affectedRows = RD.modify().sql(resolvedSql).args(parsedArgs).execute();
            }

            long elapsed = System.currentTimeMillis() - start;
            ResultWriter.printResult(r -> {
                r.put("success", true);
                r.put("affectedRows", affectedRows);
                r.put("timeMs", elapsed);
            }, main.isPretty());
            return 0;
        } catch (Exception e) {
            ResultWriter.printError(e, System.currentTimeMillis() - start);
            return 1;
        }
    }

    private cn.hutool.core.map.MapUtil<Object> toMapUtil(JSONObject obj) {
        cn.hutool.core.map.MapUtil<Object> mapUtil = cn.hutool.core.map.MapUtil.create();
        for (java.util.Map.Entry<String, Object> entry : obj) {
            mapUtil.put(entry.getKey(), entry.getValue());
        }
        return (cn.hutool.core.map.MapUtil<Object>) mapUtil;
    }
}
