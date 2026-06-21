package com.pvp_utils_skija_patch;

import com.pvp_utils_skija_patch.util.PlatformDetector;
import com.pvp_utils_skija_patch.util.PlatformDetector.Platform;

import java.io.*;
import java.net.URI;
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
    private static final String MAVEN_BASE = "https://repo1.maven.org/maven2/io/github/humbleui";
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(60);

    private static boolean nativeLoaded = false;
    private static Path cacheDir;

    public static boolean isNativeLoaded() {
        return nativeLoaded;
    }

    public static void preload() throws Exception {
        if (nativeLoaded) {
            return;
        }

        Platform platform = PlatformDetector.detectPlatform();
        if (platform == Platform.UNKNOWN) {
            SkijaPatchMod.LOGGER.warn("不支持的平台，无法预加载原生库 / Unsupported platform, cannot preload native library");
            return;
        }

        cacheDir = getCacheDir();
        Path cachedLib = cacheDir.resolve(platform.getArtifactSuffix()).resolve(platform.getNativeLibraryName());

        if (Files.exists(cachedLib)) {
            SkijaPatchMod.LOGGER.info("找到已缓存的原生库: {} / Found cached native library: {}", cachedLib, cachedLib);
            loadLibrary(cachedLib);
            return;
        }

        SkijaPatchMod.LOGGER.info("未找到缓存，正在为平台 {} 下载原生库... / No cached library found, downloading for platform: {}", platform, platform);
        downloadAndExtract(platform, cachedLib);
        loadLibrary(cachedLib);
    }

    private static Path getCacheDir() {
        String userHome = System.getProperty("user.home", ".");
        return Path.of(userHome, ".skija-natives", SKIJA_VERSION);
    }

    private static void downloadAndExtract(Platform platform, Path targetPath) throws Exception {
        String artifactName = "skija-" + platform.getArtifactSuffix();
        String jarUrl = String.format("%s/%s/%s/%s-%s.jar",
                MAVEN_BASE, artifactName, SKIJA_VERSION, artifactName, SKIJA_VERSION);

        SkijaPatchMod.LOGGER.info("正在下载: {} / Downloading: {}", jarUrl, jarUrl);

        Path tempDir = Files.createTempDirectory("skija-download-");
        Path tempJar = tempDir.resolve(artifactName + ".jar");

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(DOWNLOAD_TIMEOUT)
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(jarUrl))
                    .timeout(DOWNLOAD_TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                throw new IOException("下载失败，HTTP 状态码: " + response.statusCode() + " / Download failed with HTTP " + response.statusCode() + " for " + jarUrl);
            }

            try (InputStream in = response.body()) {
                Files.copy(in, tempJar);
            }

            SkijaPatchMod.LOGGER.info("下载完成，正在提取原生库... / Download complete, extracting native library...");

            extractNativeLib(tempJar, platform, targetPath);

        } finally {
            cleanupTempDir(tempDir);
        }
    }

    private static void extractNativeLib(Path jarPath, Platform platform, Path targetPath) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            String nativeName = platform.getNativeLibraryName();
            String platformSuffix = platform.getArtifactSuffix();

            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (!entry.isDirectory() &&
                    name.contains("native") &&
                    name.contains(platformSuffix) &&
                    name.endsWith(nativeName)) {

                    SkijaPatchMod.LOGGER.info("正在提取: {} / Extracting: {}", name, name);

                    Files.createDirectories(targetPath.getParent());

                    try (InputStream in = jar.getInputStream(entry)) {
                        Files.copy(in, targetPath);
                    }
                    return;
                }
            }

            throw new IOException("下载的 JAR 中未找到该平台的原生库: " + platform + " / Native library not found in downloaded JAR for platform: " + platform);
        }
    }

    private static void loadLibrary(Path libPath) {
        try {
            String absolutePath = libPath.toAbsolutePath().toString();
            SkijaPatchMod.LOGGER.info("正在加载原生库: {} / Loading native library from: {}", absolutePath, absolutePath);
            System.load(absolutePath);
            nativeLoaded = true;
            SkijaPatchMod.LOGGER.info("原生库加载成功 / Successfully loaded skija native library");
        } catch (UnsatisfiedLinkError e) {
            SkijaPatchMod.LOGGER.error("原生库加载失败: {} / Failed to load native library: {}", e.getMessage(), e.getMessage());
            throw e;
        }
    }

    private static void cleanupTempDir(Path tempDir) {
        try {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
        } catch (IOException ignored) {}
    }

    public static String getCurrentPlatform() {
        return PlatformDetector.detectPlatform().name();
    }
}
