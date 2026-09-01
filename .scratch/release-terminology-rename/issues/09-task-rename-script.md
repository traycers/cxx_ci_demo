Type: task
Blocked by: 02
Status: resolved

## Question

Rename `scripts/new-release.sh` → `scripts/new-track.sh` and update its content for the new
terminology — this script's whole job is creating a new release-family directory, so it's the one
piece of code most saturated with the word:

- Header comment, usage text, and inline comments: "release"/"Release" → "track"/"Track"
  throughout (e.g. `Creates a new release (see docs/adding-a-release.md)` →
  `Creates a new track (see docs/adding-a-track.md)` — update the doc cross-reference to match
  ticket 04's renamed file).
- CLI usage/arg names: `<new_config_name>` → `<new_track_name>`, `[source_config_name]` →
  `[source_track_name]`. The bash variable names reading them
  (`NEW_CONFIG_NAME`/`SOURCE_CONFIG_NAME`) → `NEW_TRACK_NAME`/`SOURCE_TRACK_NAME` — purely
  internal, but keep them matching the renamed CLI vocabulary rather than leaving a mismatch.
- `CXX_CI_DEMO_DIR`, `INDEX_FILE`, `SRC_DIR`/`DST_DIR` — path construction stays mechanically the
  same, just now resolving into `track_N`-named directories once ticket 07 has renamed the
  existing ones; no path logic changes needed here, only the variable/doc text above.
- The `to_pascal_case` conversion function is untouched — it's generic, doesn't mention "release".
- The sed rename passes (`*Id`/`*ConfigName`/`<word>_`-prefix substitutions) — update the
  `*ConfigName` pattern to `*TrackName`, matching ticket 07's `ConfigName`→`TrackName` decision, so
  a script-generated fourth track stays consistent with the three hand-renamed ones.
- The "next steps" printed output at the end — reword for "track", and update its step 5 (which
  currently says `Create refs/heads/${NEW_CONFIG_NAME} in project_a through project_e on GitLab`)
  to reference the renamed variable.

After editing, do a dry sanity pass: read through once end-to-end and confirm every remaining
occurrence of the literal word "release" in the file is either absent or a deliberate exception you
can name (there shouldn't be any exceptions — this file has none of the historical-ADR-style
reasons to keep old wording).

## Answer

`scripts/new-release.sh` → `scripts/new-track.sh`, executable bit preserved. All prose/comments,
CLI usage (`<new_track_name> [source_track_name]`), bash variables
(`NEW_TRACK_NAME`/`SOURCE_TRACK_NAME`), the `*ConfigName`→`*TrackName` sed pattern, and the
printed next-steps renamed for `track`. `to_pascal_case` left untouched (generic, per the ticket).
`bash -n` syntax check passed; post-edit grep for "release" came back clean, no exceptions needed.

**Functionally tested** (not just read-through): ran the new script against a scratch copy of the
whole repo (`new-track.sh track_test main`) — produced `TrackTest`/`TrackTestId`/
`TrackTestTrackName`/`TrackTest_*` identifiers correctly, `branch_default`/`branch_spec` correctly
`track_test`, registered `subProject(TrackTest)` in the copy's `CxxCiDemo.kt`, doubled-prefix
sanity check clean. Scratch copy deleted after verifying — nothing left behind in the working
tree beyond the renamed script itself.
