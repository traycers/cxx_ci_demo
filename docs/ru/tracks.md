[🇬🇧 English](../en/tracks.md) · 🇷🇺 Русский · [🇨🇳 中文](../zh/tracks.md)

# Список track'ов

Конкретные track'и, которые сейчас существуют в этом репозитории (что такое track вообще — см. `CONTEXT.md`, как создать новый — см. `adding-a-track.md`).

## main

Текущий track: демонстрирует изменение дерева зависимостей сборки — `project_a` теперь тянется через `project_c` к `project_d` вместо прямой зависимости от `project_b`. См. [ADR 0009](adr/0009-flat-artifact-dependencies-for-chain.md). Также единственный на данный момент track с package variant'ами `debug`/`release` и dev container образом (см. [ADR 0013](adr/0013-debug-release-subprojects-and-track-scoped-image-naming.md), `roadmap.md`). Новые track'и обычно создаются именно из `main` (это source по умолчанию для `scripts/new-track.sh`) — поэтому он идёт первым.

## Release-треки

### release_1

Первый track: примитивная структура C++ проектов (плоский `src/`, без поддиректории `cmake/`).

### release_2

Второй track: улучшенная структура C++ проектов (`app_a/`, `cmake/`).

### release_3

Третий track: демонстрирует изменение дерева зависимостей сборки относительно `release_2` — `project_a` теперь тянется через `project_c` к `project_d` вместо прямой зависимости от `project_b`. См. [ADR 0009](adr/0009-flat-artifact-dependencies-for-chain.md).
