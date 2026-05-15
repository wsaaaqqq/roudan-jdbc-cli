package org.xht.roudan.cli.command;

import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.output.ResultWriter;
import org.xht.rd.RD;
import picocli.CommandLine;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "describe",
        description = "Show table structure"
)
public class DescribeCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private Main main;

    @CommandLine.Option(names = {"-t", "--table"}, required = true, description = "Table name")
    private String tableName;

    @Override
    public Integer call() throws Exception {
        long start = System.currentTimeMillis();
        try {
            Main.init(main.getConfigFile(), main.getJdbcUrl(), main.getUser(),
                    main.getPassword(), main.getDriverClass(), main.getDriverJar(),
                    main.getDatasourceName(), main.isShowSql(), main.getConnectTimeout(), main.getSavedName());

            List<Map<String, Object>> columns = new ArrayList<>();
            Set<String> pkColumns = new HashSet<>();

            try (Connection conn = RD.getConnection()) {
                DatabaseMetaData meta = conn.getMetaData();

                try (ResultSet rs = meta.getPrimaryKeys(null, null, tableName)) {
                    while (rs.next()) {
                        pkColumns.add(rs.getString("COLUMN_NAME"));
                    }
                }

                try (ResultSet rs = meta.getColumns(null, null, tableName, null)) {
                    while (rs.next()) {
                        Map<String, Object> col = new LinkedHashMap<>();
                        col.put("name", rs.getString("COLUMN_NAME"));
                        col.put("type", rs.getString("TYPE_NAME"));
                        col.put("size", rs.getInt("COLUMN_SIZE"));
                        col.put("nullable", rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                        col.put("pk", pkColumns.contains(rs.getString("COLUMN_NAME")));
                        int scale = rs.getInt("DECIMAL_DIGITS");
                        if (scale > 0) {
                            col.put("scale", scale);
                        }
                        columns.add(col);
                    }
                }
            }

            long elapsed = System.currentTimeMillis() - start;
            ResultWriter.printResult(r -> {
                r.put("success", true);
                r.put("table", tableName);
                r.put("columns", columns);
                r.put("timeMs", elapsed);
            }, main.isPretty());
            return 0;
        } catch (Exception e) {
            ResultWriter.printError(e, System.currentTimeMillis() - start);
            return 1;
        }
    }
}
