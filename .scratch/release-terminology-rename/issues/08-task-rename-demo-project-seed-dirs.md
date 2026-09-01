Type: task
Blocked by: 02
Status: resolved

## Question

Rename the demo-project branch seed directories: for each of `repos/project_a/`, `project_b/`,
`project_c/`, `project_d/`, `project_e/`, rename the `release_1/`, `release_2/`, `release_3/`
subdirectories to `track_1/`, `track_2/`, `track_3/` (15 directory renames total).

These are plain `mv`s — a prior check (during charting) found no file inside any of them contains
the literal branch-name string (only generic project source, e.g. `app_a/sources/main.cpp`'s
unrelated `int main()`), so no content edits are needed, just the rename. Confirm that's still true
before finishing (a quick `grep -rn "release_[123]"` across the renamed trees should come back
empty) — if a repo's content changed since charting and now does reference the old name, fix that
too rather than leaving a stale reference.

`main/` in each of the five is unaffected (same reasoning as ticket 07 — `main` was never a
`release_N`).

## Answer

15 directory renames done: `project_{a,b}/{release_1,release_2,release_3}` →
`track_{1,2,3}` (6 dirs), `project_{c,d,e}/release_3` → `track_3` (3 dirs) — matches the expected
asymmetry (only `project_a`/`project_b` existed back in `release_1`/`release_2`; `project_c/d/e`
were added for `release_3`'s dependency chain). Pre-check and post-check
`grep -rlE "release_[123]"` across all five repos both came back empty — no content edits needed,
confirmed still true since charting.
