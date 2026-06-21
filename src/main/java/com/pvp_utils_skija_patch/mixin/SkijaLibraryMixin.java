package com.pvp_utils_skija_patch.mixin;

import com.pvp_utils_skija_patch.NativeLibraryPreloader;
import com.pvp_utils_skija_patch.SkijaPatchMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = io.github.humbleui.skija.impl.Library.class, remap = false)
public class SkijaLibraryMixin {

    @Inject(method = "load", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onLoad(CallbackInfo ci) {
        try {
            NativeLibraryPreloader.preload();
        } catch (Exception e) {
            SkijaPatchMod.LOGGER.warn("Mixin: 原生库预加载失败: {} / Failed to preload native library: {}", e.getMessage(), e.getMessage());
        }
    }

    @Inject(method = "staticLoad", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onStaticLoad(CallbackInfo ci) {
        try {
            NativeLibraryPreloader.preload();
        } catch (Exception e) {
            SkijaPatchMod.LOGGER.warn("Mixin staticLoad: 预加载检查失败: {} / Preload check failed: {}", e.getMessage(), e.getMessage());
        }
    }
}