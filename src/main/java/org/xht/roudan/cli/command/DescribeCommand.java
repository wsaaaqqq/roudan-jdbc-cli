package org.xht.roudan.cli.command;

import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.output.ResultWriter;
import org.xht.rd.RD;
import picocli.CommandLine;

import java.sql.*;
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

            Set<String> pkColumns = new HashSet<>();
            long elapsed;

            try (Connection conn = RD.getConnection()) {
                DatabaseMetaData meta = conn.getMetaData();

                try (ResultSet rs = meta.getPrimaryKeys(null, null, tableName)) {
                    while (rs.next()) {
                        pkColumns.add(rs.getString("COLUMN_NAME"));
                    }
                } catch (Exception e) {
                    // Some DBs/drivers don't support getPrimaryKeys
                }

                List<Map<String, Object>> cols;
                try {
                    cols = getColumnsViaMetaData(meta, tableName, pkColumns);
                    if (cols.isEmpty()) {
                        cols = getColumnsViaResultSetMeta(conn, tableName, pkColumns);
                    }
                } catch (Exception e) {
                    cols = getColumnsViaResultSetMeta(conn, tableName, pkColumns);
                }

                elapsed = System.currentTimeMillis() - start;
                List<Map<String, Object>> finalCols = cols;
                ResultWriter.printResult(r -> {
                    r.put("success", true);
                    r.put("table", tableName);
                    r.put("columns", finalCols);
                    r.put("timeMs", elapsed);
                }, main.isPretty());
            }
            return 0;
        } catch (Exception e) {
            ResultWriter.printError(e, System.currentTimeMillis() - start);
            return 1;
        }
    }

    private List<Map<String, Object>> getColumnsViaMetaData(DatabaseMetaData meta, String tableName, Set<String> pkColumns) throws Exception {
        List<Map<String, Object>> columns = doGetColumns(meta, tableName, pkColumns);
        if (columns.isEmpty()) {
            columns = doGetColumns(meta, tableName.toUpperCase(), pkColumns);
        }
        if (columns.isEmpty()) {
            columns = doGetColumns(meta, tableName.toLowerCase(), pkColumns);
        }
        return columns;
    }

    private List<Map<String, Object>> doGetColumns(DatabaseMetaData meta, String tableName, Set<String> pkColumns) throws Exception {
        List<Map<String, Object>> columns = new ArrayList<>();
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
        return columns;
    }

    private List<Map<String, Object>> getColumnsViaResultSetMeta(Connection conn, String tableName, Set<String> pkColumns) throws Exception {
        List<Map<String, Object>> columns = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName + " WHERE 1=0")) {
            ResultSetMetaData rsmd = rs.getMetaData();
            for (int i = 1; i <= rsmd.getColumnCount(); i++) {
                Map<String, Object> col = new LinkedHashMap<>();
                String colName = rsmd.getColumnName(i);
                col.put("name", colName);
                col.put("type", rsmd.getColumnTypeName(i));
                col.put("size", rsmd.getColumnDisplaySize(i));
                col.put("nullable", rsmd.isNullable(i) != ResultSetMetaData.columnNoNulls);
                col.put("pk", pkColumns.contains(colName));
                columns.add(col);
            }
        }
        return columns;
    }

}
