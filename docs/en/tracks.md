🇬🇧 English · [🇷🇺 Русский](../ru/tracks.md) · [🇨🇳 中文](../zh/tracks.md)

# Tracks

The concrete tracks that exist in this repo right now (see `CONTEXT.md` for what a track is in general, and `adding-a-track.md` for how to create a new one).

## track_1

The first track: a primitive C++ project structure (flat `src/`, no `cmake/` subdirectory).

## track_2

The second track: an improved C++ project structure (`app_a/`, `cmake/`).

## main

The current track: demonstrates changing the build's dependency tree — `project_a` now chains through `project_c` into `project_d` instead of depending on `project_b` directly. See [ADR 0009](adr/0009-flat-artifact-dependencies-for-chain.md).

## track_3

The third track: demonstrates the build's dependency-tree change relative to `track_2` — `project_a` now chains through `project_c` into `project_d` instead of depending on `project_b` directly. See [ADR 0009](adr/0009-flat-artifact-dependencies-for-chain.md).
