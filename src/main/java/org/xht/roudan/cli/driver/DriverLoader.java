package org.xht.roudan.cli.driver;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class DriverLoader {

    public static Driver load(String driverJarPaths, String driverClassName) throws Exception {
        return load(splitJarPaths(driverJarPaths), driverClassName);
    }

    public static Driver load(List<String> jarPaths, String driverClassName) throws Exception {
        List<URL> urls = new ArrayList<>();
        for (String path : jarPaths) {
            File jarFile = new File(path);
            if (!jarFile.exists()) {
                throw new IllegalArgumentException("Driver JAR not found: " + path);
            }
            urls.add(jarFile.toURI().toURL());
        }

        if (urls.isEmpty()) {
            throw new IllegalArgumentException("No driver JAR specified");
        }

        URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[0]), ClassLoader.getSystemClassLoader());

        Class<?> clazz = Class.forName(driverClassName, true, loader);
        Driver driver = (Driver) clazz.getDeclaredConstructor().newInstance();

        DriverManager.registerDriver(driver);

        Thread.currentThread().setContextClassLoader(loader);

        return driver;
    }

    public static List<String> splitJarPaths(String paths) {
        List<String> result = new ArrayList<>();
        if (paths == null || paths.isEmpty()) return result;
        for (String p : paths.split("[;,]")) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }
}
