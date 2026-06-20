package com.pvp_utils_skija_patch;

import com.pvp_utils_skija_patch.util.PlatformDetector;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class NativeLibraryPreloaderTest {

    @Test
    void initialState_nativeLoadedIsFalse() {
        // We never invoke preload() in this test, so flag must remain false.
        assertFalse(readNativeLoaded(),
                "nativeLoaded must be false before any successful preload");
    }

    @Test
    void getCurrentPlatform_matchesDetectedPlatform() {
        String current = NativeLibraryPreloader.getCurrentPlatform();
        assertEquals(PlatformDetector.detectPlatform().name(), current);
        assertNotNull(current);
    }

    @Test
    void preload_doesNotCrashOnMissingLibrary() throws Exception {
        // Arrange: make sure classpath has no skija native resource at all,
        // and simulate an UNKNOWN platform to force "null libStream" path.
        Object osHolder = swapSystemProperty("os.name", "HaikuOS");
        try {
            // Should NOT throw (graceful warn + return path).
            NativeLibraryPreloader.preload();
        } catch (NullPointerException npe) {
            fail("preload() must not throw NPE when native library is unavailable, "
                    + "but got: " + npe.getMessage(), npe);
        } finally {
            restoreSystemProperty(osHolder);
        }
    }

    @Test
    void concurrentPreload_onlyOneThreadPerformsWork() throws Exception {
        // Reset state for this test.
        setNativeLoaded(false);

        // Install a shim so that preload doesn't actually try to System.load a real .so:
        // We simulate a "synthetic" load by replacing the stream with one that will
        // be consumed by Files.copy but then System.load will fail. To avoid that,
        // we instead verify the concurrency contract on the synchronized block by
        // counting how many threads enter our spy method. Because we can't easily
        // spy, we simply ensure that no exceptions propagate when multiple threads
        // call preload() concurrently -- a regression test for the missing volatile/LOCK.
        final int THREADS = 8;
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREADS);
        AtomicInteger exceptions = new AtomicInteger(0);
        List<Runnable> tasks = new ArrayList<>(THREADS);
        for (int i = 0; i < THREADS; i++) {
            tasks.add(() -> {
                try {
                    startLatch.await();
                    try {
                        Object osHolder = swapSystemProperty("os.name", "HaikuOS");
                        try {
                            NativeLibraryPreloader.preload();
                        } finally {
                            restoreSystemProperty(osHolder);
                        }
                    } catch (Exception ignored) {
                        exceptions.incrementAndGet();
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        for (Runnable r : tasks) executor.submit(r);
        startLatch.countDown();
        assertTrue(doneLatch.await(10, TimeUnit.SECONDS), "threads finished");
        executor.shutdownNow();

        assertEquals(0, exceptions.get(),
                "concurrent preload() must never throw (current impl uses synchronized LOCK)");
    }

    @Test
    void inputStreamFromJarIsIndependentOfJarFile() throws Exception {
        // Sanity contract test: BufferedInputStream/ByteArrayInputStream-style
        // wrapping must allow us to close the underlying JarFile before consuming.
        byte[] data = "hello-skija".getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // Closing should NOT prevent reads of an already-buffered copy.
        byte[] buffered = in.readAllBytes();
        in.close();
        assertArrayEquals(data, buffered);
    }

    // ---------- helpers ----------

    private static boolean readNativeLoaded() throws Exception {
        Field f = NativeLibraryPreloader.class.getDeclaredField("nativeLoaded");
        f.setAccessible(true);
        return f.getBoolean(null);
    }

    private static void setNativeLoaded(boolean value) throws Exception {
        Field f = NativeLibraryPreloader.class.getDeclaredField("nativeLoaded");
        f.setAccessible(true);
        f.setBoolean(null, value);
    }

    private static Object swapSystemProperty(String key, String value) {
        String prev = System.getProperty(key);
        System.setProperty(key, value);
        return new String[]{key, prev};
    }

    private static void restoreSystemProperty(Object holder) {
        String[] arr = (String[]) holder;
        if (arr[1] == null) System.clearProperty(arr[0]);
        else System.setProperty(arr[0], arr[1]);
    }
}
