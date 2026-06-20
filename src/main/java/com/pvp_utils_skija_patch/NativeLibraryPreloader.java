package com.pvp_utils_skija_patch;

import com.pvp_utils_skija_patch.util.PlatformDetector;
import com.pvp_utils_skija_patch.util.PlatformDetector.Platform;

import java.io.*;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class NativeLibraryPreloader {

    private static final String SKIJA_VERSION = "0.143.16";
    private static volatile boolean nativeLoaded = false;
    private static final Object LOCK = new Object();

    public static boolean isNativeLoaded() {
        return nativeLoaded;
    }

    public static void preload() throws Exception {
        if (nativeLoaded) {
            SkijaPatchMod.LOGGER.info("Native library already loaded, skipping");
            return;
        }

        synchronized (LOCK) {
            if (nativeLoaded) {
                return;
            }

            Platform platform = PlatformDetector.detectPlatform();
            String platformName = platform.getArtifactSuffix();
            String nativeLibraryName = platform.getNativeLibraryName();

            SkijaPatchMod.LOGGER.info("Detected platform: {} (looking for {})", platform, platformName);

            // Find and extract the native library from the jar
            String resourcePath = "/skija/native/" + platformName + "/" + nativeLibraryName;

            SkijaPatchMod.LOGGER.info("Looking for native library: {}", resourcePath);

            // Try to find the native library in classpath
            InputStream libStream = NativeLibraryPreloader.class.getResourceAsStream(resourcePath);

            if (libStream == null) {
                // Try alternative paths (skija might structure files differently)
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
                // Try to find in any skija jar on classpath
                libStream = findInSkijaJar(platform, nativeLibraryName);
            }

            if (libStream == null) {
                SkijaPatchMod.LOGGER.warn("Could not find native library for platform {}", platform);
                SkijaPatchMod.LOGGER.warn("This patch mod may not have the correct skija dependencies built in.");
                return;
            }

            // Extract to temp directory
            Path tempDir = Files.createTempDirectory("skija-patch");
            Path extractedLib = tempDir.resolve(nativeLibraryName);

            try (InputStream stream = libStream) {
                Files.copy(stream, extractedLib);

                // Make it executable on Unix-like systems
                extractedLib.toFile().setReadable(true);
                extractedLib.toFile().setWritable(true, true);

                // Load the library using absolute path
                String absolutePath = extractedLib.toAbsolutePath().toString();
                SkijaPatchMod.LOGGER.info("Loading native library from: {}", absolutePath);

                // Use System.load() with absolute path
                System.load(absolutePath);

                SkijaPatchMod.LOGGER.info("Successfully loaded skija native library for {}", platform);
                nativeLoaded = true;
            }
        }
    }

    private static InputStream findInSkijaJar(Platform platform, String nativeLibraryName) {
        // Get the classpath
        String classpath = System.getProperty("java.class.path");
        String[] paths = classpath.split(File.pathSeparator);

        for (String path : paths) {
            if (path.contains("skija")) {
                try {
                    if (path.endsWith(".jar")) {
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
                                    return jar.getInputStream(entry);
                                }
                            }
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
