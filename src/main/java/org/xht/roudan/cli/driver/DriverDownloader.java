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

        String jarPath = DriverRegistry.cachePath(info);
        File jarFile = new File(jarPath);
        if (jarFile.exists()) {
            return jarPath;
        }

        String downloadUrl = DriverRegistry.mavenUrl(info);
        if (downloadUrl == null) throw new IllegalArgumentException("Cannot resolve Maven download URL");

        FileUtil.mkdir(jarFile.getParentFile());

        System.err.print("[driver] Downloading " + info.getMavenArtifact() + " " + info.getMavenVersion() + "... ");
        System.err.flush();

        HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setInstanceFollowRedirects(true);

        int status = conn.getResponseCode();
        if (status != 200) {
            // Try with full classifier: artifact-version.jar  (for some artifacts like mssql-jdbc)
            String altUrl = DriverRegistry.mavenUrl(info).replace(".jar", ".jar");
            conn = (HttpURLConnection) new URL(altUrl).openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(true);
            status = conn.getResponseCode();
            if (status != 200) {
                System.err.println("FAILED (HTTP " + status + ")");
                throw new RuntimeException("Failed to download driver from Maven Central: HTTP " + status);
            }
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
                    System.err.print("\r[driver] Downloading " + info.getMavenArtifact() + " " + info.getMavenVersion() + "... " + pct + "%  ");
                    System.err.flush();
                    lastPrint = now;
                }
            }
        }

        System.err.println("\r[driver] Downloaded " + info.getMavenArtifact() + " " + info.getMavenVersion() + " (" + FileUtil.readableFileSize(jarFile) + ")   ");
        System.err.flush();

        if (!jarFile.exists()) {
            throw new RuntimeException("Download failed: " + jarPath);
        }
        return jarPath;
    }
}
