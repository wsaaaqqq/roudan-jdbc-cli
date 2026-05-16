package org.xht.roudan.cli.driver;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class DriverRegistry {

    @Getter
    public static class DriverInfo {
        private final String driverClass;
        private final String mavenGroup;
        private final String mavenArtifact;
        private final String mavenVersion;
        private final boolean onMavenCentral;

        public DriverInfo(String driverClass, String mavenGroup, String mavenArtifact, String mavenVersion, boolean onMavenCentral) {
            this.driverClass = driverClass;
            this.mavenGroup = mavenGroup;
            this.mavenArtifact = mavenArtifact;
            this.mavenVersion = mavenVersion;
            this.onMavenCentral = onMavenCentral;
        }
    }

    private static final Map<String, DriverInfo> REGISTRY = new LinkedHashMap<>();

    static {
        reg("jdbc:mysql://",       "com.mysql.cj.jdbc.Driver",          "mysql", "mysql-connector-j", "9.2.0", true);
        reg("jdbc:postgresql://",  "org.postgresql.Driver",             "org.postgresql", "postgresql", "42.7.4", true);
        reg("jdbc:oracle:thin:@",  "oracle.jdbc.OracleDriver",          "com.oracle.database.jdbc", "ojdbc8", "21.15.0.0", true);
        reg("jdbc:sqlserver://",   "com.microsoft.sqlserver.jdbc.SQLServerDriver", "com.microsoft.sqlserver", "mssql-jdbc", "12.8.1.jre8", true);
        reg("jdbc:h2:",           "org.h2.Driver",                      "com.h2database", "h2", "2.2.224", true);
        reg("jdbc:sqlite:",       "org.sqlite.JDBC",                    "org.xerial", "sqlite-jdbc", "3.46.1.3", true);
        reg("jdbc:mariadb://",    "org.mariadb.jdbc.Driver",            "org.mariadb.jdbc", "mariadb-java-client", "3.5.1", true);
        reg("jdbc:derby://",      "org.apache.derby.jdbc.ClientDriver", "org.apache.derby", "derbyclient", "10.17.1.0", true);
        reg("jdbc:hsqldb:",       "org.hsqldb.jdbc.JDBCDriver",         "org.hsqldb", "hsqldb", "2.7.4", true);
        reg("jdbc:dm://",         "dm.jdbc.driver.DmDriver",            null, null, null, false);
    }

    private static void reg(String urlPrefix, String driverClass, String mg, String ma, String mv, boolean central) {
        REGISTRY.put(urlPrefix, new DriverInfo(driverClass, mg, ma, mv, central));
    }

    public static DriverInfo resolve(String jdbcUrl) {
        if (jdbcUrl == null) return null;
        String url = jdbcUrl.toLowerCase().trim();

        DriverInfo best = null;
        int bestLen = 0;
        for (Map.Entry<String, DriverInfo> e : REGISTRY.entrySet()) {
            String prefix = e.getKey().toLowerCase();
            if (url.startsWith(prefix) && prefix.length() > bestLen) {
                best = e.getValue();
                bestLen = prefix.length();
            }
        }
        return best;
    }

    public static String mavenUrl(DriverInfo info) {
        if (info == null || !info.onMavenCentral) return null;
        String groupPath = info.mavenGroup.replace('.', '/');
        return String.format("https://repo1.maven.org/maven2/%s/%s/%s/%s-%s.jar",
                groupPath, info.mavenArtifact, info.mavenVersion, info.mavenArtifact, info.mavenVersion);
    }

    public static String cachePath(DriverInfo info) {
        String home = System.getProperty("user.home");
        return String.format("%s/.roudan-cli/drivers/%s-%s-%s.jar",
                home, info.mavenGroup, info.mavenArtifact, info.mavenVersion);
    }
}
