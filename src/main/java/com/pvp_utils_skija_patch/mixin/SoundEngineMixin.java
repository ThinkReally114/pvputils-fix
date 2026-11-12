package com.pvp_utils_skija_patch.mixin;

import com.pvp_utils_skija_patch.audio.Mp3SoundInstance;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SoundEngine.class)
public abstract class SoundEngineMixin {
    @Shadow
    @Final
    private Options options;

    @Shadow
    private float calculateVolume(float volume, SoundSource source) {
        throw new AssertionError();
    }

    @Unique
    private float pvputilsfix$independentVolume(SoundInstance sound) {
        return Mth.clamp(sound.getVolume(), 0.0F, 1.0F)
                * Mth.clamp(options.getFinalSoundSourceVolume(SoundSource.MASTER), 0.0F, 1.0F);
    }

    @Redirect(method = "play", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/sounds/SoundEngine;calculateVolume(FLnet/minecraft/sounds/SoundSource;)F"))
    private float pvputilsfix$initialVolume(SoundEngine engine, float volume, SoundSource source,
            SoundInstance sound) {
        return sound instanceof Mp3SoundInstance ? pvputilsfix$independentVolume(sound)
                : calculateVolume(volume, source);
    }

    @Inject(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F",
            at = @At("HEAD"), cancellable = true)
    private void pvputilsfix$updateVolume(SoundInstance sound, CallbackInfoReturnable<Float> callback) {
        if (sound instanceof Mp3SoundInstance) {
            callback.setReturnValue(pvputilsfix$independentVolume(sound));
        }
    }
}
