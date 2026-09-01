Type: task
Blocked by: 02
Status: resolved

## Question

**Revised per ticket 02** (scope expanded from prose-only to full identifier rename — no more
prose/identifier split to apply here).

Rewrite `docs/en/release.md`, `docs/ru/release.md`, `docs/zh/release.md` to use `Track` for the
branch-family concept throughout — title, prose, and both mermaid diagrams (all labels, node text,
comments) — with **every** illustrative branch-name label renamed too (per ticket 02's Q1):
`release_client_x`→`track_client_x`, generic `release_1`/`release_2` teaching examples →
`track_1`/`track_2`. `hotfix_*`/`feature_*`/`special_feature` are unaffected (they never contained
"release").

Keep the three languages in sync in meaning (per ADR 0006), not machine-identical text — translate
naturally, the way the existing `ru`/`zh` versions already do for the current English original.

Diff each file against its current version before finishing and sanity-check: does every sentence
that used to say "release" now correctly say either "track" (concept) or keep its original
identifier spelling (per ticket 02's rule), with nothing missed or over-corrected?

## Answer

Rewrote and **also renamed the file itself**: `docs/{en,ru,zh}/release.md` → `docs/{en,ru,zh}/track.md`
(this ticket's original text didn't call out the filename, but it's the same `release`-in-a-noun
situation as `releases.md`→`tracks.md` and `adding-a-release.md`→`adding-a-track.md` — leaving it
named `release.md` would have been the one inconsistent leftover; noting the correction here since
ticket 02 didn't explicitly flag this file).

Title, all prose, both mermaid diagrams (all node text, comments, and every illustrative branch
label: `release_1`/`release_2`→`track_1`/`track_2`, `release_client_x`→`track_client_x`) rewritten
for `track`. `hotfix_*`/`feature_*`/`special_feature` untouched (never contained "release"). The
`<release_name>` doc placeholder → `<track_name>`, same as `config_name`→`track_name` elsewhere.
`en`/`ru`/`zh` translated in sync, not machine-identical. No other living doc linked to the old
`release.md` (only the three historical ADRs 0002/0005/0007 do, left untouched per the map's
Notes/ADR-0010 convention) — verified via grep before finishing. Post-edit grep for "release" in
all three new `track.md` files came back clean.
