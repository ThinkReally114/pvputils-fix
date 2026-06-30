package com.pvp_utils_skija_patch.mixin;

import com.pvp_utils_skija_patch.NativeLibraryPreloader;
import com.pvp_utils_skija_patch.SkijaPatchMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = io.github.humbleui.skija.impl.Library.class, remap = false)
public class SkijaLibraryMixin {

    private static void tryPreload(CallbackInfo ci, String methodName) {
        if (NativeLibraryPreloader.isNativeLoaded()) {
            ci.cancel();
            return;
        }
        try {
            NativeLibraryPreloader.preload();
            ci.cancel();
        } catch (Exception e) {
            SkijaPatchMod.LOGGER.warn("Mixin {}: 预加载失败 / Preload failed: {}", methodName, e.getMessage(), e);
        }
    }

    @Inject(method = "load", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onLoad(CallbackInfo ci) {
        tryPreload(ci, "load");
    }

    @Inject(method = "staticLoad", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onStaticLoad(CallbackInfo ci) {
        tryPreload(ci, "staticLoad");
    }
}
