package com.pvp_utils_skija_patch.mixin;

import com.pvp_utils_skija_patch.util.PlatformDetector;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class PatchMixinPlugin implements IMixinConfigPlugin {
    private static final Set<String> ANDROID_MIXINS = Set.of(
            "com.pvp_utils_skija_patch.mixin.MusicPlaybackServiceMixin",
            "com.pvp_utils_skija_patch.mixin.SoundBufferLibraryMixin",
            "com.pvp_utils_skija_patch.mixin.SoundEngineAccessor",
            "com.pvp_utils_skija_patch.mixin.SoundEngineMixin",
            "com.pvp_utils_skija_patch.mixin.SoundManagerAccessor",
            "com.pvp_utils_skija_patch.mixin.ChannelMixin"
    );

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return !ANDROID_MIXINS.contains(mixinClassName)
                || PlatformDetector.detectPlatform().getOs().equals("android");
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
