Type: task
Blocked by: 07, 08, 09
Status: resolved

## Question

Verify the identifier-level rename landed cleanly and nothing was missed or double-migrated:

1. `grep -rli "release" .` (excluding `.scratch/`, `.claude/plugins`, and this map's own tickets)
   — every remaining hit should be an intentional exception: the historical ADRs (0002, 0005,
   0007, 0008, 0010 — per the map's Notes, left untouched on purpose), the `Release` (branch
   family) glossary term note that's now `Track` but might still mention "release" in passing when
   explaining the `_Avoid_` line, and `CONTEXT.md`'s deliberately-unrenamed `config_name` entry
   (ticket 02's narrow carve-out). Anything else is a miss — fix it.
2. `grep -rln "ReleaseN\|Release_N\|release_N" -E` style check (adjust for the real pattern) across
   `repos/ci-infra/main/.teamcity/cxx_ci_demo/` and `repos/project_{a..e}/` for literal leftover
   `Release1`/`Release2`/`Release3`/`release_1`/`release_2`/`release_3` strings.
3. Doubled-prefix check in the renamed DSL directories (same check `new-release.sh`/ticket 07 does):
   no `TrackN_TrackN_` pattern anywhere.
4. Confirm every doc cross-reference updated: `CONTEXT.md`'s link to `adding-a-release.md` now
   points at `adding-a-track.md`; `releases.md`'s (now `tracks.md`) links; any other doc that
   linked the renamed files.
5. Spot-check the three renamed `docs/*/tracks.md` `## track_N` headers actually match the three
   renamed directories on disk (`track_1`, `track_2`, `track_3`) — a mismatch here would mean
   ticket 05 and ticket 07/08 drifted.

Record what was found and fixed, if anything — this ticket's value is in catching drift between
the tickets that ran before it, not in doing new renaming work itself.

## Answer

Ran the full checklist. **Two real drift items found and fixed, outside any prior ticket's scope**:

1. **`scripts/bootstrap/teamcity_ops.py`** — never covered by any of tickets 01–09 (the map's
   Destination didn't list it), but its comments referenced the old script name
   (`scripts/new-release.sh`), old doc name (`adding-a-release.md`), old identifiers
   (`release_1`/`release_2`), and generic branch-family prose ("every release's demo VCS roots",
   "one-release demo"), plus a Python variable `release_project_ids` and a log message. All fixed
   for consistency (`track_project_ids`, "track" prose, updated script/doc cross-references) —
   otherwise this file would have been the one place in the repo still describing tooling that no
   longer exists under those names.
2. **`CONTEXT.md`'s `config_name` entry**, all 3 languages — its illustrative example
   (`release_2_0` → branches `release_2_0`, `release_2_0-hotfix-1`) was left over from ticket 01,
   written before the scope expanded to identifiers. Once ticket 04 renamed the same illustrative
   example throughout `adding-a-track.md` to `track_2_0`, this one fell out of sync. Fixed to match
   (`track_2_0` throughout).

**Checklist results, otherwise clean**:
1. Full-repo `release` grep: every remaining hit is an intentional exception — `CONTEXT.md`/
   `roadmap.md`'s `release` package-variant term (×3 langs each), the 4 `Result.kt` files'
   deliberately-preserved generic "release-packaging"/"release-hardening"/"Filtering files for
   release" phrasing (ticket 07), the 5 historical ADRs untouched per convention (×3 langs), and
   12 `.gitignore` files with a generic `build/Release` CMake/MSVC boilerplate line — unrelated to
   this repo's terminology, never in scope.
2. No leftover `Release1`/`Release2`/`Release3`/`release_1`/`release_2`/`release_3` identifiers
   anywhere under the DSL or the five demo-project repos.
3. No doubled-prefix pattern anywhere in the renamed DSL tree.
4. No living doc still links to `adding-a-release.md` or `releases.md`.
5. `docs/*/tracks.md`'s `## track_1`/`## track_2`/`## track_3` headers match the three renamed
   directories on disk exactly.
