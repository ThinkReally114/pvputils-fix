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
            LOGGER.info("原生库预加载成功，平台: {} / Native library preloaded successfully for platform {}", NativeLibraryPreloader.getCurrentPlatform(), NativeLibraryPreloader.getCurrentPlatform());
        } catch (Exception e) {
            LOGGER.error("原生库预加载失败 / Failed to preload native library", e);
        }
    }
}