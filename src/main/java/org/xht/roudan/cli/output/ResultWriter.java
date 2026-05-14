package org.xht.roudan.cli.output;

import cn.hutool.json.JSONUtil;

import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public class ResultWriter {

    private static final PrintStream OUT = System.out;

    public static void printResult(Consumer<Map<String, Object>> builder, boolean pretty) {
        Map<String, Object> result = new LinkedHashMap<>();
        builder.accept(result);
        String json = pretty ? JSONUtil.toJsonPrettyStr(result) : JSONUtil.toJsonStr(result);
        OUT.println(json);
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
        OUT.println(JSONUtil.toJsonStr(result));
    }

    public static void printParamError(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", false);
        result.put("error", message);
        result.put("errorCode", "PARAM_ERROR");
        result.put("timeMs", 0);
        OUT.println(JSONUtil.toJsonStr(result));
    }

    private static String resolveErrorCode(Exception e) {
        String name = e.getClass().getName();
        if (name.contains("Config") || name.contains("Yaml") || name.contains("Parse")) {
            return "CONFIG_ERROR";
        }
        if (name.contains("Driver") || name.contains("ClassNotFoundException")) {
            return "DRIVER_ERROR";
        }
        if (name.contains("Connection") || name.contains("Communications") || name.contains("Timeout")) {
            return "CONNECTION_ERROR";
        }
        if (name.contains("SQL") || name.contains("DataIntegrity") || name.contains("Syntax")) {
            return "SQL_ERROR";
        }
        if (name.contains("IllegalArgument")) {
            return "PARAM_ERROR";
        }
        return "UNKNOWN_ERROR";
    }
}
