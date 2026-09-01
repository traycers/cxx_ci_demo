Type: task
Status: resolved
Blocked by: 01, 02, 04

## Question

Update the trilingual docs (en/ru/zh, per ADR 0006) now that package variants and the dev container are real for `main`, not just planned:

- `CONTEXT.md` (+ `docs/ru/CONTEXT.md`, `docs/zh/CONTEXT.md`): the "Package variant" and "Dev container image" glossary entries are currently marked "(planned — see roadmap.md)". Update to reflect they're implemented for `main` (still planned/not yet rolled out for `release_1`/`release_2`/`release_3`) — word this precisely, don't just delete "planned".
- `docs/{en,ru,zh}/roadmap.md`: update the "Package variants" and "Dev container image"/"Phase 1" sections similarly; add a short note that for this demo stand, no registry is used (shared `docker.sock` is sufficient — see ADR 0002's logic, extended) and that a real registry stays a documented future option, not a current gap.
- Check `docs/{en,ru,zh}/tracks.md` for whether `main`'s entry needs a mention of the new debug/release subprojects.
- Check `docs/{en,ru,zh}/developer-flow.md` — it currently says the download/build scripts aren't implemented (`tradeoff.md` defect 4); the dev container is now one (but not the only) piece of that target process — word carefully, don't overclaim the scripts now exist.
- Follow this repo's established convention (see ADR 0010, confirmed again in the prior `release-terminology-rename` map): only forward-looking docs get updated, historical ADRs are never rewritten.

## Answer

All ×3 languages (en/ru/zh), all edited directly (not machine-translated from the English pass afterward):

- `CONTEXT.md` + mirrors: "Package variant" and "Dev container image" entries reworded from "(planned)" to "(implemented for track `main`; still planned for `release_1`/`release_2`/`release_3`)", linked to ADR 0013, with the `debug`/`RelWithDebInfo` mapping description no longer hedged as future-tense.
- `roadmap.md` + mirrors: added an "Implemented for track `main`" paragraph under both the "Package variants" and "Dev container image" sections; corrected the registry paragraph specifically (it previously stated a registry was needed — turned out not to be true for this single-host demo stand, since the developer and the TeamCity agent share one Docker daemon; reworded rather than deleted, since the reasoning for *when* a registry would still matter is still valuable to keep). Added a short paragraph to Phase 1 noting the dev container/debug archive are real for `main` now, while being careful not to imply the clone/build scripts exist (they still don't).
- `tracks.md` + mirrors: one added sentence on `main`'s entry noting it's currently the only track with package variants/a dev container, linked to ADR 0013.
- `developer-flow.md` + mirrors: one added sentence clarifying the dev container + debug archive are real for `main`, the fetch/switch and build scripts are the part still missing — same "don't overclaim" boundary as the roadmap edit.
- Checked and left alone: `tradeoff.md` (its one dev-container mention is already just a forward-pointer to `roadmap.md`, no stale claim to fix) and every ADR (0002/0009/etc. — historical record, not touched, per ADR 0010's precedent).
