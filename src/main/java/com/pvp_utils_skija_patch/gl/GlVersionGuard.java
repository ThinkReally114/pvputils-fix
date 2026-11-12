package com.pvp_utils_skija_patch.gl;

import com.pvp_utils_skija_patch.SkijaPatchMod;
import com.pvp_utils_skija_patch.util.PlatformDetector;
import com.pvp_utils_skija_patch.util.PlatformDetector.Platform;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;

import java.util.concurrent.atomic.AtomicBoolean;

public final class GlVersionGuard {
    private static final AtomicBoolean PROBE_LOGGED = new AtomicBoolean();
    private static final AtomicBoolean FAILURE_LOGGED = new AtomicBoolean();
    private static volatile boolean probing;
    private static volatile Boolean supports46;

    private GlVersionGuard() {
    }

    private static boolean isMacOs() {
        Platform platform = PlatformDetector.detectPlatform();
        return platform == Platform.MACOS_X64 || platform == Platform.MACOS_ARM64;
    }

    public static void beforeCreate() {
        if (!isMacOs()) {
            return;
        }
        if (probing) {
            return;
        }
        try {
            if (supports46 == null) {
                probe();
            }
            if (Boolean.FALSE.equals(supports46)) {
                GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
                GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
                if (PROBE_LOGGED.compareAndSet(false, true)) {
                    SkijaPatchMod.LOGGER.info("[PVPUtils-Fix] Device OpenGL is below 4.6; PVPUtils's 4.6 context request is blocked");
                }
            }
        } catch (Throwable throwable) {
            if (FAILURE_LOGGED.compareAndSet(false, true)) {
                SkijaPatchMod.LOGGER.warn("[PVPUtils-Fix] OpenGL capability detection failed; leaving context hints unchanged", throwable);
            }
        }
    }

    public static void afterCreate(long window) {
        if (!isMacOs()) {
            return;
        }
        if (probing || window == 0L) {
            return;
        }
        try {
            int major = GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_CONTEXT_VERSION_MAJOR);
            int minor = GLFW.glfwGetWindowAttrib(window, GLFW.GLFW_CONTEXT_VERSION_MINOR);
            if (major <= 0) {
                return;
            }
            supports46 = major > 4 || (major == 4 && minor >= 6);
            if (PROBE_LOGGED.compareAndSet(false, true)) {
                SkijaPatchMod.LOGGER.info("[PVPUtils-Fix] Detected OpenGL {}.{}; {} PVPUtils's 4.6 context request",
                        major, minor, supports46 ? "allowing" : "blocking");
            }
        } catch (Throwable throwable) {
            if (FAILURE_LOGGED.compareAndSet(false, true)) {
                SkijaPatchMod.LOGGER.warn("[PVPUtils-Fix] Could not read the created OpenGL context version", throwable);
            }
        }
    }

    private static void probe() {
        probing = true;
        long probe = 0L;
        GLFWErrorCallback previous = null;
        try {
            previous = GLFW.glfwSetErrorCallback(null);
        } catch (Throwable ignored) {
        }
        try {
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 4);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 6);
            probe = GLFW.glfwCreateWindow(1, 1, "pvputils-fix-gl-probe", 0L, 0L);
            if (probe != 0L) {
                supports46 = Boolean.TRUE;
            } else {
                supports46 = Boolean.FALSE;
            }
        } catch (Throwable throwable) {
            supports46 = Boolean.FALSE;
        } finally {
            if (probe != 0L) {
                try {
                    GLFW.glfwDestroyWindow(probe);
                } catch (Throwable ignored) {
                }
            }
            try {
                GLFW.glfwGetError(null);
            } catch (Throwable ignored) {
            }
            try {
                GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_TRUE);
            } catch (Throwable ignored) {
            }
            if (previous != null) {
                try {
                    GLFW.glfwSetErrorCallback(previous);
                } catch (Throwable ignored) {
                }
            }
            probing = false;
        }
    }
}
