Type: task
Blocked by: 01, 03, 04, 05, 07, 08, 09, 10
Status: resolved

## Question

**Revised per ticket 02** — this now documents a full identifier-level rename (like ADR 0010
itself did for `demo-project-*`→`project_*`), not just a prose/glossary one.

Write a new ADR, `docs/en/adr/0011-<slug>.md` (pick the slug — something like
`0011-track-term-replaces-release-for-branch-family.md`) plus its `ru`/`zh` mirrors, documenting
this rename — following the exact shape and precedent of ADR 0010
(`docs/*/adr/0010-project-naming-consistency.md`): what changed (glossary term, TeamCity Kotlin DSL
objects/files/directories, `scripts/new-release.sh`→`new-track.sh`, doc file names, docker tag
prefix — everything except `CONTEXT.md`'s own `config_name` entry, deliberately left as-is per
ticket 02's narrow call), why (the double-collision reasoning worked out during charting —
"release" was ambiguous with the planned `optimized`→`release` package variant; the first
replacement candidate, `stage`, was rejected for colliding with CI/CD's own "pipeline
stage"/"staging environment" meaning; `track` was chosen instead; the identifier scope itself grew
mid-effort once a suffix-only `ConfigName`→`TrackName` rename was found to produce inconsistent
mixed identifiers — see ticket 02), and what was deliberately left alone (the live GitLab/TeamCity
stand — see the map's Out of scope — and pre-existing ADRs, per the convention ADR 0010 itself
established of not rewriting historical ADRs).

Write this last, once tickets 01/03/04/05/07/08/09/10 are actually done, so it accurately records
what was renamed rather than what was planned.

## Answer

Wrote `docs/{en,ru,zh}/adr/0011-track-term-replaces-release-for-branch-family.md`, following ADR
0010's exact shape (title + 3–4 paragraphs, no frontmatter/sections): the double-collision context
(Release vs. the planned `optimized` variant), why `stage` was rejected and `track` chosen, the
full list of what got renamed (glossary, DSL, script, seed dirs, doc files, the `ConfigName`→
`TrackName` mid-effort scope grew), and what was deliberately left alone (`CONTEXT.md`'s
`config_name` term name, the live GitLab/TeamCity stand, and the pre-existing ADRs — citing ADR
0010's own precedent for not rewriting historical ADRs). No ADR index file exists elsewhere in the
repo to update (checked — ADR 0010 isn't cross-linked from anywhere but its own file).

This closes the map — every ticket (01–10) is resolved.
