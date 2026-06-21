package com.pvp_utils_skija_patch;

import com.pvp_utils_skija_patch.util.PlatformDetector;
import com.pvp_utils_skija_patch.util.PlatformDetector.Platform;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class NativeLibraryPreloader {

    private static boolean nativeLoaded = false;
    private static Path tempDir;

    public static boolean isNativeLoaded() {
        return nativeLoaded;
    }

    public static void preload() throws Exception {
        if (nativeLoaded) {
            return;
        }

        Platform platform = PlatformDetector.detectPlatform();
        if (platform == Platform.UNKNOWN) {
            SkijaPatchMod.LOGGER.warn("Unsupported platform, cannot preload native library");
            return;
        }

        String platformName = platform.getArtifactSuffix();
        String nativeLibraryName = platform.getNativeLibraryName();

        SkijaPatchMod.LOGGER.info("Detected platform: {} (looking for {})", platform, platformName);

        String resourcePath = "/skija/native/" + platformName + "/" + nativeLibraryName;

        InputStream libStream = NativeLibraryPreloader.class.getResourceAsStream(resourcePath);

        if (libStream == null) {
            String[] alternativePaths = {
                "/native/" + platformName + "/" + nativeLibraryName,
                "/META-INF/resources/native/" + platformName + "/" + nativeLibraryName,
                "/io/github/humbleui/skija/native/" + platformName + "/" + nativeLibraryName
            };

            for (String altPath : alternativePaths) {
                libStream = NativeLibraryPreloader.class.getResourceAsStream(altPath);
                if (libStream != null) {
                    resourcePath = altPath;
                    break;
                }
            }
        }

        if (libStream == null) {
            libStream = findInSkijaJar(platform, nativeLibraryName);
        }

        if (libStream == null) {
            SkijaPatchMod.LOGGER.warn("Could not find native library for platform {}", platform);
            return;
        }

        tempDir = Files.createTempDirectory("skija-patch");
        Path extractedLib = tempDir.resolve(nativeLibraryName);

        try (InputStream in = libStream) {
            Files.copy(in, extractedLib);

            extractedLib.toFile().setReadable(true);
            extractedLib.toFile().setWritable(true, true);

            String absolutePath = extractedLib.toAbsolutePath().toString();
            SkijaPatchMod.LOGGER.info("Loading native library from: {}", absolutePath);

            System.load(absolutePath);

            SkijaPatchMod.LOGGER.info("Successfully loaded skija native library for {}", platform);
            nativeLoaded = true;
        } finally {
            cleanupTempDir();
        }
    }

    private static void cleanupTempDir() {
        if (tempDir != null) {
            try {
                Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); } catch (IOException ignored) {}
                    });
            } catch (IOException ignored) {}
            tempDir = null;
        }
    }

    private static InputStream findInSkijaJar(Platform platform, String nativeLibraryName) {
        String classpath = System.getProperty("java.class.path");
        String[] paths = classpath.split(File.pathSeparator);

        for (String path : paths) {
            if (path.contains("skija") && path.endsWith(".jar")) {
                try (JarFile jar = new JarFile(path)) {
                    Enumeration<JarEntry> entries = jar.entries();

                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();

                        if (name.contains("native") &&
                            name.contains(platform.getOs()) &&
                            name.contains(nativeLibraryName) &&
                            !entry.isDirectory()) {

                            SkijaPatchMod.LOGGER.info("Found native library in jar: {}", name);
                            byte[] data = jar.getInputStream(entry).readAllBytes();
                            return new ByteArrayInputStream(data);
                        }
                    }
                } catch (Exception e) {
                    SkijaPatchMod.LOGGER.debug("Error reading jar {}: {}", path, e.getMessage());
                }
            }
        }
        return null;
    }

    public static String getCurrentPlatform() {
        return PlatformDetector.detectPlatform().name();
    }
}
