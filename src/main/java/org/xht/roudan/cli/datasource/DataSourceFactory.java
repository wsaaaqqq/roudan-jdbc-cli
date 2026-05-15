package org.xht.roudan.cli.datasource;

import cn.hutool.core.util.StrUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.xht.roudan.cli.config.CliConfig;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

@Slf4j
public class DataSourceFactory {

    public static DataSource create(Driver driver, String url, String user, String password) throws Exception {
        return create(driver, url, user, password, null, 30000, null);
    }

    public static DataSource create(Driver driver, String url, String user, String password,
                                      CliConfig.Settings settings) throws Exception {
        return create(driver, url, user, password, settings, settings != null ? (int)settings.getConnectionTimeout() : 10000, null);
    }

    public static DataSource create(Driver driver, String url, String user, String password,
                                      CliConfig.Settings settings, int connectTimeout) throws Exception {
        return create(driver, url, user, password, settings, connectTimeout, null);
    }

    public static DataSource create(Driver driver, String url, String user, String password,
                                      CliConfig.Settings settings, int connectTimeout, String driverClassName) throws Exception {
        log.debug("Creating DataSource for: {}", url);
        return createHikari(url, user, password, settings, connectTimeout, driverClassName);
    }

    private static HikariDataSource createHikari(String url, String user, String password,
                                                   CliConfig.Settings settings, int connectTimeout, String driverClassName) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        if (StrUtil.isNotBlank(user)) config.setUsername(user);
        if (password != null) config.setPassword(password);
        if (StrUtil.isNotBlank(driverClassName)) config.setDriverClassName(driverClassName);
        if (settings != null) {
            config.setMaximumPoolSize(settings.getMaxPoolSize());
            config.setMinimumIdle(settings.getMinIdle());
        } else {
            config.setMaximumPoolSize(2);
            config.setMinimumIdle(0);
        }
        config.setConnectionTimeout(connectTimeout);
        return new HikariDataSource(config);
    }

    public static class SimpleDataSource implements DataSource {
        private final Driver driver;
        private final String url;
        private final String user;
        private final String password;
        private int loginTimeout;

        public SimpleDataSource(Driver driver, String url, String user, String password) {
            this.driver = driver;
            this.url = url;
            this.user = user;
            this.password = password;
        }

        @Override
        public Connection getConnection() throws SQLException {
            Properties props = new Properties();
            if (StrUtil.isNotBlank(user)) {
                props.setProperty("user", user);
            }
            if (password != null) {
                props.setProperty("password", password);
            }
            return driver.connect(url, props);
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            Properties props = new Properties();
            props.setProperty("user", username);
            if (password != null) props.setProperty("password", password);
            return driver.connect(url, props);
        }

        @Override public PrintWriter getLogWriter() { return null; }
        @Override public void setLogWriter(PrintWriter out) {}
        @Override public void setLoginTimeout(int seconds) { this.loginTimeout = seconds; }
        @Override public int getLoginTimeout() { return loginTimeout; }
        @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException { throw new SQLFeatureNotSupportedException(); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("unwrap not supported"); }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }
}
