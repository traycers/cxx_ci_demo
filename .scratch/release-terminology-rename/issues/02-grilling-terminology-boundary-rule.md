Type: grilling
Status: resolved

## Question

Two docs mix the renamed concept (`Release`→`Track`) with real, unrenamed identifiers in the same
text, and need a precise rule for which is which before anyone edits them — guessing per-file risks
inconsistent results between `docs/en/`, `docs/ru/`, `docs/zh/`:

- **`docs/*/release.md`** — a from-scratch conceptual doc (title "# Release", ~20 lines of prose,
  two mermaid diagrams) explaining the branch-family mechanism in general terms. Its diagrams use
  branch-name labels that are **illustrative examples invented for the diagram**, not real repo
  identifiers: `release_1`, `release_2`, `hotfix_1`..`hotfix_4`, `feature_1`..`feature_3`,
  `release_client_x`, `special_feature`. (`release_1`/`release_2` happen to coincide with this
  repo's real config names, but the diagram doesn't know that — it's teaching the general
  mechanism, the same diagram would make sense in a repo that had never created a real release_1.)
- **`docs/*/adding-a-release.md`** — the concrete how-to, whose prose is *about* the general
  concept ("A **release**... is one `cxx_ci_demo/<config_name>/` directory...") but whose worked
  example uses real, unrenamed identifiers throughout: `release_2_0`, `Release20`, `Release20Id`,
  `Release20ConfigName`, `Main_ProjectA` → `Release20_ProjectA`, `scripts/new-release.sh`.

Open questions to resolve live (this is why it's a grilling ticket, not a task):

1. In `release.md`'s diagrams, do the **illustrative** branch labels (`release_client_x`,
   `hotfix_*`, `feature_*`, and the generic `release_1`/`release_2` used as teaching examples)
   get renamed to fit the new term (e.g. `track_client_x`, generic examples reworded to
   `track_1`/`track_2`), since they're invented for the doc and carry no real-world identifier
   constraint? Recommend: yes — nothing here is a real identifier, so nothing is exempt from the
   Out-of-scope carve-out.
2. In `adding-a-release.md`, confirm the boundary precisely: every plain-English sentence
   explaining *what a release/track is or does* → "track"; every literal identifier, filename, or
   code-adjacent string (`release_2_0`, `Release20*`, `new-release.sh`, `cxx_ci_demo/release_2_0/`)
   → stays exactly as-is, byte for byte. Walk a few of the doc's trickier sentences (e.g. the
   `## Branch naming convention` section, which uses the bare word "release" both as prose *and*
   inside a literal `refs/heads/(release_2_0)` pattern two lines apart) to confirm the rule
   produces an unambiguous edit for each one.
3. Does the doc's own **title/H1** ("Adding a new release") change to "Adding a new track", even
   though the file itself keeps the name `adding-a-release.md` (Out of scope)? Recommend: yes —
   the title is prose describing the concept, not a stand-in for the filename; a mismatched
   title/filename pair is expected and fine here (same pattern the map already accepts for
   `releases.md`'s title vs. its unchanged `## release_1` headers).

Record the settled rule precisely enough that tickets 03 and 04 can apply it mechanically without
re-litigating it per sentence.

## Answer

**Scope changed mid-resolution** — this ticket's original premise (prose renames, identifiers stay)
no longer holds. Walking the `## Branch naming convention` example surfaced that the `ConfigName`
Kotlin suffix (`Release1ConfigName` etc.) is purely a documentation-coined term, not a literal code
string — which led to deciding it should rename too (`ConfigName`→`TrackName`) — which in turn made
a suffix-only rename produce inconsistent mixed identifiers (`Release1TrackName`). Resolution: the
map's destination now includes a **full identifier-level rename**, repo-files-only, no live
GitLab/TeamCity push. See the map's updated Destination/Out-of-scope for the authoritative scope;
tickets 07–10 are new, covering the actual identifier work. Tickets 03–05 are revised in place.

Settled answers to this ticket's three original questions (still valid, now simpler since there's
no more prose/identifier split to maintain):

1. `release.md` diagram labels — **all renamed** (`release_client_x`→`track_client_x`,
   `hotfix_*`/`feature_*`/`special_feature` unaffected since they don't contain "release", generic
   `release_1`/`release_2` examples → `track_1`/`track_2`).
2. `adding-a-release.md` — **no boundary needed anymore**: prose *and* every worked-example
   identifier (`release_2_0`→`track_2_0`, `Release20`→`Track20`, `Release20ConfigName`→
   `Track20TrackName`, `scripts/new-release.sh`→`scripts/new-track.sh`) rename together, since the
   real script/DSL they illustrate is also being renamed (ticket 09/07). No more mismatch to
   reconcile.
3. Title **and file** rename: `# Adding a new release` → `# Adding a new track`,
   `adding-a-release.md` → `adding-a-track.md`. Same reasoning extends to `releases.md` →
   `tracks.md` (ticket 05) for the same consistency the user invoked for `ConfigName`.
