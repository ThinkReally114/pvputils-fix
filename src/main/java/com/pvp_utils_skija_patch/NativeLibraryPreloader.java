package com.pvp_utils_skija_patch;

import com.pvp_utils_skija_patch.util.PlatformDetector;
import com.pvp_utils_skija_patch.util.PlatformDetector.Platform;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class NativeLibraryPreloader {

    private static String skijaVersion;
    private static final String[] MAVEN_MIRRORS = {
            "https://maven.aliyun.com/repository/central",
            "https://mirrors.tencent.com/nexus/repository/maven-public",
            "https://maven.google.com",
            "https://repo1.maven.org/maven2"
    };
    private static final Duration DOWNLOAD_TIMEOUT = Duration.ofSeconds(60);

    private static final AtomicBoolean nativeLoaded = new AtomicBoolean(false);
    private static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(DOWNLOAD_TIMEOUT)
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public static boolean isNativeLoaded() {
        return nativeLoaded.get();
    }

    public static void preload() throws Exception {
        if (!nativeLoaded.compareAndSet(false, true)) {
            return;
        }

        String version = getSkijaVersion();
        if (version == null) {
            nativeLoaded.set(false);
            SkijaPatchMod.LOGGER.error("无法读取 Skija 版本 / Failed to read Skija version from resource");
            return;
        }

        Platform platform = PlatformDetector.detectPlatform();
        if (platform == Platform.UNKNOWN) {
            nativeLoaded.set(false);
            SkijaPatchMod.LOGGER.warn("不支持的平台，无法预加载原生库 / Unsupported platform, cannot preload native library");
            return;
        }

        Path cacheDir = getCacheDir(version);
        Path cachedLib = cacheDir.resolve(platform.getArtifactSuffix()).resolve(platform.getNativeLibraryName());

        if (Files.exists(cachedLib)) {
            SkijaPatchMod.LOGGER.info("找到已缓存的原生库: {} / Found cached native library: {}", cachedLib, cachedLib);
        } else {
            SkijaPatchMod.LOGGER.info("未找到缓存，正在为平台 {} 下载原生库... / No cached library found, downloading for platform: {}", platform, platform);
            downloadAndExtract(platform, cachedLib, version);
        }

        System.load(cachedLib.toAbsolutePath().toString());
        SkijaPatchMod.LOGGER.info("原生库加载成功 / Native library loaded via System.load()");

        initSkijaNatively();
        SkijaPatchMod.LOGGER.info("Skia native 初始化完成 / Skia native initialization complete");
    }

    private static String getSkijaVersion() {
        if (skijaVersion != null) {
            return skijaVersion;
        }
        try (InputStream in = NativeLibraryPreloader.class.getResourceAsStream("/skija-version.properties")) {
            if (in == null) {
                SkijaPatchMod.LOGGER.warn("skija-version.properties 未找到，使用默认版本 / skija-version.properties not found, using default version");
                skijaVersion = "0.143.16";
                return skijaVersion;
            }
            Properties props = new Properties();
            props.load(in);
            skijaVersion = props.getProperty("skija.version");
            return skijaVersion;
        } catch (IOException e) {
            SkijaPatchMod.LOGGER.error("读取 Skija 版本失败 / Failed to read Skija version", e);
            skijaVersion = "0.143.16";
            return skijaVersion;
        }
    }

    private static void initSkijaNatively() throws Exception {
        Class<?> libraryClass = Class.forName("io.github.humbleui.skija.impl.Library");

        for (Field f : libraryClass.getDeclaredFields()) {
            if (f.getType() == boolean.class && java.lang.reflect.Modifier.isStatic(f.getModifiers())) {
                f.setAccessible(true);
                f.setBoolean(null, true);
            }
        }

        for (Method m : libraryClass.getDeclaredMethods()) {
            if (m.getParameterCount() == 0 && java.lang.reflect.Modifier.isNative(m.getModifiers())) {
                m.setAccessible(true);
                m.invoke(null);
                break;
            }
        }
    }

    private static Path getCacheDir(String version) {
        String cacheBase = System.getProperty("user.home", ".");
        if (cacheBase.equals(".") || cacheBase.isEmpty()) {
            cacheBase = System.getProperty("java.io.tmpdir", "/tmp");
        }
        return Path.of(cacheBase, ".skija-natives", version);
    }

    private static void downloadAndExtract(Platform platform, Path targetPath, String version) throws Exception {
        String artifactName = "skija-" + platform.getArtifactSuffix();
        String artifactPath = String.format("/io/github/humbleui/%s/%s/%s-%s.jar",
                artifactName, version, artifactName, version);

        Path tempDir = Files.createTempDirectory("skija-download-");
        try {
            Path tempJar = tempDir.resolve(artifactName + ".jar");

            Properties checksums = loadChecksums();
            String expectedChecksum = checksums != null
                    ? checksums.getProperty(platform.getArtifactSuffix())
                    : null;

            IOException lastException = null;
            for (String mirror : MAVEN_MIRRORS) {
                String jarUrl = mirror + artifactPath;
                SkijaPatchMod.LOGGER.info("正在下载: {} / Downloading: {}", jarUrl, jarUrl);

                try {
                    HttpRequest request = HttpRequest.newBuilder()
                            .uri(URI.create(jarUrl))
                            .timeout(DOWNLOAD_TIMEOUT)
                            .header("User-Agent", "PVPUtils-Skija-Patch/" + version)
                            .header("Accept", "application/octet-stream,*/*")
                            .GET()
                            .build();

                    HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

                    if (response.statusCode() == 200) {
                        try (InputStream in = response.body()) {
                            Files.copy(in, tempJar, StandardCopyOption.REPLACE_EXISTING);
                        }

                        if (expectedChecksum != null) {
                            String actualChecksum = computeSha256(tempJar);
                            if (!expectedChecksum.equals(actualChecksum)) {
                                SkijaPatchMod.LOGGER.warn("校验和不匹配 ({}), 期望: {}, 实际: {} / Checksum mismatch, expected: {}, actual: {}",
                                        mirror, expectedChecksum, actualChecksum, expectedChecksum, actualChecksum);
                                Files.deleteIfExists(tempJar);
                                lastException = new IOException("Checksum mismatch from " + mirror);
                                continue;
                            }
                            SkijaPatchMod.LOGGER.info("校验和验证通过 / Checksum verified");
                        }

                        SkijaPatchMod.LOGGER.info("下载完成，正在提取原生库... / Download complete, extracting native library...");
                        extractNativeLib(tempJar, platform, targetPath);
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
        } finally {
            cleanupTempDir(tempDir);
        }
    }

    private static Properties loadChecksums() {
        try (InputStream in = NativeLibraryPreloader.class.getResourceAsStream("/checksums.properties")) {
            if (in == null) {
                SkijaPatchMod.LOGGER.warn("checksums.properties 未找到，跳过校验和验证 / checksums.properties not found, skipping checksum verification");
                return null;
            }
            Properties props = new Properties();
            props.load(in);
            return props;
        } catch (IOException e) {
            SkijaPatchMod.LOGGER.warn("加载校验和失败 / Failed to load checksums", e);
            return null;
        }
    }

    private static String computeSha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static void extractNativeLib(Path jarPath, Platform platform, Path targetPath) throws IOException {
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            String expectedName = platform.getNativeLibraryName();

            var entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();

                if (!entry.isDirectory() && name.endsWith(expectedName)) {
                    SkijaPatchMod.LOGGER.info("正在提取: {} / Extracting: {}", name, name);
                    Files.createDirectories(targetPath.getParent());
                    try (InputStream in = jar.getInputStream(entry)) {
                        Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                    }
                    return;
                }
            }

            SkijaPatchMod.LOGGER.warn("JAR 内容: / JAR contents:");
            var allEntries = jar.entries();
            while (allEntries.hasMoreElements()) {
                JarEntry e = allEntries.nextElement();
                if (!e.isDirectory()) {
                    SkijaPatchMod.LOGGER.warn("  - {}", e.getName());
                }
            }

            throw new IOException("下载的 JAR 中未找到该平台的原生库: " + platform + " / Native library not found in downloaded JAR for platform: " + platform);
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
