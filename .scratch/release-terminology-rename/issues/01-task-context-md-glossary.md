Type: task
Status: resolved

## Question

Apply the already-settled terminology decision to the canonical glossary, `CONTEXT.md` (root) and
its trilingual mirrors `docs/ru/CONTEXT.md`, `docs/zh/CONTEXT.md` (per ADR 0006 — keep meaning in
sync, translate, don't just copy English).

1. **`Release` (branch family) entry** → rename the term itself to **`Track`**. Rewrite the
   definition prose to drop "Release" and use "Track" throughout (the underlying facts don't
   change: one `cxx_ci_demo/<config_name>/` subtree in `ci-infra`, own TeamCity subproject/VCS
   roots/build configs, shared GitLab repos, differ by which branch each VCS root watches). Keep
   the cross-reference to `docs/en/adding-a-release.md` (that file's name is unchanged — see the
   map's Out of scope).
2. **`Package variant` (planned) entry** → rename `optimized` to **`release`** in the entry text.
   Remove/rewrite the sentence "Deliberately not called 'release', to avoid colliding with the
   existing **Release** (branch family) term above" — that collision no longer exists once the
   branch-family term is `Track`. Keep `debug` unchanged.
3. Check the `_Avoid_` line under the old Release entry (currently warns against "build
   configuration" as too vague) — decide whether it still reads sensibly under the new `Track`
   heading and adjust if not.

This is faithful execution of an already-resolved decision, not a fresh one — no need to re-grill
the term choice. If translating into `ru`/`zh` surfaces a genuine ambiguity the English resolution
didn't anticipate, stop and flag it rather than guessing.

## Answer

Applied in `CONTEXT.md`, `docs/ru/CONTEXT.md`, `docs/zh/CONTEXT.md`:

- `**Release** (branch family)` → `**Track** (branch family)`, prose reworded ("Tracks differ
  purely in which branch...").  `_Avoid_` line extended: `release` is now avoided *here* (it's the
  package-variant term below); the old "build configuration" caveat kept, reworded for "track".
- `config_name` entry: "The release's name" → "the track's name"; the worked example
  (`release_2_0`) kept as a literal identifier, just re-labelled "track `release_2_0`" instead of
  "release `release_2_0`".
- `Package variant`: `optimized` → `release` everywhere in the entry, including the
  `CMAKE_BUILD_TYPE=RelWithDebInfo` mapping sentence. Added a parenthetical explicitly
  disambiguating from CMake's own `CMAKE_BUILD_TYPE=Release` value (same word, different thing) —
  this addresses the minor ambiguity the charting session flagged and accepted (Q3), by naming it
  in the glossary rather than leaving it implicit. Removed the now-obsolete "deliberately not
  called release, to avoid colliding with Release" sentence and its matching `_Avoid_: release`
  line — the collision it warned about no longer exists.
- No `ru`/`zh`-specific ambiguity surfaced; both mirrors translate the same changes made to the
  English original, keeping `Track`/`Bootstrap`/`config_name` etc. as untranslated identifiers per
  this glossary's existing convention.
