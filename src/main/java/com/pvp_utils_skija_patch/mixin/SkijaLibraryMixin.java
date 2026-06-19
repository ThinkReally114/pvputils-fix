package com.pvp_utils_skija_patch.mixin;

import com.pvp_utils_skija_patch.SkijaPatchMod;
import com.pvp_utils_skija_patch.NativeLibraryPreloader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to intercept skija's Library.load() calls.
 * This ensures the native library is properly loaded before skija tries to use it.
 * 
 * Note: remap = false because skija is a third-party library, not Minecraft code.
 */
@Mixin(value = io.github.humbleui.skija.impl.Library.class, remap = false)
public class SkijaLibraryMixin {

    /**
     * Intercept the load() method to ensure native library is available.
     */
    @Inject(method = "load", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onLoad(CallbackInfoReturnable<Boolean> cir) {
        try {
            SkijaPatchMod.LOGGER.info("Mixin: Attempting to preload skija native library");
            NativeLibraryPreloader.preload();
            // Don't cancel - let the original method also try to load
            // If we already loaded it, the original method will find it already loaded
        } catch (Exception e) {
            SkijaPatchMod.LOGGER.warn("Mixin: Failed to preload native library: {}", e.getMessage());
            // Don't cancel - let the original method try to load
        }
    }

    /**
     * Intercept staticLoad to handle any fallback scenarios.
     */
    @Inject(method = "staticLoad", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onStaticLoad(CallbackInfoReturnable<Boolean> cir) {
        try {
            SkijaPatchMod.LOGGER.info("Mixin staticLoad: Preloading native library");
            NativeLibraryPreloader.preload();
        } catch (Exception e) {
            SkijaPatchMod.LOGGER.warn("Mixin staticLoad: Preload check failed: {}", e.getMessage());
        }
    }
}