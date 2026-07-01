<p align="left">
  
<img src="src/main/resources/icon.png" width="128" alt="PVPUtils-fix icon">
</p>

# PVPUtils-fix

A Fabric client-side mod that patches [PVPUtils](https://modrinth.com/mod/pvp-utils) to support all platforms by pre-loading the correct Skia native library at startup.

## Problem

PVPUtils only bundles the Skia native library for Windows (`.dll`), causing crashes on Linux, macOS, and Android when Skia features are used.

## Solution

This mod detects the current platform, downloads the matching Skia native library from Maven, verifies its integrity via SHA-256 checksum, and uses a Mixin to pre-load it before PVPUtils tries to load it.

## Supported Platforms

- Windows (x64, arm64)
- Linux (x64, arm64)
- macOS (x64, arm64)
- Android (arm64)

## Features

- **Cross-platform support** — Windows, Linux, macOS, and Android
- **SHA-256 checksum verification** — Ensures downloaded native libraries haven't been tampered with
- **Thread-safe loading** — Uses `AtomicBoolean` to prevent race conditions
- **Multi-mirror fallback** — Downloads from Aliyun, Tencent, Google, and Maven Central
- **Runtime download** — No native libraries bundled in the jar, downloaded on first launch

## Requirements

- Minecraft 1.21.11
- Fabric Loader >= 0.18.4
- Fabric API >= 0.141.3
- Java 21+

## Installation

1. Install [Fabric Loader](https://fabricmc.net/) for Minecraft 1.21.11
2. Install [PVPUtils](https://modrinth.com/mod/pvp-utils)
3. Install this mod into your `mods` folder
4. Launch the game — Skia features will work on any supported platform

## Building

```bash
./gradlew generateChecksums
./gradlew build
```

The built jar will be in `build/libs/`.

## Download

Get the latest build from [GitHub Actions](https://github.com/ThinkReally114/pvputils-fix/actions) or [Modrinth](https://modrinth.com/mod/pvputils-fix).

## License

MIT
