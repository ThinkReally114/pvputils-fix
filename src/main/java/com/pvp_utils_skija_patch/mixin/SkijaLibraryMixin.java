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
 */
@Mixin(targets = "io.github.humbleui.skija.impl.Library")
public class SkijaLibraryMixin {

    /**
     * Intercept the load() method to ensure native library is available.
     */
    @Inject(method = "load", at = @At("HEAD"), cancellable = true)
    private static void onLoad(CallbackInfoReturnable<Boolean> cir) {
        try {
            // If we haven't preloaded yet, try to preload now
            if (!NativeLibraryPreloader.class.getDeclaredField("nativeLoaded").getBoolean(null)) {
                SkijaPatchMod.LOGGER.info("Mixin: Attempting to preload skija native library");
                NativeLibraryPreloader.preload();
            }
        } catch (Exception e) {
            SkijaPatchMod.LOGGER.warn("Mixin: Failed to preload native library: {}", e.getMessage());
            // Don't cancel - let the original method try to load
        }
    }

    /**
     * Intercept staticLoad to handle any fallback scenarios.
     */
    @Inject(method = "staticLoad", at = @At("HEAD"), cancellable = true)
    private static void onStaticLoad(CallbackInfoReturnable<Boolean> cir) {
        try {
            // Ensure the library is preloaded before static initialization
            if (!NativeLibraryPreloader.class.getDeclaredField("nativeLoaded").getBoolean(null)) {
                SkijaPatchMod.LOGGER.info("Mixin staticLoad: Preloading native library");
                NativeLibraryPreloader.preload();
            }
        } catch (Exception e) {
            SkijaPatchMod.LOGGER.warn("Mixin staticLoad: Preload check failed: {}", e.getMessage());
        }
    }
}
