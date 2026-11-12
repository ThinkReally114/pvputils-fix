package com.pvp_utils_skija_patch.audio;

import com.pvp_utils_skija_patch.SkijaPatchMod;
import net.minecraft.client.resources.sounds.AbstractSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.valueproviders.ConstantFloat;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

public final class Mp3SoundInstance extends AbstractSoundInstance implements TickableSoundInstance {
    private static final AtomicLong NEXT_ID = new AtomicLong();
    private final WeighedSoundEvents events;
    private final Mp3AudioStream stream;
    private volatile float gain;
    private volatile boolean stopped;

    public Mp3SoundInstance(Mp3AudioStream stream, float gain) {
        super(Identifier.fromNamespaceAndPath("pvputilsfix", "netease/" + NEXT_ID.incrementAndGet()),
                SoundSource.MUSIC, SoundInstance.createUnseededRandom());
        this.stream = stream;
        this.gain = gain;
        relative = true;
        attenuation = Attenuation.NONE;
        sound = new Sound(identifier, ConstantFloat.of(1.0F), ConstantFloat.of(1.0F),
                1, Sound.Type.FILE, true, false, 0);
        events = new WeighedSoundEvents(identifier, null);
        events.addSound(sound);
    }

    @Override
    public WeighedSoundEvents resolve(SoundManager soundManager) {
        return events;
    }

    @Override
    public float getVolume() {
        return gain;
    }

    public void setGain(float gain) {
        this.gain = gain;
    }

    public Mp3AudioStream getStream() {
        return stream;
    }

    public void stop() {
        stopped = true;
        try {
            stream.close();
        } catch (IOException exception) {
            SkijaPatchMod.LOGGER.warn("[PVPUtils-Fix] Failed to close NetEase MP3 stream", exception);
        }
    }

    @Override
    public boolean isStopped() {
        return stopped || stream.isClosed();
    }

    @Override
    public void tick() {
    }
}
