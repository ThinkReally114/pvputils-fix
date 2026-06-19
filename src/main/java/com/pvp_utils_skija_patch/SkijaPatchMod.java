package com.pvp_utils_skija_patch;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkijaPatchMod implements ClientModInitializer {
    public static final String MOD_ID = "pvputils-skija-patch";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        LOGGER.info("PVPUtils Skia Patch initialized - pre-loading native libraries");

        // Preload native library immediately on mod initialization
        if (!NativeLibraryPreloader.isNativeLoaded()) {
            try {
                NativeLibraryPreloader.preload();
                LOGGER.info("PVPUtils Skia Patch: Native library preloaded successfully for platform {}", NativeLibraryPreloader.getCurrentPlatform());
            } catch (Exception e) {
                LOGGER.error("PVPUtils Skia Patch: Failed to preload native library", e);
            }
        }
    }
}