package com.pvp_utils_skija_patch.audio;

import net.minecraft.resources.Identifier;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class Mp3StreamRegistry {
    private static final Map<Identifier, Mp3AudioStream> STREAMS = new ConcurrentHashMap<>();

    private Mp3StreamRegistry() {
    }

    public static void register(Identifier path, Mp3AudioStream stream) {
        STREAMS.put(path, stream);
    }

    public static Mp3AudioStream take(Identifier path) {
        return STREAMS.remove(path);
    }

    public static void remove(Identifier path) {
        STREAMS.remove(path);
    }
}
