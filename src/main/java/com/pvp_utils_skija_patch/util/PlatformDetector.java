package com.pvp_utils_skija_patch.util;

import java.util.Locale;

public class PlatformDetector {

    public enum Platform {
        WINDOWS_X64("windows", "x64", "dll"),
        WINDOWS_ARM64("windows", "arm64", "dll"),
        LINUX_X64("linux", "x64", "so"),
        LINUX_ARM64("linux", "arm64", "so"),
        MACOS_X64("macos", "x64", "dylib"),
        MACOS_ARM64("macos", "arm64", "dylib"),
        ANDROID_X64("android", "x64", "so"),
        ANDROID_ARM64("android", "arm64", "so"),
        UNKNOWN("unknown", "unknown", "so");

        private final String os;
        private final String arch;
        private final String extension;

        Platform(String os, String arch, String extension) {
            this.os = os;
            this.arch = arch;
            this.extension = extension;
        }

        public String getOs() { return os; }
        public String getArch() { return arch; }
        public String getExtension() { return extension; }

        public String getArtifactSuffix() {
            return os + "-" + arch;
        }

        public String getNativeLibraryName() {
            return "skija." + extension;
        }
    }

    public static Platform detectPlatform() {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);

        // Check for Android (PojavLauncher)
        if (isAndroid()) {
            if (arch.contains("aarch64") || arch.contains("arm64")) {
                return Platform.ANDROID_ARM64;
            } else {
                return Platform.ANDROID_X64;
            }
        }

        // Detect OS
        boolean isWindows = os.contains("win");
        boolean isMac = os.contains("mac") || os.contains("darwin");
        boolean isLinux = os.contains("linux") || os.contains("unix");

        // Detect architecture
        boolean isArm64 = arch.contains("aarch64") || arch.contains("arm64");
        boolean isX64 = arch.contains("amd64") || arch.contains("x86_64") || arch.contains("x64");

        if (isWindows) {
            return isArm64 ? Platform.WINDOWS_ARM64 : Platform.WINDOWS_X64;
        } else if (isMac) {
            return isArm64 ? Platform.MACOS_ARM64 : Platform.MACOS_X64;
        } else if (isLinux) {
            return isArm64 ? Platform.LINUX_ARM64 : Platform.LINUX_X64;
        }

        return Platform.UNKNOWN;
    }

    private static boolean isAndroid() {
        // Check for Dalvik VM (Android/PojavLauncher)
        String vmName = System.getProperty("java.vm.name", "").toLowerCase(Locale.ROOT);
        if (vmName.contains("dalvik")) {
            return true;
        }

        // Check for Android vendor
        String vendor = System.getProperty("java.vendor", "").toLowerCase(Locale.ROOT);
        if (vendor.contains("android")) {
            return true;
        }

        // Check for Android system files
        try {
            if (new java.io.File("/system/build.prop").exists()) {
                return true;
            }
        } catch (Exception ignored) {}

        return false;
    }

    public static boolean isSupportedPlatform() {
        Platform platform = detectPlatform();
        return platform != Platform.UNKNOWN;
    }
}
