package org.xht.roudan.cli.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CliConfig {
    private String url;
    private String user;
    private String password;
    private String driverClass;
    private String driverJar;
    private Settings settings;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Settings {
        private boolean showSql;
        private boolean autoCommit;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class YamlRoot {
        private Map<String, DatasourceConfig> datasources;
        private Settings settings;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DatasourceConfig {
        private String url;
        private String user;
        private String password;
        private String driver;
        private String driverJar;
    }
}
