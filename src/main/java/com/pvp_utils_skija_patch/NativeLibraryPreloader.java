package com.pvp_utils_skija_patch;

import com.pvp_utils_skija_patch.util.PlatformDetector;
import com.pvp_utils_skija_patch.util.PlatformDetector.Platform;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.atomic.AtomicBoolean;

public class NativeLibraryPreloader {

    private static final AtomicBoolean nativeLoaded = new AtomicBoolean(false);

    public static boolean isNativeLoaded() {
        return nativeLoaded.get();
    }

    public static void preload() throws Exception {
        if (!nativeLoaded.compareAndSet(false, true)) {
            return;
        }

        Platform platform = PlatformDetector.detectPlatform();
        if (platform == Platform.UNKNOWN) {
            nativeLoaded.set(false);
            SkijaPatchMod.LOGGER.warn("不支持的平台，无法预加载原生库 / Unsupported platform, cannot preload native library");
            return;
        }

        String libName = platform.getNativeLibraryName();
        String resourcePath = "/natives/" + platform.getArtifactSuffix() + "/" + libName;

        InputStream libStream = NativeLibraryPreloader.class.getResourceAsStream(resourcePath);
        if (libStream == null) {
            nativeLoaded.set(false);
            SkijaPatchMod.LOGGER.error("未找到打包的原生库资源: {} / Bundled native library not found: {}", resourcePath, resourcePath);
            return;
        }

        Path tempDir = Files.createTempDirectory("skija-native-");
        Path nativeLib = tempDir.resolve(libName);
        try {
            Files.copy(libStream, nativeLib, StandardCopyOption.REPLACE_EXISTING);
            libStream.close();

            SkijaPatchMod.LOGGER.info("已从资源提取原生库: {} / Extracted native library from resource: {}", resourcePath, nativeLib);
            System.load(nativeLib.toAbsolutePath().toString());
            SkijaPatchMod.LOGGER.info("原生库加载成功 / Native library loaded via System.load()");
        } catch (Exception e) {
            try { Files.deleteIfExists(nativeLib); } catch (Exception ignored) {}
            try { Files.deleteIfExists(tempDir); } catch (Exception ignored) {}
            throw e;
        }

        initSkijaNatively();
        SkijaPatchMod.LOGGER.info("Skia native 初始化完成 / Skia native initialization complete");
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

    public static String getCurrentPlatform() {
        return PlatformDetector.detectPlatform().name();
    }
}
