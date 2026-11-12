package com.pvp_utils_skija_patch.audio;

import javazoom.jl.decoder.Bitstream;
import javazoom.jl.decoder.BitstreamException;
import javazoom.jl.decoder.Decoder;
import javazoom.jl.decoder.DecoderException;
import javazoom.jl.decoder.Header;
import javazoom.jl.decoder.SampleBuffer;
import net.minecraft.client.sounds.AudioStream;

import javax.sound.sampled.AudioFormat;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class Mp3AudioStream implements AudioStream {
    private final Bitstream bitstream;
    private final Decoder decoder = new Decoder();
    private final long startMs;
    private AudioFormat format;
    private short[] samples;
    private int sampleOffset;
    private int sampleLength;
    private long skipFrames;
    private long encodedBytes;
    private boolean ended;
    private volatile boolean closed;
    private volatile boolean paused;

    public Mp3AudioStream(File file, long startMs) throws IOException {
        this.startMs = Math.max(0L, startMs);
        bitstream = new Bitstream(new BufferedInputStream(new FileInputStream(file)));
        encodedBytes = Math.max(0, bitstream.header_pos());
        try {
            if (!loadFrame()) {
                throw new IOException("No MP3 audio at the requested position");
            }
        } catch (IOException | RuntimeException exception) {
            try {
                close();
            } catch (IOException closeException) {
                exception.addSuppressed(closeException);
            }
            throw exception;
        }
    }

    @Override
    public AudioFormat getFormat() {
        return format;
    }

    @Override
    public synchronized ByteBuffer read(int requestedBytes) throws IOException {
        if (closed || ended && sampleOffset >= sampleLength) {
            return null;
        }
        int frameSize = format.getFrameSize();
        int capacity = Math.max(frameSize, requestedBytes - requestedBytes % frameSize);
        ByteBuffer buffer = ByteBuffer.allocateDirect(capacity).order(ByteOrder.LITTLE_ENDIAN);
        while (buffer.remaining() >= frameSize) {
            if (sampleOffset >= sampleLength && !loadFrame()) {
                break;
            }
            int count = Math.min(buffer.remaining() / Short.BYTES, sampleLength - sampleOffset);
            for (int i = 0; i < count; i++) {
                buffer.putShort(samples[sampleOffset++]);
            }
        }
        if (buffer.position() == 0) {
            return null;
        }
        buffer.flip();
        return buffer;
    }

    private boolean loadFrame() throws IOException {
        while (!ended && !closed) {
            Header header;
            try {
                header = bitstream.readFrame();
            } catch (BitstreamException exception) {
                throw new IOException("Failed to read MP3 frame", exception);
            }
            if (header == null) {
                ended = true;
                return false;
            }
            encodedBytes += Integer.BYTES + header.framesize;
            try {
                if (format == null) {
                    int channels = header.mode() == Header.SINGLE_CHANNEL ? 1 : 2;
                    format = new AudioFormat(header.frequency(), 16, channels, true, false);
                    skipFrames = startMs * header.frequency() / 1000L;
                }
                int frameSamples = header.layer() == 1 ? 384
                        : header.layer() == 3 && header.version() != Header.MPEG1 ? 576 : 1152;
                if (skipFrames > frameSamples + (long) format.getSampleRate() / 4L) {
                    skipFrames -= frameSamples;
                    continue;
                }
                SampleBuffer decoded = (SampleBuffer) decoder.decodeFrame(header, bitstream);
                if (decoded.getSampleFrequency() != (int) format.getSampleRate()
                        || decoded.getChannelCount() != format.getChannels()) {
                    throw new IOException("MP3 audio format changed during playback");
                }
                int discardedFrames = (int) Math.min(skipFrames, frameSamples);
                skipFrames -= discardedFrames;
                samples = decoded.getBuffer();
                sampleLength = decoded.getBufferLength();
                sampleOffset = Math.min(sampleLength, discardedFrames * format.getChannels());
                if (sampleOffset < sampleLength) {
                    return true;
                }
            } catch (DecoderException exception) {
                throw new IOException("Failed to decode MP3 frame", exception);
            } finally {
                bitstream.closeFrame();
            }
        }
        return false;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public boolean isPaused() {
        return paused;
    }

    public boolean isClosed() {
        return closed;
    }

    public synchronized long getEncodedBytes() {
        return encodedBytes;
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        samples = null;
        sampleOffset = sampleLength;
        try {
            bitstream.close();
        } catch (BitstreamException exception) {
            throw new IOException("Failed to close MP3 stream", exception);
        }
    }
}
