package com.pvp_utils_skija_patch.audio;

import com.goxr3plus.streamplayer.enums.Status;
import com.goxr3plus.streamplayer.stream.StreamPlayer;
import com.goxr3plus.streamplayer.stream.StreamPlayerException;
import com.pvp_utils_skija_patch.SkijaPatchMod;
import com.pvp_utils_skija_patch.mixin.SoundEngineAccessor;
import com.pvp_utils_skija_patch.mixin.SoundManagerAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.Identifier;

import java.io.File;
import java.io.IOException;

public final class SoundEngineStreamPlayer extends StreamPlayer {
    private File file;
    private Mp3AudioStream preparedStream;
    private long startMs;
    private volatile Mp3SoundInstance currentSound;
    private volatile float gain = 1.0F;
    private volatile Status playbackStatus = Status.INIT;

    public SoundEngineStreamPlayer() {
        SkijaPatchMod.LOGGER.info("[PVPUtils-Fix] NetEase music playback uses Minecraft's sound engine");
    }

    @Override
    public synchronized void open(File file) throws StreamPlayerException {
        Mp3AudioStream stream = openStream(file, 0L);
        stop();
        this.file = file;
        preparedStream = stream;
        playbackStatus = Status.OPENED;
    }

    @Override
    public synchronized void play() throws StreamPlayerException {
        Mp3AudioStream stream = preparedStream;
        if (stream == null) {
            stream = openStream(file, startMs);
        }
        preparedStream = null;
        start(stream, false);
    }

    @Override
    public synchronized boolean pause() {
        Mp3SoundInstance sound = currentSound;
        if (sound == null || sound.isStopped() || playbackStatus == Status.PAUSED) {
            return false;
        }
        sound.getStream().setPaused(true);
        playbackStatus = Status.PAUSED;
        updateChannel(sound);
        return true;
    }

    @Override
    public synchronized boolean resume() {
        Mp3SoundInstance sound = currentSound;
        if (sound != null && !sound.isStopped()) {
            if (playbackStatus != Status.PAUSED) {
                return false;
            }
            sound.getStream().setPaused(false);
            playbackStatus = Status.PLAYING;
            updateChannel(sound);
            return true;
        }
        if (file == null) {
            return false;
        }
        try {
            play();
            return true;
        } catch (StreamPlayerException exception) {
            SkijaPatchMod.LOGGER.error("[PVPUtils-Fix] Failed to resume NetEase music", exception);
            return false;
        }
    }

    @Override
    public synchronized void stop() {
        Mp3SoundInstance sound = currentSound;
        currentSound = null;
        playbackStatus = Status.STOPPED;
        startMs = 0L;
        closePreparedStream();
        dispose(sound);
    }

    @Override
    public synchronized long seekTo(int seconds) throws StreamPlayerException {
        long targetMs = Math.max(0L, (long) seconds) * 1000L;
        Mp3AudioStream stream = openStream(file, targetMs);
        long encodedBytes = stream.getEncodedBytes();
        boolean paused = playbackStatus == Status.PAUSED;
        startMs = targetMs;
        closePreparedStream();
        if (currentSound == null) {
            preparedStream = stream;
            playbackStatus = Status.OPENED;
        } else {
            start(stream, paused);
        }
        return encodedBytes;
    }

    @Override
    public synchronized void setGain(double gain) {
        this.gain = Double.isFinite(gain) ? (float) Math.max(0.0, Math.min(1.0, gain)) : 0.0F;
        Mp3SoundInstance sound = currentSound;
        if (sound != null) {
            sound.setGain(this.gain);
            updateChannel(sound);
        }
    }

    @Override
    public Status getStatus() {
        return playbackStatus;
    }

    @Override
    public boolean isPlaying() {
        return playbackStatus == Status.PLAYING;
    }

    @Override
    public boolean isPaused() {
        return playbackStatus == Status.PAUSED;
    }

    @Override
    public boolean isStopped() {
        return playbackStatus == Status.STOPPED;
    }

    @Override
    public boolean isOpened() {
        return playbackStatus == Status.OPENED;
    }

    @Override
    public boolean isPausedOrPlaying() {
        return isPaused() || isPlaying();
    }

    private Mp3AudioStream openStream(File file, long positionMs) throws StreamPlayerException {
        if (file == null) {
            throw new StreamPlayerException(new IOException("No MP3 file has been opened"));
        }
        try {
            return new Mp3AudioStream(file, positionMs);
        } catch (IOException exception) {
            throw new StreamPlayerException(exception);
        }
    }

    private void start(Mp3AudioStream stream, boolean paused) {
        Mp3SoundInstance previous = currentSound;
        Mp3SoundInstance sound = new Mp3SoundInstance(stream, gain);
        stream.setPaused(paused);
        currentSound = sound;
        playbackStatus = paused ? Status.PAUSED : Status.PLAYING;
        dispose(previous);
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (currentSound != sound || sound.isStopped()) {
                sound.stop();
                return;
            }
            SoundManager manager = minecraft.getSoundManager();
            Identifier path = sound.getSound().getPath();
            Mp3StreamRegistry.register(path, stream);
            try {
                if (manager.play(sound) == SoundEngine.PlayResult.NOT_STARTED) {
                    playbackFailed(sound);
                    SkijaPatchMod.LOGGER.warn("[PVPUtils-Fix] Minecraft could not start NetEase music playback");
                }
            } catch (RuntimeException exception) {
                playbackFailed(sound);
                manager.stop(sound);
                SkijaPatchMod.LOGGER.error("[PVPUtils-Fix] Failed to start NetEase music playback", exception);
            } finally {
                Mp3StreamRegistry.remove(path);
            }
        });
    }

    private void updateChannel(Mp3SoundInstance sound) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            if (currentSound != sound || sound.isStopped()) {
                return;
            }
            SoundEngine engine = ((SoundManagerAccessor) minecraft.getSoundManager()).pvputilsfix$getSoundEngine();
            SoundEngineAccessor access = (SoundEngineAccessor) engine;
            ChannelAccess.ChannelHandle handle = access.pvputilsfix$getChannels().get(sound);
            if (handle != null) {
                float volume = access.pvputilsfix$calculateVolume(sound);
                handle.execute(channel -> {
                    channel.setVolume(volume);
                    if (sound.getStream().isPaused()) {
                        channel.pause();
                    } else {
                        channel.unpause();
                    }
                });
            }
        });
    }

    private void dispose(Mp3SoundInstance sound) {
        if (sound == null) {
            return;
        }
        sound.stop();
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.getSoundManager().stop(sound));
    }

    private synchronized void playbackFailed(Mp3SoundInstance sound) {
        if (currentSound == sound) {
            currentSound = null;
            playbackStatus = Status.STOPPED;
        }
        sound.stop();
    }

    private void closePreparedStream() {
        if (preparedStream != null) {
            try {
                preparedStream.close();
            } catch (IOException exception) {
                SkijaPatchMod.LOGGER.warn("[PVPUtils-Fix] Failed to close prepared NetEase MP3 stream", exception);
            }
            preparedStream = null;
        }
    }
}
