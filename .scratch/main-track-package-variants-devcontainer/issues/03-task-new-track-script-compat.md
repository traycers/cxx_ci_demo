Type: task
Status: resolved
Blocked by: 01, 02

## Question

`scripts/new-track.sh` defaults its source track to `main` (`source_track_name` defaults to `main` — see `adding-a-track.md`), and mechanically copies+renames a track directory's identifiers (`*Id`/`*TrackName`/`<word>_`-prefix passes, `to_pascal_case`, the `Duplicate JVM class name` file-rename rule). Once tickets 01/02 land, `main`'s directory shape changes significantly: nested `Main_Debug`/`Main_Release` subprojects (each with their own doubled build-type set) instead of a flat list, plus the new `Main_BuildDevImage` build type and `cxxci-<track>`/`cxxci-<track>-dev` naming.

Verify (and fix if needed) that `new-track.sh`, run against the new `main/` structure, still produces correct, non-doubled-prefix identifiers and correct image-tag interpolations for a hypothetical new track — for every level of nesting (the two subprojects' own ids/build types, not just the top-level track object). This repo has hit the doubled-prefix bug for real, twice, historically (see `adding-a-track.md`) — don't skip live testing against a scratch copy, same as the prior `release-terminology-rename` map's ticket 09 did.

Concretely, after ticket 01, `main/` has **three** files with top-level `val`s instead of one: `Main.kt` (`MainId`/`MainTrackName`, as before), `debug/MainDebug.kt` (`MainDebugId`), `release/MainRelease.kt` (`MainReleaseId`) — the same "Kotlin wraps top-level vals in a synthetic class named by the *file*" rule from `adding-a-track.md` applies to all three, so `new-track.sh` must rename all three files (not just the top-level one) to unique names for the new track, or a clone will collide on `MainDebugKt`/`MainReleaseKt` with `main/`'s own files. Every other new file from ticket 01 (`debug/templates/BaseBuild.kt`, `debug/buildTypes/{ProjectA,ProjectC,ProjectD,ProjectE,Result}.kt` and their `release/` siblings) contains only `object` declarations, so — like the rest of the tree — they're safe to keep sharing base filenames across track directories; only the object *names* need the per-track prefix pass already handled by the script's `<word>_`-prefix logic.

Test by running the script against a scratch copy of the repo (not the live repo) and inspecting the generated identifiers/tags by hand.

## Answer

Found and fixed two real bugs, both confirmed by an actual dry run (`bash scripts/new-track.sh track_2_0 main` against a scratch copy of the repo — not the live one):

1. **`main/debug/MainDebug.kt`/`main/release/MainRelease.kt` were never renamed at all** — the script only ever `mv`'d the single file named exactly `${SOURCE_WORD}.kt`. Fixed by generalizing that into a loop over every file matching `grep -rl '^val '` (every top-level-`val`-bearing file, the actual JVM-synthetic-class-name risk factor) whose basename starts with the source word — aborts loudly instead of guessing if it ever finds one that doesn't (same "fail loudly" philosophy as the existing doubled-prefix check).
2. **`MainDebugId`/`MainReleaseId` (ticket 01's val names) didn't match any of the script's existing identifier-rename patterns** — none of `\b${SOURCE_WORD}Id\b`, `\b${SOURCE_WORD}_`, or `\b${SOURCE_WORD}\b` match a 3-part compound like "MainDebugId" (no word boundary between "Main" and "Debug", and "MainId" isn't a substring of it). Rather than teach the script a new pattern for an inconsistent naming choice, fixed it at the source: renamed ticket 01's `MainDebugId`/`MainReleaseId` to `Main_DebugId`/`Main_ReleaseId` (underscore-joined, matching the rest of the codebase's `Main_ProjectA`-style convention) — the existing `\b${SOURCE_WORD}_` pattern already handles this correctly with zero script changes. Re-verified: no duplicate object names or top-level-val filenames anywhere in `.teamcity/` after this rename.

Dry-run result for `track_2_0` (from `main`): produced `Track20.kt`, `debug/Track20Debug.kt`, `release/Track20Release.kt` — all three uniquely named, no collision with `main/`'s own files; every object (`Track20_Debug_ProjectA`, `Track20_BuildDevImage`, etc.), id, and the new `cxxci-track_2_0:...`/`cxxci-track_2_0-dev:latest` image-tag interpolation came out correct; doubled-prefix check clean; no leftover `Main`/`MainId`/`MainTrackName`/`Main_` references anywhere in the generated `track_2_0/`. Scratch copy deleted after inspection.

Not fixed (pre-existing, out of this ticket's scope): a few comment strings in the generated files still say "MainDebug.kt"/"MainRelease.kt" in prose — the script has never rewritten comment prose, only identifiers and literal branch-name string values; harmless, cosmetic, consistent with existing script behavior.
