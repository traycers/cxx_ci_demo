🇬🇧 English · [🇷🇺 Русский](../ru/releases.md) · [🇨🇳 中文](../zh/releases.md)

# Releases

The concrete releases that exist in this repo right now (see `CONTEXT.md` for what a release is in general, and `adding-a-release.md` for how to create a new one).

## release_1

The first release: a primitive C++ project structure (flat `src/`, no `cmake/` subdirectory).

## release_2

The second release: an improved C++ project structure (`app_a/`, `cmake/`).

## main

The current release: demonstrates changing the build's dependency tree — `project_a` now chains through `project_c` into `project_d` instead of depending on `project_b` directly. See [ADR 0009](adr/0009-flat-artifact-dependencies-for-chain.md).

## release_3

The third release: demonstrates the build's dependency-tree change relative to `release_2` — `project_a` now chains through `project_c` into `project_d` instead of depending on `project_b` directly. See [ADR 0009](adr/0009-flat-artifact-dependencies-for-chain.md).
