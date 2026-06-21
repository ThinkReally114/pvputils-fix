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
    private static final Object LOAD_LOCK = new Object();

    public static boolean isNativeLoaded() {
        return nativeLoaded;
    }

    public static void preload() throws Exception {
        if (nativeLoaded) {
            SkijaPatchMod.LOGGER.info("Native library already loaded, skipping");
            return;
        }

        synchronized (LOAD_LOCK) {
            if (nativeLoaded) {
                return;
            }

            Platform platform = PlatformDetector.detectPlatform();
            if (platform == PlatformDetector.Platform.UNKNOWN) {
                SkijaPatchMod.LOGGER.warn("Unsupported platform detected; skipping native library preload");
                return;
            }

            String platformName = platform.getArtifactSuffix();
            String nativeLibraryName = platform.getNativeLibraryName();

            SkijaPatchMod.LOGGER.info("Detected platform: {} (looking for {})", platform, platformName);

            String resourcePath = "/skija/native/" + platformName + "/" + nativeLibraryName;

            SkijaPatchMod.LOGGER.info("Looking for native library: {}", resourcePath);

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
                SkijaPatchMod.LOGGER.warn("This patch mod may not have the correct skija dependencies built in.");
                return;
            }

            Path tempDir = Files.createTempDirectory("skija-patch");
            Path extractedLib = tempDir.resolve(nativeLibraryName);

            try (InputStream stream = libStream) {
                Files.copy(stream, extractedLib);

                extractedLib.toFile().setReadable(true);
                extractedLib.toFile().setWritable(true, true);

                String absolutePath = extractedLib.toAbsolutePath().toString();
                SkijaPatchMod.LOGGER.info("Loading native library from: {}", absolutePath);

                System.load(absolutePath);

                SkijaPatchMod.LOGGER.info("Successfully loaded skija native library for {}", platform);
                nativeLoaded = true;
            }
        }
    }

    private static InputStream findInSkijaJar(Platform platform, String nativeLibraryName) {
        String classpath = System.getProperty("java.class.path");
        String[] paths = classpath.split(File.pathSeparator);

        for (String path : paths) {
            if (path.contains("skija")) {
                JarFile jar = null;
                try {
                    if (path.endsWith(".jar")) {
                        jar = new JarFile(path);
                        Enumeration<JarEntry> entries = jar.entries();

                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();

                            if (name.contains("native") &&
                                name.contains(platform.getOs()) &&
                                name.contains(nativeLibraryName) &&
                                !entry.isDirectory()) {

                                SkijaPatchMod.LOGGER.info("Found native library in jar: {}", name);
                                return new BufferedJarInputStream(jar, jar.getInputStream(entry));
                            }
                        }
                        jar.close();
                        jar = null;
                    }
                } catch (Exception e) {
                    SkijaPatchMod.LOGGER.debug("Error reading jar {}: {}", path, e.getMessage());
                    if (jar != null) {
                        try { jar.close(); } catch (IOException ignored) {}
                    }
                }
            }
        }
        return null;
    }

    private static final class BufferedJarInputStream extends FilterInputStream {
        private final JarFile jar;

        BufferedJarInputStream(JarFile jar, InputStream in) {
            super(in);
            this.jar = jar;
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                jar.close();
            }
        }
    }

    public static String getCurrentPlatform() {
        return PlatformDetector.detectPlatform().name();
    }
}
