package org.xht.roudan.cli.driver;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class DriverDownloader {

    private static final int BUFFER_SIZE = 8192;

    public static String download(DriverRegistry.DriverInfo info) throws Exception {
        if (info == null) throw new IllegalArgumentException("No driver info for auto-download");
        if (!info.isOnMavenCentral()) {
            throw new IllegalArgumentException(
                    "Driver for " + info.getDriverClass() + " is not available on Maven Central. " +
                    "Download the JAR manually and use -j to specify its path.");
        }

        StringBuilder allPaths = new StringBuilder();

        String primaryPath = downloadJar(info.getMavenGroup(), info.getMavenArtifact(), info.getMavenVersion(),
                info.getMavenArtifact() + " " + info.getMavenVersion());
        allPaths.append(primaryPath);

        for (String[] dep : info.getExtraDependencies()) {
            String depPath = downloadJar(dep[0], dep[1], dep[2], dep[1] + " " + dep[2]);
            allPaths.append(";").append(depPath);
        }

        return allPaths.toString();
    }

    private static String downloadJar(String group, String artifact, String version, String label) throws Exception {
        String jarPath = DriverRegistry.cachePathExtra(group, artifact, version);
        File jarFile = new File(jarPath);
        if (jarFile.exists()) {
            return jarPath;
        }

        String downloadUrl = DriverRegistry.mavenUrlExtra(group, artifact, version);
        FileUtil.mkdir(jarFile.getParentFile());

        System.err.print("[driver] Downloading " + label + "... ");
        System.err.flush();

        HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setInstanceFollowRedirects(true);

        int status = conn.getResponseCode();
        if (status != 200) {
            System.err.println("FAILED (HTTP " + status + ")");
            throw new RuntimeException("Failed to download " + label + " from Maven Central: HTTP " + status);
        }

        int totalBytes = conn.getContentLength();
        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(jarFile)) {
            byte[] buf = new byte[BUFFER_SIZE];
            int read, downloaded = 0;
            long lastPrint = 0;
            while ((read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
                downloaded += read;
                long now = System.currentTimeMillis();
                if (totalBytes > 0 && now - lastPrint > 500) {
                    int pct = (int) ((long) downloaded * 100 / totalBytes);
                    System.err.print("\r[driver] Downloading " + label + "... " + pct + "%  ");
                    System.err.flush();
                    lastPrint = now;
                }
            }
        }

        System.err.println("\r[driver] Downloaded " + label + " (" + FileUtil.readableFileSize(jarFile) + ")   ");
        System.err.flush();

        if (!jarFile.exists()) {
            throw new RuntimeException("Download failed: " + jarPath);
        }
        return jarPath;
    }
}
