Type: task
Blocked by: 02
Status: resolved

## Question

**Revised per ticket 02** (scope expanded from prose-only to full identifier rename — no more
prose/identifier split to apply here; this ticket now also renames the file itself).

Rename `docs/en/adding-a-release.md` → `docs/en/adding-a-track.md` (same for `ru`/`zh`), and
rewrite it fully:

- Title: `# Adding a new release` → `# Adding a new track`.
- All prose: "release"/"Release" → "track"/"Track".
- The worked example, uniformly: `release_2_0`→`track_2_0`, `Release20`→`Track20`,
  `Release20Id`→`Track20Id`, `Release20ConfigName`→`Track20TrackName` (the `ConfigName` suffix
  itself is renamed — see the map's Notes), `Main_ProjectA`/`Release20_ProjectA`-style object
  names → same pattern with `Track20_`, `MainConfigName`→`MainTrackName`.
- Every reference to `scripts/new-release.sh` → `scripts/new-track.sh` (the real script — see
  ticket 09; if that ticket hasn't landed yet in this working tree, still write this doc against
  the renamed name, since ticket 09 owns making the script match).
- `config_name` as a bare doc-level placeholder (e.g. `` `<config_name>` `` in `` `cxx_ci_demo/<config_name>/` `` and `` `refs/heads/<config_name>` ``) → `` `<track_name>` ``. Note the one
  narrow exception ticket 02 recorded: `CONTEXT.md`'s own `**config_name**` glossary entry (ticket
  01, already closed) was deliberately *not* renamed — don't let this ticket's find-replace touch
  `CONTEXT.md`.
- The `## Branch naming convention` section's literal patterns (`refs/heads/(release_2_0)`,
  `refs/heads/(release_2_0-*)`) → `refs/heads/(track_2_0)` / `refs/heads/(track_2_0-*)` — these
  describe the *worked example*'s branch, which is itself being renamed, so they change too (this
  is different from a real repo's actual `release_1`/`release_2`/`release_3` branches, which this
  ticket doesn't touch — see ticket 08 for those).

Keep `en`/`ru`/`zh` in sync in meaning per ADR 0006. Diff each file against its current version
before finishing, and update any inbound links to the old `adding-a-release.md` filename
(`CONTEXT.md`'s `Track` entry links here — check it still resolves after ticket 01's edit).

## Answer

Renamed and rewrote `docs/{en,ru,zh}/adding-a-release.md` → `adding-a-track.md`. Title, all prose,
and the entire worked example renamed uniformly: `release_2_0`→`track_2_0`, `Release20`→`Track20`,
`Release20Id`→`Track20Id`, `Release20ConfigName`→`Track20TrackName`,
`Release20_ProjectA`/etc.→`Track20_*`, every `scripts/new-release.sh` reference →
`scripts/new-track.sh`, `config_name` doc placeholders → `track_name`, the branch-spec literal
patterns (`refs/heads/(track_2_0)` etc.) renamed since they describe the worked example's own
branch. `releases.md`→`tracks.md` cross-reference updated in the intro paragraph.

**One correction surfacing while writing this**: the `main`/`Release20` comparison table has a
`MainConfigName`→`Release20ConfigName` row. Since `ConfigName`→`TrackName` is a universal suffix
rename (not scoped to `Release`-prefixed identifiers), this row is now
`MainTrackName`→`Track20TrackName` — meaning **`main`'s own DSL is not fully unaffected** the way
ticket 07 currently states ("nothing there contains 'release'" — true, but `MainConfigName` does
contain the `ConfigName` suffix being renamed). Flagging for ticket 07 to pick up: `Main.kt` needs
`MainConfigName`→`MainTrackName` too, even though `Main`/`Main_*` object names themselves don't
change.

Fixed the three living inbound links to the old filename: `CONTEXT.md`, `docs/ru/CONTEXT.md`,
`docs/zh/CONTEXT.md`'s `Track` entries now point at `adding-a-track.md`. Left `docs/*/releases.md`'s
own references to the old filename alone — that file's rename/rewrite is ticket 05's job, not
this one's. Left the three historical ADRs (0002, 0005, 0007, en/ru/zh) referencing the old
filename untouched, per the map's Notes / ADR-0010 convention.
