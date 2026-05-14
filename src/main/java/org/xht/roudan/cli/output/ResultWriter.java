package org.xht.roudan.cli.output;

import cn.hutool.json.JSONUtil;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ResultWriter {

    public static void printResult(Consumer<Map<String, Object>> builder, boolean pretty) {
        Map<String, Object> result = new LinkedHashMap<>();
        builder.accept(result);
        String json = pretty ? JSONUtil.toJsonPrettyStr(result) : JSONUtil.toJsonStr(result);
        System.out.println(json);
    }

    public static void printQueryResult(
            List<String> cols, List<Object[]> rows, int rowCount, long timeMs,
            String format, boolean pretty, boolean noHeader
    ) {
        if ("csv".equalsIgnoreCase(format)) {
            System.out.println(formatCsv(cols, rows, noHeader));
        } else if ("table".equalsIgnoreCase(format)) {
            System.out.println(formatTable(cols, rows, noHeader));
        } else {
            boolean pp = pretty || "json-pretty".equalsIgnoreCase(format);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", true);
            result.put("rowCount", rowCount);
            result.put("cols", cols);
            result.put("rows", rows);
            result.put("timeMs", timeMs);
            String json = pp ? JSONUtil.toJsonPrettyStr(result) : JSONUtil.toJsonStr(result);
            System.out.println(json);
        }
    }

    public static void printError(Exception e) {
        printError(e, 0);
    }

    public static void printError(Exception e, long timeMs) {
        String errorCode = resolveErrorCode(e);
        String message = e.getClass().getSimpleName() + ": " + e.getMessage();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("error", message);
        result.put("errorCode", errorCode);
        result.put("timeMs", timeMs);
        System.out.println(JSONUtil.toJsonStr(result));
    }

    public static void printParamError(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("error", message);
        result.put("errorCode", "PARAM_ERROR");
        result.put("timeMs", 0);
        System.out.println(JSONUtil.toJsonStr(result));
    }

    private static String resolveErrorCode(Exception e) {
        String name = resolveName(e).toLowerCase();
        if (name.contains("config") || name.contains("yaml") || name.contains("parse"))
            return "CONFIG_ERROR";
        if (name.contains("driver") || name.contains("classnotfound"))
            return "DRIVER_ERROR";
        if (name.contains("connection") || name.contains("communications") || name.contains("timeout"))
            return "CONNECTION_ERROR";
        if (name.contains("sql") || name.contains("dataIntegrity") || name.contains("syntax")
                || name.contains("jdbc") || name.contains("table") || name.contains("column"))
            return "SQL_ERROR";
        if (name.contains("illegalargument"))
            return "PARAM_ERROR";
        return "UNKNOWN_ERROR";
    }

    private static String resolveName(Throwable t) {
        StringBuilder sb = new StringBuilder();
        while (t != null) {
            sb.append(t.getClass().getName());
            t = t.getCause();
        }
        return sb.toString();
    }

    private static String formatCsv(List<String> cols, List<Object[]> rows, boolean noHeader) {
        StringBuilder sb = new StringBuilder();
        if (!noHeader && !cols.isEmpty()) {
            for (int i = 0; i < cols.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(escapeCsv(cols.get(i)));
            }
            sb.append('\n');
        }
        for (Object[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (i > 0) sb.append(',');
                sb.append(escapeCsv(String.valueOf(row[i])));
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String escapeCsv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }

    private static String formatTable(List<String> cols, List<Object[]> rows, boolean noHeader) {
        int[] widths = new int[cols.size()];
        for (int i = 0; i < cols.size(); i++)
            widths[i] = cols.get(i).length();
        for (Object[] row : rows) {
            for (int i = 0; i < row.length && i < widths.length; i++) {
                int len = String.valueOf(row[i]).length();
                if (len > widths[i]) widths[i] = len;
            }
        }

        StringBuilder sb = new StringBuilder();
        if (!noHeader) {
            sb.append('+');
            for (int w : widths) {
                for (int i = 0; i < w + 2; i++) sb.append('-');
                sb.append('+');
            }
            sb.append('\n');
            sb.append('|');
            for (int i = 0; i < cols.size(); i++) {
                sb.append(' ').append(padRight(cols.get(i), widths[i])).append(" |");
            }
            sb.append('\n');
        }
        sb.append('+');
        for (int w : widths) {
            for (int i = 0; i < w + 2; i++) sb.append('-');
            sb.append('+');
        }
        sb.append('\n');
        for (Object[] row : rows) {
            sb.append('|');
            for (int i = 0; i < row.length; i++) {
                sb.append(' ').append(padRight(String.valueOf(row[i]), widths[i])).append(" |");
            }
            sb.append('\n');
        }
        sb.append('+');
        for (int w : widths) {
            for (int i = 0; i < w + 2; i++) sb.append('-');
            sb.append('+');
        }
        return sb.toString();
    }

    private static String padRight(String s, int n) {
        if (s.length() >= n) return s;
        StringBuilder sb = new StringBuilder(s);
        for (int i = s.length(); i < n; i++) sb.append(' ');
        return sb.toString();
    }
}
