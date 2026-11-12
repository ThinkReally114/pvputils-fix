package com.pvp_utils_skija_patch.mixin;

import com.mojang.blaze3d.audio.Channel;
import com.pvp_utils_skija_patch.audio.Mp3AudioStream;
import net.minecraft.client.sounds.AudioStream;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Channel.class)
public abstract class ChannelMixin {
    @Shadow
    private AudioStream stream;

    @Inject(method = "play", at = @At("TAIL"))
    private void pvputilsfix$applyPlaybackState(CallbackInfo callback) {
        if (stream instanceof Mp3AudioStream mp3) {
            Channel channel = (Channel) (Object) this;
            if (mp3.isClosed()) {
                channel.stop();
            } else if (mp3.isPaused()) {
                channel.pause();
            }
        }
    }

    @Inject(method = "unpause", at = @At("HEAD"), cancellable = true)
    private void pvputilsfix$keepManualPause(CallbackInfo callback) {
        if (stream instanceof Mp3AudioStream mp3 && (mp3.isPaused() || mp3.isClosed())) {
            callback.cancel();
        }
    }
}
