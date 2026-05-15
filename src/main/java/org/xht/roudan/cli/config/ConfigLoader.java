package org.xht.roudan.cli.config;

import cn.hutool.core.io.FileUtil;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

public class ConfigLoader {

    private static final String DEFAULT_CONFIG_FILE = "roudan-config.yaml";

    public static CliConfig load(String configFile, String cliUrl, String cliUser,
                                  String cliPassword, String cliDriverClass, String cliDriverJar) throws Exception {
        return load(configFile, cliUrl, cliUser, cliPassword, cliDriverClass, cliDriverJar, null);
    }

    public static CliConfig load(String configFile, String cliUrl, String cliUser,
                                  String cliPassword, String cliDriverClass, String cliDriverJar,
                                  String savedName) throws Exception {
        CliConfig config = new CliConfig();

        if (configFile != null) {
            loadFromYaml(configFile, config);
        } else if (cliUrl == null) {
            String envConfig = System.getenv("ROUDAN_JDBC_CONFIG");
            if (envConfig != null) {
                loadFromYaml(envConfig, config);
            } else if (new File(DEFAULT_CONFIG_FILE).exists()) {
                loadFromYaml(DEFAULT_CONFIG_FILE, config);
            } else {
                String homeConfig = System.getProperty("user.home") + "/.roudan/config.yaml";
                if (FileUtil.exist(homeConfig)) {
                    loadFromYaml(homeConfig, config);
                }
            }
        }

        mergeEnv(config);
        mergeCli(config, cliUrl, cliUser, cliPassword, cliDriverClass, cliDriverJar);
        mergeConnectionStore(config, savedName);
        validate(config);
        return config;
    }

    private static void loadFromYaml(String path, CliConfig config) throws Exception {
        try (InputStream in = new FileInputStream(path)) {
            Yaml yaml = new Yaml();
            CliConfig.YamlRoot root = yaml.loadAs(in, CliConfig.YamlRoot.class);
            if (root != null && root.getDatasources() != null) {
                CliConfig.DatasourceConfig ds = root.getDatasources().get("default");
                if (ds != null) {
                    config.setUrl(ds.getUrl());
                    config.setUser(ds.getUser());
                    config.setPassword(ds.getPassword());
                    config.setDriverClass(ds.getDriver());
                    config.setDriverJar(ds.getDriverJar());
                }
            }
            if (root != null && root.getSettings() != null) {
                CliConfig.Settings settings = new CliConfig.Settings();
                CliConfig.Settings src = root.getSettings();
                settings.setShowSql(src.isShowSql());
                settings.setAutoCommit(src.isAutoCommit());
                settings.setMaxPoolSize(src.getMaxPoolSize());
                settings.setMinIdle(src.getMinIdle());
                settings.setConnectionTimeout(src.getConnectionTimeout());
                config.setSettings(settings);
            }
        }
    }

    private static void mergeEnv(CliConfig config) {
        if (config.getUrl() == null) config.setUrl(getEnv("ROUDAN_JDBC_URL"));
        if (config.getUser() == null) config.setUser(getEnv("ROUDAN_JDBC_USER"));
        if (config.getPassword() == null) config.setPassword(getEnv("ROUDAN_JDBC_PASSWORD"));
        if (config.getDriverClass() == null) config.setDriverClass(getEnv("ROUDAN_JDBC_DRIVER"));
        if (config.getDriverJar() == null) config.setDriverJar(getEnv("ROUDAN_JDBC_DRIVER_JAR"));
    }

    private static void mergeCli(CliConfig config, String url, String user, String password,
                                  String driverClass, String driverJar) {
        if (url != null) config.setUrl(url);
        if (user != null) config.setUser(user);
        if (password != null) config.setPassword(password);
        if (driverClass != null) config.setDriverClass(driverClass);
        if (driverJar != null) config.setDriverJar(driverJar);
    }

    private static void mergeConnectionStore(CliConfig config, String savedName) {
        if (config.getUrl() != null) return;

        CliConfig saved;
        if (savedName != null) {
            saved = ConnectionStore.getByName(savedName);
        } else {
            saved = ConnectionStore.getCurrent();
        }
        if (saved != null) {
            if (config.getUrl() == null) config.setUrl(saved.getUrl());
            if (config.getUser() == null) config.setUser(saved.getUser());
            if (config.getPassword() == null) config.setPassword(saved.getPassword());
            if (config.getDriverClass() == null) config.setDriverClass(saved.getDriverClass());
            if (config.getDriverJar() == null) config.setDriverJar(saved.getDriverJar());
        }
    }

    public static CliConfig.DatasourceConfig loadDatasource(String configFile, String datasourceName) throws Exception {
        try (InputStream in = new FileInputStream(configFile)) {
            Yaml yaml = new Yaml();
            CliConfig.YamlRoot root = yaml.loadAs(in, CliConfig.YamlRoot.class);
            if (root != null && root.getDatasources() != null) {
                CliConfig.DatasourceConfig ds = root.getDatasources().get(datasourceName);
                if (ds == null) {
                    throw new IllegalArgumentException("Datasource '" + datasourceName + "' not found in config: " + configFile);
                }
                return ds;
            }
        }
        throw new IllegalArgumentException("No datasources defined in config: " + configFile);
    }

    private static void validate(CliConfig config) {
        if (config.getUrl() == null) {
            throw new IllegalArgumentException("JDBC URL is required. Provide -u or configure in YAML.");
        }
    }

    private static String getEnv(String key) {
        String val = System.getenv(key);
        return val != null && !val.isEmpty() ? val : null;
    }
}
