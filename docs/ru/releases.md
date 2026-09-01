[🇬🇧 English](../en/releases.md) · 🇷🇺 Русский · [🇨🇳 中文](../zh/releases.md)

_Перевод `docs/en/releases.md`. При изменении оригинала обновите и эту версию — см. [ADR 0006](adr/0006-trilingual-docs-mirror-tree.md)._

# Список релизов

Конкретные релизы, которые сейчас существуют в этом репозитории (что такое релиз вообще — см. `CONTEXT.md`, как создать новый — см. `adding-a-release.md`).

## release_1

Первый релиз: примитивная структура C++ проектов (плоский `src/`, без поддиректории `cmake/`).

## release_2

Второй релиз: улучшенная структура C++ проектов (`app_a/`, `cmake/`).

## main

Текущий релиз: демонстрирует изменение дерева зависимостей сборки — `project_a` теперь тянется через `project_c` к `project_d` вместо прямой зависимости от `project_b`. См. [ADR 0009](adr/0009-flat-artifact-dependencies-for-chain.md).

## release_3

Третий релиз: демонстрирует изменение дерева зависимостей сборки относительно `release_2` — `project_a` теперь тянется через `project_c` к `project_d` вместо прямой зависимости от `project_b`. См. [ADR 0009](adr/0009-flat-artifact-dependencies-for-chain.md).
