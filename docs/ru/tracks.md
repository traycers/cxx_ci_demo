[🇬🇧 English](../en/tracks.md) · 🇷🇺 Русский · [🇨🇳 中文](../zh/tracks.md)

_Перевод `docs/en/tracks.md`. При изменении оригинала обновите и эту версию — см. [ADR 0006](adr/0006-trilingual-docs-mirror-tree.md)._

# Список track'ов

Конкретные track'и, которые сейчас существуют в этом репозитории (что такое track вообще — см. `CONTEXT.md`, как создать новый — см. `adding-a-track.md`).

## track_1

Первый track: примитивная структура C++ проектов (плоский `src/`, без поддиректории `cmake/`).

## track_2

Второй track: улучшенная структура C++ проектов (`app_a/`, `cmake/`).

## main

Текущий track: демонстрирует изменение дерева зависимостей сборки — `project_a` теперь тянется через `project_c` к `project_d` вместо прямой зависимости от `project_b`. См. [ADR 0009](adr/0009-flat-artifact-dependencies-for-chain.md).

## track_3

Третий track: демонстрирует изменение дерева зависимостей сборки относительно `track_2` — `project_a` теперь тянется через `project_c` к `project_d` вместо прямой зависимости от `project_b`. См. [ADR 0009](adr/0009-flat-artifact-dependencies-for-chain.md).
