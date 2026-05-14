package org.xht.roudan.cli.driver;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;

@Slf4j
public class DriverLoader {

    public static Driver load(String driverJarPath, String driverClassName) throws Exception {
        File jarFile = new File(driverJarPath);
        if (!jarFile.exists()) {
            throw new IllegalArgumentException("Driver JAR not found: " + driverJarPath);
        }

        URL[] urls = { jarFile.toURI().toURL() };
        URLClassLoader loader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader());

        Class<?> clazz = Class.forName(driverClassName, true, loader);
        Driver driver = (Driver) clazz.getDeclaredConstructor().newInstance();

        log.debug("Loaded JDBC driver: {} from {}", driverClassName, driverJarPath);
        return driver;
    }
}
