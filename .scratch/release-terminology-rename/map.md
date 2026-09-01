Label: wayfinder:map

## Destination

**(Revised after ticket 02 — scope expanded from prose-only to full identifier-level rename; see
ticket 02's Answer for why.)**

Repo-wide consistency, prose *and* identifiers: the branch-family concept currently called
**Release** is renamed to **Track** everywhere in this repo's files — `CONTEXT.md`, all `docs/*`
prose (`release.md`, `adding-a-release.md`→`adding-a-track.md`, `releases.md`→`tracks.md`,
`roadmap.md`, `tradeoff.md`, `developer-flow.md`, across `en`/`ru`/`zh`), the TeamCity Kotlin DSL
(`repos/ci-infra/main/.teamcity/cxx_ci_demo/release_{1,2,3}/` directories, `Release{1,2,3}.kt`
files, every `Release{1,2,3}*` object/id/param, the `ConfigName` suffix → `TrackName`, the
`release_{1,2,3}` config-name string values and their `branch_default`/`branch_spec` patterns, the
`cxxci-build:release_N-...` docker tag prefix), the demo-project branch seed directories
(`repos/project_{a..e}/release_{1,2,3}/` → `track_{1,2,3}/`), and `scripts/new-release.sh` →
`scripts/new-track.sh` (script content included). The planned package-variant term `optimized` is
renamed to **release**, now that the word is free. A new ADR records why, following the precedent
of ADR 0010.

**Files only — not the live stand** (see Out of scope): the running GitLab/TeamCity stand already
has real `release_1`/`release_2` branches and TeamCity subprojects built from them. This map does
not push the rename there, delete/recreate those branches or subprojects, or touch anything on the
live server — that stays a distinct, deliberately deferred action.

The map is done when every file listed above consistently says "Track"/"track" for the
branch-family concept and "release" for the package variant — including identifiers, filenames,
and directory names — with nothing left half-migrated, and the live stand still running unchanged
on the old names.

## Notes

- **Execution mode, not pure planning** — like the `teamcity-cxx-ci` map before it, tickets here
  apply their decision directly to the repo (edit the files), not just record it. This is a
  deliberate deviation from wayfinder's plan-only default, chosen because open decisions are
  nearly exhausted already (see below) — what's left is a large volume of careful, judgment-tinged
  editing across 3 languages, not open questions.
- **Tracker — local-markdown** (no external tracker configured for this repo; see
  `issue-tracker-local.md`). This map and its tickets are files under
  `.scratch/release-terminology-rename/`.
- **Terminology already settled** by the charting conversation (recorded here since it predates
  any ticket):
  - Branch-family term: `Release` → **`track`** (rejected `stage` — collides with CI/CD's
    "pipeline stage" / "staging environment"; rejected `line` and `variant` as less precise than
    the chosen `track`, modeled on browser release-tracks).
  - Scope: **full identifier-level rename, repo-files-only** (revised in ticket 02 — originally
    prose-only, expanded once the `ConfigName` Kotlin suffix turned out to be doc-coined rather
    than a literal code string, and a suffix-only rename would've produced inconsistent mixed
    identifiers like `Release1TrackName`). Not pushed to the live GitLab/TeamCity stand.
  - Package variant: `optimized` → **`release`** accepted as-is, despite `release` also loosely
    suggesting CMake's own `CMAKE_BUILD_TYPE=Release` (this variant is actually
    `RelWithDebInfo`) — judged a minor, acceptable ambiguity, not worth a different name.
  - `ConfigName`/`config_name` → **`TrackName`/`track_name`** throughout (both the Kotlin suffix
    and the `CONTEXT.md`-adjacent doc placeholder), for the same mixed-identifier consistency
    reason as above. Note: the `CONTEXT.md` **`config_name`** glossary entry itself (ticket 01,
    already closed) was deliberately left un-renamed — that specific call predates this expansion
    and the user scoped it narrowly at the time ("уже узко"); revisit only if a later ticket finds
    it now reads inconsistently against the renamed Kotlin suffix.
