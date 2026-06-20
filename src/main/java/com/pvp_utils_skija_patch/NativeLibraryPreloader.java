package com.pvp_utils_skija_patch;

import com.pvp_utils_skija_patch.util.PlatformDetector;
import com.pvp_utils_skija_patch.util.PlatformDetector.Platform;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class NativeLibraryPreloader {

    private static final AtomicBoolean nativeLoaded = new AtomicBoolean(false);

    public static boolean isNativeLoaded() {
        return nativeLoaded.get();
    }

    public static void preload() throws Exception {
        if (!nativeLoaded.compareAndSet(false, true)) {
            SkijaPatchMod.LOGGER.info("Native library already loaded, skipping");
            return;
        }

        Platform platform = PlatformDetector.detectPlatform();
        String platformName = platform.getArtifactSuffix();
        String nativeLibraryName = platform.getNativeLibraryName();

        SkijaPatchMod.LOGGER.info("Detected platform: {} (looking for {})", platform, platformName);

        String resourcePath = "/skija/native/" + platformName + "/" + nativeLibraryName;

        SkijaPatchMod.LOGGER.info("Looking for native library: {}", resourcePath);

        Path extractedLib = null;
        try (InputStream libStream = openLibraryStream(platform, platformName, nativeLibraryName)) {
            if (libStream == null) {
                SkijaPatchMod.LOGGER.warn("Could not find native library for platform {}", platform);
                SkijaPatchMod.LOGGER.warn("This patch mod may not have the correct skija dependencies built in.");
                nativeLoaded.set(false);
                return;
            }

            Path tempDir = Files.createTempDirectory("skija-patch");
            tempDir.toFile().deleteOnExit();
            extractedLib = tempDir.resolve(nativeLibraryName);
            extractedLib.toFile().deleteOnExit();

            Files.copy(libStream, extractedLib);

            extractedLib.toFile().setReadable(true);
            extractedLib.toFile().setWritable(true, true);

            String absolutePath = extractedLib.toAbsolutePath().toString();
            SkijaPatchMod.LOGGER.info("Loading native library from: {}", absolutePath);

            System.load(absolutePath);

            SkijaPatchMod.LOGGER.info("Successfully loaded skija native library for {}", platform);
        } catch (Exception e) {
            nativeLoaded.set(false);
            throw e;
        }
    }

    private static InputStream openLibraryStream(Platform platform, String platformName, String nativeLibraryName) {
        String primary = "/skija/native/" + platformName + "/" + nativeLibraryName;
        InputStream libStream = NativeLibraryPreloader.class.getResourceAsStream(primary);
        if (libStream != null) {
            return libStream;
        }

        String[] alternativePaths = {
            "/native/" + platformName + "/" + nativeLibraryName,
            "/META-INF/resources/native/" + platformName + "/" + nativeLibraryName,
            "/io/github/humbleui/skija/native/" + platformName + "/" + nativeLibraryName
        };

        for (String altPath : alternativePaths) {
            libStream = NativeLibraryPreloader.class.getResourceAsStream(altPath);
            if (libStream != null) {
                return libStream;
            }
        }

        return findInSkijaJar(platform, nativeLibraryName);
    }

    static InputStream findInSkijaJar(Platform platform, String nativeLibraryName) {
        String classpath = System.getProperty("java.class.path");
        String[] paths = classpath.split(File.pathSeparator);

        for (String path : paths) {
            if (path.contains("skija")) {
                if (!path.endsWith(".jar")) {
                    continue;
                }
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
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            try (InputStream entryStream = jar.getInputStream(entry)) {
                                byte[] buf = new byte[8192];
                                int n;
                                while ((n = entryStream.read(buf)) > 0) {
                                    baos.write(buf, 0, n);
                                }
                            }
                            return new ByteArrayInputStream(baos.toByteArray());
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
