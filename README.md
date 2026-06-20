# PVPUtils-fix

A Fabric client-side mod that patches [PVPUtils](https://modrinth.com/mod/pvp-utils) to support Windows and Linux by pre-loading the correct Skia native library at startup.

## Problem

PVPUtils only bundles the Skia native library for Windows (`.dll`), causing crashes on Linux when Skia features are used.

## Solution

This mod includes Skia native libraries for both Windows and Linux and uses a Mixin to pre-load the correct one before PVPUtils tries to load it.

## Supported Platforms

- Windows (x64, arm64)
- Linux (x64, arm64)

## Requirements

- Minecraft 1.21.11
- Fabric Loader >= 0.18.4
- Fabric API
- Java 21+

## Installation

1. Install [Fabric Loader](https://fabricmc.net/) for Minecraft 1.21.11
2. Install [PVPUtils](https://modrinth.com/mod/pvp-utils)
3. Install this mod into your `mods` folder
4. Launch the game — Skia features will work on any supported platform

## Building

```bash
./gradlew build
```

The built jar will be in `build/libs/`.

## Download

Get the latest build from [GitHub Actions](https://github.com/ThinkReally114/pvputils-fix/actions).

## License

MIT
