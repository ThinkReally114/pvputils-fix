package com.pvp_utils_skija_patch;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkijaPatchMod implements ClientModInitializer {
    public static final String MOD_ID = "pvputils-skija-patch";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static boolean nativeLoaded = false;

    @Override
    public void onInitializeClient() {
        LOGGER.info("PVPUtils Skia Patch initialized - pre-loading native libraries");

        ClientLifecycleEvents.CLIENT_STARTING.register(this::onClientStarting);
    }

    private void onClientStarting(MinecraftClient client) {
        if (!nativeLoaded) {
            try {
                NativeLibraryPreloader.preload();
                nativeLoaded = true;
                LOGGER.info("PVPUtils Skia Patch: Native library preloaded successfully for platform {}", NativeLibraryPreloader.getCurrentPlatform());
            } catch (Exception e) {
                LOGGER.error("PVPUtils Skia Patch: Failed to preload native library", e);
            }
        }
    }
}
