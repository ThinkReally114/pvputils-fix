package com.pvp_utils_skija_patch;

import com.pvp_utils_skija_patch.util.PlatformDetector;
import com.pvp_utils_skija_patch.util.PlatformDetector.Platform;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class NativeLibraryPreloader {

    private static final String SKIJA_VERSION = "0.143.16";
    private static final String[] MAVEN_MIRRORS = {
            "https://maven.aliyun.com/repository/central",
            "https://mirrors.tencent.com/nexus/repository/maven-public",
            "https://maven.google.com",
            "https://repo1.maven.org/maven2"
    };
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(60);

    private static boolean nativeAddedToClasspath = false;

    public static boolean isNativeLoaded() {
        return nativeAddedToClasspath;
    }

    public static void preload() throws Exception {
        if (nativeAddedToClasspath) {
            return;
        }

        Platform platform = PlatformDetector.detectPlatform();
        if (platform == Platform.UNKNOWN) {
            SkijaPatchMod.LOGGER.warn("不支持的平台，无法预加载原生库 / Unsupported platform, cannot preload native library");
            return;
        }

        Path cacheDir = getCacheDir();
        Path cachedJar = cacheDir.resolve(platform.getArtifactSuffix() + ".jar");

        if (Files.exists(cachedJar)) {
            SkijaPatchMod.LOGGER.info("找到已缓存的原生 JAR: {} / Found cached native JAR: {}", cachedJar, cachedJar);
        } else {
            SkijaPatchMod.LOGGER.info("未找到缓存，正在为平台 {} 下载原生库... / No cached JAR found, downloading for platform: {}", platform, platform);
            downloadJar(platform, cachedJar);
        }

        addToClasspath(cachedJar);
        nativeAddedToClasspath = true;
        SkijaPatchMod.LOGGER.info("原生 JAR 已添加到 classpath / Native JAR added to classpath");
    }

    private static Path getCacheDir() {
        String userHome = System.getProperty("user.home", ".");
        return Path.of(userHome, ".skija-natives", SKIJA_VERSION);
    }

    private static void downloadJar(Platform platform, Path targetPath) throws Exception {
        String artifactName = "skija-" + platform.getArtifactSuffix();
        String artifactPath = String.format("/io/github/humbleui/%s/%s/%s-%s.jar",
                artifactName, SKIJA_VERSION, artifactName, SKIJA_VERSION);

        Files.createDirectories(targetPath.getParent());

        IOException lastException = null;
        for (String mirror : MAVEN_MIRRORS) {
            String jarUrl = mirror + artifactPath;
            SkijaPatchMod.LOGGER.info("正在下载: {} / Downloading: {}", jarUrl, jarUrl);

            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(DOWNLOAD_TIMEOUT)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build();

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(jarUrl))
                        .timeout(DOWNLOAD_TIMEOUT)
                        .header("User-Agent", "PVPUtils-Skija-Patch/" + SKIJA_VERSION)
                        .header("Accept", "application/octet-stream,*/*")
                        .GET()
                        .build();

                HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

                if (response.statusCode() == 200) {
                    try (InputStream in = response.body()) {
                        Files.copy(in, targetPath);
                    }
                    SkijaPatchMod.LOGGER.info("下载完成 / Download complete");
                    return;
                }

                lastException = new IOException("HTTP " + response.statusCode());
                SkijaPatchMod.LOGGER.warn("下载失败 ({}): {} / Download failed ({}): {}", jarUrl, response.statusCode(), jarUrl, response.statusCode());

            } catch (Exception e) {
                lastException = new IOException(e.getMessage(), e);
                SkijaPatchMod.LOGGER.warn("下载失败: {} / Download failed: {} - {}", jarUrl, jarUrl, e.getMessage());
            }
        }

        throw new IOException("所有镜像源下载失败 / All mirrors failed", lastException);
    }

    private static void addToClasspath(Path jarPath) throws Exception {
        URL jarUrl = jarPath.toUri().toURL();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();

        if (cl instanceof URLClassLoader) {
            Method addURL = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            addURL.setAccessible(true);
            addURL.invoke(cl, jarUrl);
        } else {
            SkijaPatchMod.LOGGER.warn("类加载器不支持动态添加 URL: {} / ClassLoader does not support addURL: {}", cl.getClass().getName(), cl.getClass().getName());
        }
    }

    public static String getCurrentPlatform() {
        return PlatformDetector.detectPlatform().name();
    }
}
