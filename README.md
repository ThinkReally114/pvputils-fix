# PVPUtils Skia Cross-Platform Patch

A Fabric mod that patches PVPUtils to support all platforms (Windows/Linux/macOS/Android).

## Problem

PVPUtils only includes Skia native library for Windows (`.dll`), causing crashes on Linux/Android.

## Solution

This patch mod includes all platform native libraries and pre-loads the correct one at startup.

## Supported Platforms

- Windows (x64, arm64)
- Linux (x64, arm64)
- macOS (x64, arm64)

## Usage

1. Install PVPUtils normally
2. Install this patch mod
3. Enjoy Skia-powered features on any platform!

## Building

```bash
./gradlew build
```

## Download

Get the latest build from [Actions](https://github.com/ThinkReally114/pvputils-fix/actions).
