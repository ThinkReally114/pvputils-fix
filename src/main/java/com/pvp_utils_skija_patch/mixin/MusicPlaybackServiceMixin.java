package com.pvp_utils_skija_patch.mixin;

import com.goxr3plus.streamplayer.stream.StreamPlayer;
import com.pvp_utils_skija_patch.audio.SoundEngineStreamPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "com.pvp_utils.client.NeteaseMusic.MusicPlaybackService", remap = false)
public abstract class MusicPlaybackServiceMixin {
    @Redirect(method = "<init>", at = @At(value = "NEW",
            target = "com/goxr3plus/streamplayer/stream/StreamPlayer"), remap = false)
    private StreamPlayer pvputilsfix$createSoundEnginePlayer() {
        return new SoundEngineStreamPlayer();
    }
}
