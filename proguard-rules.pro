# ProGuard rules for pvputils-skija-patch
# Only shrink (remove unused code), do NOT obfuscate or optimize

# Keep mod entry points
-keep class com.pvp_utils_skija_patch.SkijaPatchMod { *; }
-keep class com.pvp_utils_skija_patch.NativeLibraryPreloader { *; }
-keep class com.pvp_utils_skija_patch.util.PlatformDetector { *; }
-keep class com.pvp_utils_skija_patch.util.PlatformDetector$Platform { *; }

# Keep Mixin classes
-keep class com.pvp_utils_skija_patch.mixin.** { *; }

# Keep enum values() and valueOf()
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Mixin annotations and framework
-keep @org.spongepowered.asm.mixin.Mixin class * { *; }
-keepclassmembers class * {
    @org.spongepowered.asm.mixin.* <methods>;
}

# Keep Skija API classes
-keep class io.github.humbleui.skija.** { *; }

# Keep SLF4J logging
-keep class org.slf4j.** { *; }

# Don't warn about missing references
-dontwarn
