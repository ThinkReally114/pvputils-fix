package com.pvp_utils_skija_patch.mixin;

import com.pvp_utils_skija_patch.audio.Mp3AudioStream;
import com.pvp_utils_skija_patch.audio.Mp3StreamRegistry;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(SoundBufferLibrary.class)
public abstract class SoundBufferLibraryMixin {
    @Inject(method = "getStream", at = @At("HEAD"), cancellable = true)
    private void pvputilsfix$openMp3Stream(Identifier path, boolean looping,
            CallbackInfoReturnable<CompletableFuture<AudioStream>> callback) {
        Mp3AudioStream stream = Mp3StreamRegistry.take(path);
        if (stream != null) {
            callback.setReturnValue(CompletableFuture.completedFuture(stream));
        }
    }
}
