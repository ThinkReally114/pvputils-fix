package com.pvp_utils_skija_patch.util;

import com.pvp_utils_skija_patch.util.PlatformDetector.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.*;

class PlatformDetectorTest {

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void detectPlatform_windows_returnsWindowsVariant() {
        Platform p = PlatformDetector.detectPlatform();
        assertTrue(p == Platform.WINDOWS_X64 || p == Platform.WINDOWS_ARM64);
        assertTrue(PlatformDetector.isSupportedPlatform());
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void detectPlatform_linux_returnsLinuxVariant() {
        Platform p = PlatformDetector.detectPlatform();
        assertTrue(p == Platform.LINUX_X64 || p == Platform.LINUX_ARM64);
        assertTrue(PlatformDetector.isSupportedPlatform());
    }

    @Test
    void detectPlatform_unknownOs_returnsUnknown() throws Exception {
        Object holder = setProperty("os.name", "FreeBSD");
        try {
            Platform p = PlatformDetector.detectPlatform();
            assertEquals(Platform.UNKNOWN, p);
            assertFalse(PlatformDetector.isSupportedPlatform());
        } finally {
            restoreProperty(holder);
        }
    }

    @Test
    void platform_values_haveNonNullStrings() {
        for (Platform p : Platform.values()) {
            assertNotNull(p.getOs());
            assertNotNull(p.getArch());
            assertNotNull(p.getExtension());
            assertNotNull(p.getArtifactSuffix());
            assertNotNull(p.getNativeLibraryName());
            assertTrue(p.getNativeLibraryName().startsWith("skija."));
        }
    }

    @Test
    void nativeLibraryName_hasCorrectExtension() {
        assertEquals("skija.dll", Platform.WINDOWS_X64.getNativeLibraryName());
        assertEquals("skija.dll", Platform.WINDOWS_ARM64.getNativeLibraryName());
        assertEquals("skija.so", Platform.LINUX_X64.getNativeLibraryName());
        assertEquals("skija.so", Platform.LINUX_ARM64.getNativeLibraryName());
        assertEquals("skija.so", Platform.UNKNOWN.getNativeLibraryName());
    }

    @Test
    void artifactSuffix_combinesOsAndArch() {
        assertEquals("windows-x64", Platform.WINDOWS_X64.getArtifactSuffix());
        assertEquals("linux-arm64", Platform.LINUX_ARM64.getArtifactSuffix());
    }

    private static Object setProperty(String key, String value) {
        String prev = System.getProperty(key);
        System.setProperty(key, value);
        return new Object[]{key, prev};
    }

    @SuppressWarnings("unchecked")
    private static void restoreProperty(Object holder) {
        Object[] arr = (Object[]) holder;
        String key = (String) arr[0];
        String prev = (String) arr[1];
        if (prev == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, prev);
        }
    }
}