- **Domain/glossary** — root `CONTEXT.md` (terms: Release/Track, Package variant). Mirrors:
  `docs/ru/CONTEXT.md`, `docs/zh/CONTEXT.md` (per ADR 0006, trilingual mirror tree — keep them in
  sync with the English original's meaning, translated).
- **Established repo convention** (see ADR 0010): historical ADRs that predate a rename are left
  untouched, never rewritten to use new terminology — only forward-looking docs (CONTEXT.md,
  guides, README) get updated. Apply the same rule here: ADRs 0002/0005/0007/0008 (which mention
  "release" only incidentally) are **not** touched by this map.
- If a ticket's resolution surfaces a new open architectural question, stop and run `/grilling` +
  `/domain-modeling` again rather than deciding it silently inside a task ticket.

## Decisions so far

- [CONTEXT.md glossary update](issues/01-task-context-md-glossary.md) — `Release` (branch family)
  → `Track` and `optimized` → `release` applied to `CONTEXT.md`/`ru`/`zh`, with a new parenthetical
  disambiguating the package variant from CMake's own `CMAKE_BUILD_TYPE=Release`; obsolete
  collision-avoidance sentences removed.
- [Terminology boundary rule](issues/02-grilling-terminology-boundary-rule.md) — scope expanded
  mid-ticket from prose-only to full identifier-level rename (files only, not the live stand);
  `ConfigName`/`config_name` → `TrackName`/`track_name`; `release.md` diagram labels and
  `adding-a-release.md`'s worked example (and the file itself → `adding-a-track.md`) all rename
  uniformly, no more prose/identifier split to track. New tickets 07–10 cover the actual identifier
  work; tickets 03–05 revised in place.
- [Rewrite release.md](issues/03-task-rewrite-release-md.md) — `docs/{en,ru,zh}/release.md` →
  `track.md` (file renamed too, a gap in ticket 02/the earlier restructuring — noted and fixed
  here), full prose + both mermaid diagrams rewritten for `track`, all illustrative branch labels
  renamed.
- [Rewrite adding-a-release.md](issues/04-task-rewrite-adding-a-release-md.md) —
  `docs/{en,ru,zh}/adding-a-release.md` → `adding-a-track.md`, full rewrite including the worked
  example (`Release20`→`Track20` etc.). Surfaced that `MainConfigName`→`MainTrackName` is needed
  too (ticket 07 updated). `CONTEXT.md`'s links to this file fixed in all 3 languages.
- [Small doc sweep](issues/05-task-small-doc-sweep.md) — `releases.md`→`tracks.md` (headers
  `release_N`→`track_N`), `roadmap.md` (`optimized`→`release`, plus caught "release image"/
  "release template" mentions that would've become misleading post-rename), `tradeoff.md`,
  `developer-flow.md` — all 3 languages.
- [Rename TeamCity DSL](issues/07-task-rename-teamcity-dsl.md) — `release_1/2/3`→`track_1/2/3`
  fully renamed in `repos/ci-infra/.../cxx_ci_demo/` (dirs, files, identifiers, string literals,
  docker tags via interpolation, `CxxCiDemo.kt` registration), `MainConfigName`→`MainTrackName`.
  Prose done line-by-line, not blanket sed — preserved unrelated generic "release-packaging"/
  "release-hardening"/"Filtering files for release" phrasing, renamed genuine branch-family
  mentions including one judgment call ("this release's chain" → "this track's chain", ADR 0009
  dependency chain, not generic packaging).
- [Rename demo-project seed dirs](issues/08-task-rename-demo-project-seed-dirs.md) — 15 directory
  renames across `repos/project_{a..e}/`, `release_N`→`track_N`. Plain `mv`s, no content edits
  needed (confirmed clean before and after).
- [Rename script](issues/09-task-rename-script.md) — `scripts/new-release.sh` → `new-track.sh`,
  full rename (CLI args, bash vars, `*ConfigName`→`*TrackName` sed pattern). Functionally tested
  against a scratch copy of the repo — generated identifiers, branch patterns, and `CxxCiDemo.kt`
  registration all correct; doubled-prefix check clean.
- [Verification sweep](issues/10-task-verification-sweep.md) — found and fixed two real drift
  items no prior ticket covered: `scripts/bootstrap/teamcity_ops.py` (never in the map's
  Destination — old script/doc names and identifiers in its comments, plus a
  `release_project_ids` variable, all renamed), and `CONTEXT.md`'s `config_name` entry's
  illustrative example (`release_2_0`→`track_2_0`, out of sync with ticket 04's renamed doc, ×3
  languages). Everything else checked clean.
- [Write ADR](issues/06-task-write-adr.md) — `docs/{en,ru,zh}/adr/0011-track-term-replaces-release-for-branch-family.md`,
  following ADR 0010's shape. **Map complete — every ticket (01–10) resolved.**

## Not yet specified

<!-- none currently — the charting grep enumerated every prose file touching "release"/"optimized";
     if a resolving session finds another one, add it here before ticketing it -->

## Out of scope

- **Pushing the rename to the live GitLab/TeamCity stand** — recreating/renaming the real
  `release_1`/`release_2` branches (5 GitLab repos) and their TeamCity subprojects/build history
  under the `track_*` names, or deleting the old ones. Ruled out when the identifier-rename scope
  was decided (ticket 02): git can't rename a branch atomically (push-new + delete-old), and
  TeamCity subprojects would need recreating or a `useFromVCS` re-sync — real risk against a
  stand with a known open bug (docker-pull), for no benefit this map's destination needs. Stays a
  distinct, deliberately deferred action if ever wanted.
- ~~Identifier-level rename (originally out of scope — see ticket 02's Answer for why this was
  reversed and folded into the destination instead).~~
