🇬🇧 English · [🇷🇺 Русский](../../ru/adr/0005-bilingual-docs-mirror-tree.md) · [🇨🇳 中文](../../zh/adr/0005-bilingual-docs-mirror-tree.md)

**Superseded by [ADR 0006](0006-trilingual-docs-mirror-tree.md)**: documentation moved from two languages to three (Chinese added); kept here for the history of why the mirror is shaped the way it is.

# Bilingual documentation — `docs/en/` + `docs/ru/`, root `README.md`/`CONTEXT.md` stay put

Documentation in this repo exists in both English and Russian. We first tried a mirrored `docs/<lang>/` tree that held a translation for every doc regardless of where its English original lived (repo root or `docs/`), so `README.md`/`CONTEXT.md` translated straight into `docs/ru/README.md`/`docs/ru/CONTEXT.md`. In practice this mixed English and Russian files inside `docs/` itself (`docs/adr/*.md` sitting right next to a `docs/ru/` subfolder) — confusing to browse, and inconsistent with how `docs/ru/` looked complete on its own.

We switched to a plain symmetric split for everything actually inside `docs/`: `docs/en/` holds every English doc that isn't `README.md`/`CONTEXT.md`, `docs/ru/` its exact mirror (`docs/en/adr/0001-....md` ↔ `docs/ru/adr/0001-....md`, `docs/en/adding-a-release.md` ↔ `docs/ru/adding-a-release.md`). `README.md` and `CONTEXT.md` are the deliberate exception: they stay at the repo root in English, not under `docs/en/`, because that's where GitHub needs `README.md` to render the repo homepage and where the `domain-modeling` skill looks for `CONTEXT.md` by convention. Their Russian translations still live at `docs/ru/README.md` and `docs/ru/CONTEXT.md`, so `docs/ru/` remains the complete Russian mirror even though its English counterpart for those two files is the repo root, not `docs/en/`.

`CONTEXT.md` is the one file whose canonical language changed as part of this overall effort (see history for the original mixed-language version): it is now fully English at the root — canonical for future `domain-modeling` sessions — with the prior Russian content preserved as its translation at `docs/ru/CONTEXT.md`.

Every language pair carries a language-switcher link at the top of both files, and the translated side additionally carries a short note pointing back at its source file to keep in sync.

Consequence: from this point on, every new doc (a new ADR, a new guide) must be added in both languages before the work is considered done — a doc added in English only is an incomplete addition, not a followup task. A new ADR goes in `docs/en/adr/` + `docs/ru/adr/`; nothing new gets added to the repo root except `README.md`/`CONTEXT.md` themselves. This is a deliberate, ongoing cost, not a one-time backfill; the mirror only stays trustworthy if nothing is ever added to just one side of it.
