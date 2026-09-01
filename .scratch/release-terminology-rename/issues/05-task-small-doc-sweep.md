Type: task
Blocked by: 01
Status: resolved

## Question

**Revised per ticket 02** (scope expanded from prose-only to full identifier rename — this ticket
now also renames `releases.md` itself and its `## release_N` headers).

Update the remaining docs that mention the renamed concepts. For each, edit `en`, `ru`, and `zh`:

- **`docs/*/releases.md`** → rename the file itself to **`docs/*/tracks.md`**. Title and lead-in
  prose ("Releases" / "Список релизов" / "Release 列表", "The concrete releases that exist...") →
  "Track" wording. The `## release_1` / `## release_2` / `## release_3` section headers → `##
  track_1` / `## track_2` / `## track_3` (these name the same three tracks whose directories
  ticket 07/08 rename on disk — keep this doc's headers matching whatever those tickets actually
  land on). `## main` is unaffected (never contained "release"). Note: this file was just edited
  (added a `## release_3` section) in a prior session — re-read it fresh, don't work from a stale
  mental model of its contents. Update any inbound links to the old `releases.md` filename.
- **`docs/*/roadmap.md`** — the `## Package variants — optimized and debug` section: rename
  `optimized` to `release` throughout (heading included — becomes something like "release and
  debug"), and rewrite the sentence "Deliberately not called `release`, to avoid colliding with
  the existing **Release** (branch family) term... — `project_a/release` would be ambiguous
  between..." — that reasoning is now obsolete (the branch-family concept is `Track`); replace it
  with a short note that this is why the variant is now free to be called `release`, or drop the
  sentence if it no longer earns its place. Everywhere else in the doc that says "release image"
  meaning the root image build's docker image, or similar unrelated uses — check each one is
  actually about the branch-family/package-variant concepts before touching it, not just any
  appearance of the word.
- **`docs/*/tradeoff.md`** — the handful of plain "release(s)" mentions (feature-branch model,
  "future releases", "fast creation of new releases", "old releases", "Releases are independent of
  each other") → "track(s)".
- **`docs/*/developer-flow.md`** — one mention ("a binary references a library the release
  wouldn't actually ship") → "track".

Use ticket 01's resolved `CONTEXT.md` wording as the canonical phrasing to stay consistent with.

## Answer

- **`releases.md`→`tracks.md`** (all 3 languages): title, lead-in prose, and all four `## ` headers
  renamed (`release_1`→`track_1`, `release_2`→`track_2`, `release_3`→`track_3`, `## main`
  untouched). Re-read fresh per the ticket's own warning — confirmed it had the `## release_3`
  section from the prior session, migrated correctly.
- **`roadmap.md`**: `optimized`→`release` in the heading and bullet; the obsolete
  collision-avoidance sentence replaced with a short note pointing at why `release` is free now.
  Also caught and fixed a real latent bug the ticket didn't anticipate: "release image"/"release
  模板"/"release-шаблоне" mentions describing the *root image build* and `BaseBuild.kt` template
  (unrelated to the package variant) would have become actively misleading post-rename — a reader
  could misread "release image" as "the image for the `release` package variant" now that word
  means something else in this repo. Renamed those to "track's root image" / "track template"
  instead of leaving them as stray "release"s.
- **`tradeoff.md`**: all plain "release(s)" mentions (advantages 1/3/4, disadvantages 4/7) →
  "track(s)", in all 3 languages including the Russian original's existing phrasing/typos
  (left those alone, only swapped the term).
- **`developer-flow.md`**: the one mention → "track", all 3 languages.

No dangling links to the old `releases.md` filename anywhere (verified by grep after finishing).
Post-edit grep confirms every remaining "release" across these files is the intentional package
variant term, nothing missed.
