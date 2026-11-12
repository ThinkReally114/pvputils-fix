package com.pvp_utils_skija_patch.mixin;

import com.pvp_utils_skija_patch.gl.GlVersionGuard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "org.lwjgl.glfw.GLFW", remap = false)
public abstract class GlfwContextMixin {
    @Inject(method = "glfwCreateWindow(IILjava/lang/CharSequence;JJ)J", at = @At("HEAD"), remap = false)
    private static void pvputilsfix$guardContextVersion(int width, int height, CharSequence title,
            long monitor, long share, CallbackInfoReturnable<Long> callback) {
        GlVersionGuard.beforeCreate();
    }

    @Inject(method = "glfwCreateWindow(IILjava/lang/CharSequence;JJ)J", at = @At("RETURN"), remap = false)
    private static void pvputilsfix$recordContextVersion(int width, int height, CharSequence title,
            long monitor, long share, CallbackInfoReturnable<Long> callback) {
        GlVersionGuard.afterCreate(callback.getReturnValue());
    }
}
