package com.pvp_utils_skija_patch.util;

import com.pvp_utils_skija_patch.util.PlatformDetector.Platform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

public class PlatformDetectorTest {

    @ParameterizedTest
    @CsvSource({
        "Windows 10,     amd64,   WINDOWS_X64,   windows-x64,   skija.dll",
        "Windows 11,     aarch64, WINDOWS_ARM64, windows-arm64, skija.dll",
        "Linux,          amd64,   LINUX_X64,     linux-x64,     skija.so",
        "Linux,          aarch64, LINUX_ARM64,   linux-arm64,   skija.so",
        "Mac OS X,       x86_64,  MACOS_X64,     macos-x64,     skija.dylib",
        "Mac OS X,       aarch64, MACOS_ARM64,   macos-arm64,   skija.dylib",
        "Darwin,         arm64,   MACOS_ARM64,   macos-arm64,   skija.dylib"
    })
    void detectPlatform_returnsExpectedValues(String osName, String arch, String expectedName,
                                              String expectedSuffix, String expectedLibName) {
        String prevOs = System.getProperty("os.name");
        String prevArch = System.getProperty("os.arch");
        try {
            System.setProperty("os.name", osName);
            System.setProperty("os.arch", arch);

            Platform platform = PlatformDetector.detectPlatform();
            assertEquals(Platform.valueOf(expectedName), platform,
                "Unexpected platform for os=[" + osName + "] arch=[" + arch + "]");
            assertEquals(expectedSuffix, platform.getArtifactSuffix());
            assertEquals(expectedLibName, platform.getNativeLibraryName());
            assertTrue(PlatformDetector.isSupportedPlatform());
        } finally {
            System.setProperty("os.name", prevOs);
            System.setProperty("os.arch", prevArch);
        }
    }

    @Test
    void detectPlatform_unknownOs_returnsUnknown() {
        String prevOs = System.getProperty("os.name");
        String prevArch = System.getProperty("os.arch");
        try {
            System.setProperty("os.name", "FreeBSD");
            System.setProperty("os.arch", "amd64");

            Platform platform = PlatformDetector.detectPlatform();
            assertEquals(Platform.UNKNOWN, platform);
            assertFalse(PlatformDetector.isSupportedPlatform());
        } finally {
            System.setProperty("os.name", prevOs);
            System.setProperty("os.arch", prevArch);
        }
    }
}
