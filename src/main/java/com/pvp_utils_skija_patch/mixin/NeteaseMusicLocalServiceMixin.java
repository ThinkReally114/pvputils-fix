package com.pvp_utils_skija_patch.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

@Pseudo
@Mixin(targets = "com.pvp_utils.client.NeteaseMusic.NeteaseMusicLocalService", remap = false)
public class NeteaseMusicLocalServiceMixin {

    @Inject(method = "start", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onStart(CallbackInfo ci) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("win")) {
            ci.cancel();
        }
    }
}
