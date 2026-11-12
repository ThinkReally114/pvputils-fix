# PVPUtils-FIX

A Fabric client-side mod that patches PVPUtils with cross-platform Skia native loading and Android NetEase music playback through Minecraft's sound engine.

## Problem

PVPUtils only bundles the Skia native library for Windows (`.dll`), causing crashes on Linux, macOS, and Android when Skia features are used.

Its NetEase music player also relies on Java Sound output, which can fail with `LINE_NOT_SUPPORTED` on Android launchers.

(On Android, please use the OpenLTW renderer to achieve perfect rendering.)

## OpenGL Context Guard

- Probes the device OpenGL version each launch with a hidden 1x1 GLFW context while the game window is being created.
- When the device is below OpenGL 4.6, the real context is created with Minecraft's usual 3.3 hints, so PVPUtils's `WindowMixin` cannot request an unsupported 4.6 context.
- Minecraft's boot GLFW error dialog is suppressed during the probe, so unsupported devices no longer stop at the driver warning.

## NetEase Music Playback

- Only on Android, decodes cached MP3 files using the JLayer library bundled with PVPUtils and streams PCM through Minecraft's OpenAL sound engine.
- Supports playback, pause/resume, seeking, volume changes, and PVPUtils's existing song queue and repeat controls.
- Seeking scans MP3 frames without decoding the whole preceding track, then decodes a short preroll before the requested position.
- Playback volume is controlled by PVPUtils and Minecraft's Master slider, independently of the Music and Jukebox/Note Blocks sliders.
- PVPUtils continues to track lyrics and song completion using its existing duration clock; its Java Sound PCM visualizer is unavailable with this backend.

Install the built patch JAR alongside PVPUtils in the launcher's `mods` directory.

## Supported Platforms

- Windows
- Linux
- macOS
- Android

## Requirements

- Minecraft 1.21.11
- Fabric Loader >= 0.18.4
- Fabric API >= 0.141.3
- Java 21+
- PVPUtils

___
(Random musing: Do people really play Minecraft PVP on their phones?)
