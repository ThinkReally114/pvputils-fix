# PVPUtils-FIX

A Fabric client-side mod that patches PVPUtils to support all platforms by pre-loading the correct Skia native library at startup.

## Problem

PVPUtils only bundles the Skia native library for Windows (`.dll`), causing crashes on Linux, macOS, and Android when Skia features are used.

(On Android, please use the OpenLTW renderer to achieve perfect rendering.)

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
