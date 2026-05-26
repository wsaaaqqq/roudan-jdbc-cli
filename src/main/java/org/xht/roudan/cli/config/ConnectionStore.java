package org.xht.roudan.cli.config;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConnectionStore {

    private static final String STORE_DIR = System.getProperty("user.home") + "/.roudan";
    private static final String STORE_FILE = STORE_DIR + "/connections.json";

    public static synchronized void save(String name, String url, String user, String password,
                                         String driverClass, String driverJar) {
        JSONObject root = loadRoot();
        JSONObject conns = root.getJSONObject("connections");
        if (conns == null) {
            conns = new JSONObject();
            root.set("connections", conns);
        }

        JSONObject entry = new JSONObject();
        entry.set("url", url);
        entry.set("user", user);
        entry.set("password", password);
        entry.set("driverClass", driverClass);
        entry.set("driverJar", driverJar);
        conns.set(name, entry);

        root.set("current", name);
        writeRoot(root);
    }

    public static synchronized void remove(String name) {
        JSONObject root = loadRoot();
        JSONObject conns = root.getJSONObject("connections");
        if (conns != null) {
            conns.remove(name);
        }
        if (name.equals(root.getStr("current"))) {
            root.set("current", null);
        }
        writeRoot(root);
    }

    public static synchronized void rename(String oldName, String newName) {
        JSONObject root = loadRoot();
        JSONObject conns = root.getJSONObject("connections");
        if (conns == null || !conns.containsKey(oldName)) {
            return;
        }
        conns.set(newName, conns.get(oldName));
        conns.remove(oldName);
        if (oldName.equals(root.getStr("current"))) {
            root.set("current", newName);
        }
        writeRoot(root);
    }

    public static synchronized void setCurrent(String name) {
        JSONObject root = loadRoot();
        root.set("current", name);
        writeRoot(root);
    }

    public static synchronized CliConfig getCurrent() {
        JSONObject root = loadRoot();
        String current = root.getStr("current");
        if (current == null) return null;

        JSONObject conns = root.getJSONObject("connections");
        if (conns == null) return null;

        JSONObject entry = conns.getJSONObject(current);
        if (entry == null) return null;

        return toConfig(entry);
    }

    public static synchronized CliConfig getByName(String name) {
        JSONObject root = loadRoot();
        JSONObject conns = root.getJSONObject("connections");
        if (conns == null) return null;

        JSONObject entry = conns.getJSONObject(name);
        if (entry == null) return null;

        return toConfig(entry);
    }

    public static synchronized Map<String, CliConfig> list() {
        Map<String, CliConfig> result = new LinkedHashMap<>();
        JSONObject root = loadRoot();
        String current = root.getStr("current");
        JSONObject conns = root.getJSONObject("connections");
        if (conns == null) return result;

        for (String key : conns.keySet()) {
            CliConfig cfg = toConfig(conns.getJSONObject(key));
            result.put(key + (key.equals(current) ? " *" : ""), cfg);
        }
        return result;
    }

    public static synchronized String getCurrentName() {
        JSONObject root = loadRoot();
        return root.getStr("current");
    }

    private static CliConfig toConfig(JSONObject entry) {
        CliConfig config = new CliConfig();
        config.setUrl(entry.getStr("url"));
        config.setUser(entry.getStr("user"));
        config.setPassword(entry.getStr("password"));
        config.setDriverClass(entry.getStr("driverClass"));
        config.setDriverJar(entry.getStr("driverJar"));
        return config;
    }

    private static JSONObject loadRoot() {
        File file = new File(STORE_FILE);
        if (!file.exists()) {
            return new JSONObject();
        }
        String content = FileUtil.readString(file, StandardCharsets.UTF_8);
        if (content == null || content.trim().isEmpty()) {
            return new JSONObject();
        }
        return JSONUtil.parseObj(content);
    }

    private static void writeRoot(JSONObject root) {
        FileUtil.mkdir(STORE_DIR);
        FileUtil.writeString(root.toStringPretty(), new File(STORE_FILE), StandardCharsets.UTF_8);
    }
}
