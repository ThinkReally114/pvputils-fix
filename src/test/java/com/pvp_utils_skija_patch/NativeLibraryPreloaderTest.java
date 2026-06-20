package com.pvp_utils_skija_patch;

import com.pvp_utils_skija_patch.util.PlatformDetector.Platform;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.*;

public class NativeLibraryPreloaderTest {

    @Test
    void findInSkijaJar_findsNativeEntry_andDoesNotLeakJar(@TempDir Path tempDir) throws Exception {
        Path fakeJar = tempDir.resolve("skija-fake.jar");
        byte[] payload = "nativelib-bytes".getBytes();
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(fakeJar.toFile()))) {
            JarEntry entry = new JarEntry("native/linux/x64/skija.so");
            jos.putNextEntry(entry);
            jos.write(payload);
            jos.closeEntry();

            JarEntry other = new JarEntry("META-INF/MANIFEST.MF");
            jos.putNextEntry(other);
            jos.write("Manifest-Version: 1.0\n".getBytes());
            jos.closeEntry();
        }

        String prevCp = System.getProperty("java.class.path");
        try {
            System.setProperty("java.class.path", fakeJar.toAbsolutePath() + File.pathSeparator + "something-else");

            InputStream in = NativeLibraryPreloader.findInSkijaJar(Platform.LINUX_X64, "skija.so");

            assertNotNull(in, "Expected to locate the native library entry");
            byte[] read = in.readAllBytes();
            in.close();
            assertArrayEquals(payload, read);
        } finally {
            System.setProperty("java.class.path", prevCp);
        }
    }

    @Test
    void findInSkijaJar_missingEntry_returnsNull(@TempDir Path tempDir) throws Exception {
        Path fakeJar = tempDir.resolve("skija-empty.jar");
        try (JarOutputStream jos = new JarOutputStream(new FileOutputStream(fakeJar.toFile()))) {
            JarEntry entry = new JarEntry("something/else.txt");
            jos.putNextEntry(entry);
            jos.write("hi".getBytes());
            jos.closeEntry();
        }

        String prevCp = System.getProperty("java.class.path");
        try {
            System.setProperty("java.class.path", fakeJar.toAbsolutePath().toString());
            InputStream in = NativeLibraryPreloader.findInSkijaJar(Platform.LINUX_X64, "skija.so");
            assertNull(in);
        } finally {
            System.setProperty("java.class.path", prevCp);
        }
    }
}
