[🇬🇧 English](../en/tracks.md) · [🇷🇺 Русский](../ru/tracks.md) · 🇨🇳 中文

_翻译自 `docs/en/tracks.md`。原文变更时请同步更新本文件——参见 [ADR 0006](adr/0006-trilingual-docs-mirror-tree.md)。_

# Track 列表

本仓库目前实际存在的 track(track 一般是什么见 `CONTEXT.md`,如何创建新 track 见 `adding-a-track.md`)。

## track_1

第一个 track:原始的 C++ 项目结构(扁平的 `src/`,没有 `cmake/` 子目录)。

## track_2

第二个 track:改进后的 C++ 项目结构(`app_a/`、`cmake/`)。

## main

当前的 track:展示了构建依赖树的变化——`project_a` 现在通过 `project_c` 链到 `project_d`,而不是直接依赖 `project_b`。见 [ADR 0009](adr/0009-flat-artifact-dependencies-for-chain.md)。

## track_3

第三个 track:相对于 `track_2` 展示了构建依赖树的变化——`project_a` 现在通过 `project_c` 链到 `project_d`,而不是直接依赖 `project_b`。见 [ADR 0009](adr/0009-flat-artifact-dependencies-for-chain.md)。
