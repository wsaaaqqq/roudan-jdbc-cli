package org.xht.roudan.cli.command;

import org.xht.roudan.cli.Main;
import org.xht.roudan.cli.output.ResultWriter;
import org.xht.rd.RD;
import picocli.CommandLine;

import java.sql.*;
import java.util.*;
import java.util.concurrent.Callable;

@CommandLine.Command(
        name = "gen",
        description = "Generate DDL or INSERT statements from a table"
)
public class GenCommand implements Callable<Integer> {

    @CommandLine.ParentCommand
    private Main main;

    @CommandLine.Option(names = {"-t", "--table"}, required = true, description = "Table name")
    private String table;

    @CommandLine.Option(names = "--ddl", description = "Generate CREATE TABLE DDL")
    private boolean ddl;

    @CommandLine.Option(names = "--insert", description = "Generate INSERT statements from existing data")
    private boolean insert;

    @CommandLine.Option(names = "--sample", description = "Max rows for INSERT sample (default: all)")
    private Integer sample;

    @Override
    public Integer call() throws Exception {
        long start = System.currentTimeMillis();
        try {
            Main.init(
                    main.getConfigFile(), main.getJdbcUrl(), main.getUser(),
                    main.getPassword(), main.getDriverClass(), main.getDriverJar(),
                    main.getDatasourceName(), main.isShowSql(), main.getConnectTimeout(), main.getSavedName()
            );

            if (!ddl && !insert) {
                ddl = true;
            }

            StringBuilder output = new StringBuilder();

            try (Connection conn = RD.getConnection()) {
                DatabaseMetaData meta = conn.getMetaData();

                List<ColumnInfo> columns = new ArrayList<>();
                Set<String> pkColumns = new HashSet<>();

                try (ResultSet rs = meta.getPrimaryKeys(null, null, table)) {
                    while (rs.next()) {
                        pkColumns.add(rs.getString("COLUMN_NAME"));
                    }
                } catch (Exception ignored) {}

                try (ResultSet rs = meta.getColumns(null, null, table, null)) {
                    while (rs.next()) {
                        ColumnInfo col = new ColumnInfo();
                        col.name = rs.getString("COLUMN_NAME");
                        col.type = rs.getString("TYPE_NAME");
                        col.size = rs.getInt("COLUMN_SIZE");
                        col.scale = rs.getInt("DECIMAL_DIGITS");
                        col.nullable = rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
                        col.pk = pkColumns.contains(col.name);
                        col.autoIncrement = "YES".equalsIgnoreCase(rs.getString("IS_AUTOINCREMENT"));
                        columns.add(col);
                    }
                }

                if (ddl) {
                    output.append("CREATE TABLE ").append(table).append(" (\n");
                    for (int i = 0; i < columns.size(); i++) {
                        ColumnInfo col = columns.get(i);
                        output.append("  ").append(col.name).append(" ").append(col.type);
                        if (col.size > 0 && !isFixedLengthType(col.type)) {
                            output.append("(").append(col.size);
                            if (col.scale > 0) output.append(",").append(col.scale);
                            output.append(")");
                        }
                        if (!col.nullable) output.append(" NOT NULL");
                        if (col.pk) output.append(" PRIMARY KEY");
                        if (col.autoIncrement) output.append(" AUTO_INCREMENT");
                        if (i < columns.size() - 1) output.append(",");
                        output.append("\n");
                    }
                    output.append(")");
                }

                if (insert) {
                    String sql = "SELECT * FROM " + table;
                    if (sample != null) {
                        try {
                            String db = conn.getMetaData().getDatabaseProductName().toLowerCase();
                            if (db.contains("mysql") || db.contains("postgresql") || db.contains("h2")) {
                                sql += " LIMIT " + sample;
                            } else if (db.contains("oracle")) {
                                sql = "SELECT * FROM " + table + " WHERE ROWNUM <= " + sample;
                            } else if (db.contains("microsoft") || db.contains("sql server")) {
                                sql = "SELECT TOP " + sample + " * FROM " + table;
                            }
                        } catch (Exception ignored) {}
                    }

                    List<String> colNames = new ArrayList<>();
                    for (ColumnInfo col : columns) colNames.add(col.name);

                    try (Statement stmt = conn.createStatement();
                         ResultSet rs = stmt.executeQuery(sql)) {
                        while (rs.next()) {
                            StringBuilder insertSql = new StringBuilder("INSERT INTO ");
                            insertSql.append(table).append(" (");
                            for (int i = 0; i < colNames.size(); i++) {
                                if (i > 0) insertSql.append(", ");
                                insertSql.append(colNames.get(i));
                            }
                            insertSql.append(") VALUES (");
                            for (int i = 0; i < colNames.size(); i++) {
                                if (i > 0) insertSql.append(", ");
                                Object val = rs.getObject(colNames.get(i));
                                if (val == null) {
                                    insertSql.append("NULL");
                                } else if (val instanceof Number || val instanceof Boolean) {
                                    insertSql.append(val);
                                } else {
                                    String sv = val.toString().replace("'", "''");
                                    insertSql.append("'").append(sv).append("'");
                                }
                            }
                            insertSql.append(");");
                            output.append("\n").append(insertSql.toString());
                        }
                    }
                }
            }

            long elapsed = System.currentTimeMillis() - start;
            String result = output.toString();
            ResultWriter.printResult(r -> {
                r.put("success", true);
                r.put("table", table);
                r.put("generatedSql", result);
                r.put("timeMs", elapsed);
            }, main.isPretty());
            return 0;
        } catch (Exception e) {
            ResultWriter.printError(e, System.currentTimeMillis() - start);
            return 1;
        }
    }

    private boolean isFixedLengthType(String type) {
        String t = type.toUpperCase();
        return t.equals("INT") || t.equals("INTEGER") || t.equals("BIGINT") || t.equals("SMALLINT")
                || t.equals("TINYINT") || t.equals("FLOAT") || t.equals("DOUBLE") || t.equals("REAL")
                || t.equals("DATE") || t.equals("TIME") || t.equals("TIMESTAMP")
                || t.equals("BOOLEAN") || t.equals("BIT") || t.equals("BLOB") || t.equals("CLOB");
    }

    private static class ColumnInfo {
        String name;
        String type;
        int size;
        int scale;
        boolean nullable;
        boolean pk;
        boolean autoIncrement;
    }
}
