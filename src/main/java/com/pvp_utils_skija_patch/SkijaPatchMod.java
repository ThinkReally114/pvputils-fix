package com.pvp_utils_skija_patch;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkijaPatchMod implements ClientModInitializer {
    public static final String MOD_ID = "pvputils-skija-patch";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("PVPUtils Skia Patch 已初始化 - 正在预加载原生库 / PVPUtils Skia Patch initialized - pre-loading native libraries");

        try {
            NativeLibraryPreloader.preload();
            if (NativeLibraryPreloader.isNativeLoaded()) {
                LOGGER.info("原生库预加载成功，平台: {} / Native library preloaded successfully for platform {}",
                        NativeLibraryPreloader.getCurrentPlatform(), NativeLibraryPreloader.getCurrentPlatform());
            } else {
                LOGGER.warn("原生库预加载未完成，平台可能不受支持 / Native library preload did not complete, platform may be unsupported");
            }
        } catch (Exception e) {
            LOGGER.error("原生库预加载失败，Skia 功能将不可用 / Failed to preload native library, Skia features will be unavailable", e);
        }

        Thread musicThread = new Thread(() -> {
            try {
                NeteaseMusicPlatformFix.start();
            } catch (Throwable t) {
                LOGGER.error("网易云音乐后台线程异常 / Netease Music background thread error", t);
            }
        }, "PVPUtils-NeteaseMusic-Start");
        musicThread.setDaemon(true);
        musicThread.start();
        Runtime.getRuntime().addShutdownHook(new Thread(NeteaseMusicPlatformFix::stop,
                "PVPUtils-NeteaseMusic-Shutdown"));
    }
}
