package com.pvp_utils_skija_patch.util;

import java.util.Locale;

public class PlatformDetector {

    private static Platform cachedPlatform;

    public enum Platform {
        WINDOWS_X64("windows", "x64", "dll"),
        WINDOWS_ARM64("windows", "arm64", "dll"),
        LINUX_X64("linux", "x64", "so"),
        LINUX_ARM64("linux", "arm64", "so"),


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
        if (cachedPlatform != null) {
            return cachedPlatform;
        }

        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);

        boolean isWindows = os.contains("win");
        boolean isLinux = os.contains("linux") || os.contains("unix");

        boolean isArm64 = arch.contains("aarch64") || arch.contains("arm64");

        if (isWindows) {
            cachedPlatform = isArm64 ? Platform.WINDOWS_ARM64 : Platform.WINDOWS_X64;
        } else if (isLinux) {
            cachedPlatform = isArm64 ? Platform.LINUX_ARM64 : Platform.LINUX_X64;
        } else {
            cachedPlatform = Platform.UNKNOWN;
        }

        return cachedPlatform;
    }

    public static boolean isSupportedPlatform() {
        Platform platform = detectPlatform();
        return platform != Platform.UNKNOWN;
    }
}
