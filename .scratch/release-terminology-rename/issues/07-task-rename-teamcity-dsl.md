Type: task
Blocked by: 02
Status: resolved

## Question

Rename the TeamCity Kotlin DSL for the three existing releases (soon: tracks) in
`repos/ci-infra/main/.teamcity/cxx_ci_demo/`, in place — this is a **rename**, not the
copy-a-new-one operation `scripts/new-release.sh` does, so don't reach for that script; do it by
hand, using its renaming logic (see `docs/en/adding-a-release.md`'s "What the script does" section
— soon `adding-a-track.md`, ticket 04 — as the reference for *which* identifiers move together) as
a guide, adapted for rename instead of copy.

For each of `release_1`, `release_2`, `release_3`:

1. **Directory**: `cxx_ci_demo/release_N/` → `cxx_ci_demo/track_N/`.
2. **File**: `ReleaseN.kt` → `TrackN.kt` (PascalCase of `track_N` — e.g. `track_1` → `Track1`,
   same `to_pascal_case` scheme `new-release.sh` already uses: capitalize each `_`-segment, join
   with no separator).
3. **Identifiers**, longest-match-first exactly like the script's own sed passes (`*Id`/
   `*ConfigName`-suffixed forms before the bare prefix, so a blanket rename doesn't clip them):
   `ReleaseN` → `TrackN`, `ReleaseNId` → `TrackNId`, `ReleaseNConfigName` → `TrackNTrackName`
   (note: **not** `TrackNConfigName` — the `ConfigName` suffix itself is being renamed to
   `TrackName`, per the map's Notes), every `ReleaseN_*` object (`ReleaseN_ProjectA`,
   `ReleaseN_BuildCImage`, `ReleaseN_ResultBuild`, `ReleaseN_BaseBuild`, `ReleaseN_ProjectAVcs`,
   etc.) → `TrackN_*`.
4. **String literals / data** (not identifiers): the `TrackNConfigName`... — sorry, `TrackNTrackName` val's string value `"release_N"` → `"track_N"`; the project's `name`/`description`;
   `branch_default` (`"refs/heads/release_N"` → `"refs/heads/track_N"`) and both `branch_spec`
   alternatives (`refs/heads/(release_N)` / `refs/heads/(release_N-*)` → `refs/heads/(track_N)` /
   `refs/heads/(track_N-*)`).
5. **Docker tag prefix**: every `cxxci-build:release_N-...` reference (`BuildCImage`'s tag, the
   `keep_images_count` cleanup step's prefix filter) → `cxxci-build:track_N-...`.
6. **Registration**: in `CxxCiDemo.kt`, `subProject(ReleaseN)` → `subProject(TrackN)` for all
   three.
7. **Do not** touch the `.teamcity/cxx_ci_demo/` path segments in `BuildCImage.kt`'s
   `docker build -f ...` lines beyond the `${...ConfigName}`→`${...TrackName}` reference itself —
   same caution `adding-a-release.md` gives for the copy case, applies equally here.

Sanity-check like the script does: grep each renamed directory for a doubled prefix
(`TrackN_TrackN_` or similar) — a leftover would mean a string literal got clobbered by a
too-broad find-replace.

`main`'s own DSL (`Main.kt`, `Main_*` objects) is **mostly** unaffected — `main` was never a
`release_N` identifier, so `Main`, `Main_ProjectA`, etc. don't change. **Exception, found while
resolving ticket 04**: `MainConfigName` does need to become `MainTrackName` — the `ConfigName`
suffix rename is universal (applies to every track including `main`), not scoped to
`Release`-prefixed identifiers. Rename that one identifier (and its `${MainConfigName}` reference
in `BuildCImage.kt`, and `name = MainConfigName` in `Main.kt`) alongside the three `release_N`
tracks.

## Answer

Renamed `release_1`/`release_2`/`release_3` → `track_1`/`track_2`/`track_3` throughout
`repos/ci-infra/main/.teamcity/cxx_ci_demo/`: directories, `ReleaseN.kt`→`TrackN.kt`, every
identifier (`ReleaseN`/`ReleaseNId`/`ReleaseN_*` → `TrackN`/`TrackNId`/`TrackN_*`,
`ReleaseNConfigName`→`TrackNTrackName`), the config-name string values and
`branch_default`/`branch_spec` patterns, docker tag prefixes (these fell out automatically —
they're built by Kotlin string interpolation off the renamed `TrackNTrackName` val, not a
separate hardcoded literal). `CxxCiDemo.kt` registrations → `subProject(Track1/2/3)`, plus its
own "Index of release/branch-family configurations" comment and example
(`NextReleaseId`→`NextTrackId`). `MainConfigName`→`MainTrackName` applied to `main/` as flagged.
Doubled-prefix sanity check across the whole `.teamcity/` tree: clean. `settings.kts` and
Dockerfiles: no "release" mentions, untouched.

**Prose required per-line judgment, not blanket find-replace** — a bare `\brelease\b` sed would
have clobbered unrelated generic-English uses of "release" (as in "shipping a release", not the
branch-family concept). Found in every `buildTypes/Result.kt` (all 4: main, track_1/2/3):
"Aggregation/**release**-packaging build type", "future **release**-hardening logic",
`"Filtering files for release - ..."` — left these three exactly as-is, they're about packaging
for a software release, unrelated to `Track`. Everything else that *was* about the branch-family
concept got renamed: "One "release" / branch-family configuration" → "track", "Copyable for the
new release" → "track", "every image this release ever built" → "track", "this release's own
directory/subdirectory" → "track's", "two releases sharing the one docker daemon" → "two tracks
sharing", cross-repo mentions like "unlike release_1/release_2 where project_a..." (track_3's and
main's `ProjectB.kt`) → "track_1/track_2". One judgment call worth flagging: `Result.kt`'s "project_b
isn't part of this release's chain at all" (track_3 and main) — despite sitting right next to the
generic "release-packaging" phrase, this one *is* about the track-specific a→c→d dependency chain
(ADR 0009), so it became "this track's chain", not left alone.
