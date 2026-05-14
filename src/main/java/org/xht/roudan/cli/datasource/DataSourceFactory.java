package org.xht.roudan.cli.datasource;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

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
        log.debug("Creating DataSource for: {}", url);

        try {
            Class<?> hikariDs = Class.forName("com.zaxxer.hikari.HikariDataSource");
            return createHikari(hikariDs, driver, url, user, password);
        } catch (ClassNotFoundException e) {
            log.debug("HikariCP not available, using SimpleDataSource");
        }

        return new SimpleDataSource(driver, url, user, password);
    }

    private static DataSource createHikari(Class<?> hikariClass, Driver driver, String url,
                                            String user, String password) throws Exception {
        Object ds = hikariClass.getDeclaredConstructor().newInstance();
        hikariClass.getMethod("setJdbcUrl", String.class).invoke(ds, url);
        if (StrUtil.isNotBlank(user)) {
            hikariClass.getMethod("setUsername", String.class).invoke(ds, user);
        }
        if (password != null) {
            hikariClass.getMethod("setPassword", String.class).invoke(ds, password);
        }
        hikariClass.getMethod("setMaximumPoolSize", int.class).invoke(ds, 2);
        hikariClass.getMethod("setMinimumIdle", int.class).invoke(ds, 0);
        hikariClass.getMethod("setConnectionTimeout", long.class).invoke(ds, 10000L);
        return (DataSource) ds;
    }

    public static class SimpleDataSource implements DataSource {
        private final Driver driver;
        private final String url;
        private final String user;
        private final String password;

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
        @Override public void setLoginTimeout(int seconds) {}
        @Override public int getLoginTimeout() { return 0; }
        @Override public Logger getParentLogger() throws SQLFeatureNotSupportedException { throw new SQLFeatureNotSupportedException(); }
        @Override public <T> T unwrap(Class<T> iface) throws SQLException { throw new SQLException("unwrap not supported"); }
        @Override public boolean isWrapperFor(Class<?> iface) { return false; }
    }
}
