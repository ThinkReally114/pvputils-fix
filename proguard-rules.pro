-keep class com.pvp_utils_skija_patch.SkijaPatchMod { *; }
-keep class com.pvp_utils_skija_patch.NativeLibraryPreloader { *; }
-keep class com.pvp_utils_skija_patch.util.PlatformDetector { *; }
-keep class com.pvp_utils_skija_patch.util.PlatformDetector$Platform { *; }

-keep class com.pvp_utils_skija_patch.mixin.** { *; }
-keep class com.pvp_utils_skija_patch.audio.** { *; }

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep @org.spongepowered.asm.mixin.Mixin class * { *; }
-keepclassmembers class * {
    @org.spongepowered.asm.mixin.* <methods>;
}

-keep class io.github.humbleui.skija.** { *; }

-keep class org.slf4j.** { *; }

-dontwarn
