🇬🇧 English · [🇷🇺 Русский](../../ru/adr/0006-trilingual-docs-mirror-tree.md) · [🇨🇳 中文](../../zh/adr/0006-trilingual-docs-mirror-tree.md)

**Supersedes [ADR 0005](0005-bilingual-docs-mirror-tree.md)**: the mirror-tree structure and the "every new doc lands on every side before it's done" rule survive unchanged; the language count moves from two to three, and this record replaces ADR 0005 as the source of truth for the current policy. ADR 0005 stays as the historical record of why the mirror is shaped the way it is.

# Trilingual documentation — `docs/zh/` joins `docs/en/` + `docs/ru/`

We added Simplified Chinese (`zh-Hans`) as a third documentation language, for the same reason Russian was added in ADR 0005: readers who aren't comfortable in English need this repo's docs. `docs/zh/` follows exactly the same shape ADR 0005 set up for `docs/ru/` — a full mirror of everything under `docs/`, with `README.md`/`CONTEXT.md` staying canonical-English at the repo root and their Chinese translations living at `docs/zh/README.md`/`docs/zh/CONTEXT.md`. The language-switcher header on every file now lists all three in the order they were introduced (`🇬🇧 English · 🇷🇺 Русский · 🇨🇳 中文`), and the translation note under the switcher on a translated file still names its source and still points readers to the governing ADR — now 0006 instead of 0005.

We backfilled all pre-existing docs into `docs/zh/` in the same change that introduced it, rather than letting `zh` sit partially populated: ADR 0005's own premise is that the mirror only stays trustworthy if nothing is ever added on just one side, and a repo with `zh` half-covered for an unbounded stretch is exactly the drift that rule exists to prevent.

The Chinese translations are a first-pass machine translation, not yet checked by a fluent/native reviewer — treat `docs/zh/` as functionally accurate but unreviewed until someone with real Chinese fluency has read it against the English originals.

We did not add automated tooling to check the three trees stay in sync (no script, no CI check) — this repo has 9 docs and no CI pipeline of its own to hang a check on, so a parity script would be infrastructure built for a problem that's still small enough to hold in a person's head. Worth revisiting if the doc count grows enough that manual discipline stops being reliable.

Consequence: same rule as ADR 0005, widened — every new doc now goes in `docs/en/`, `docs/ru/`, and `docs/zh/` together before it's considered done.
