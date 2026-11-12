package com.pvp_utils_skija_patch;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkijaPatchMod implements ClientModInitializer {
    public static final String MOD_ID = "pvputils-skija-patch";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("[PVPUtils-Fix] PVPUtils-Fix initialized - pre-loading native libraries");

        try {
            NativeLibraryPreloader.preload();
            if (NativeLibraryPreloader.isNativeLoaded()) {
                LOGGER.info("[PVPUtils-Fix] Native library preloaded successfully for platform {}",
                        NativeLibraryPreloader.getCurrentPlatform(), NativeLibraryPreloader.getCurrentPlatform());
            } else {
                LOGGER.warn("[PVPUtils-Fix] Native library preload did not complete, platform may be unsupported");
            }
        } catch (Exception e) {
            LOGGER.error("[PVPUtils-Fix] Failed to preload native library, Skia features will be unavailable", e);
        }
    }
}
