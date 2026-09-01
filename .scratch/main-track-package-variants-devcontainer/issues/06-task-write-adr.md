Type: task
Status: resolved
Blocked by: 01, 02

## Question

Write an ADR (`docs/{en,ru,zh}/adr/0013-...md`, following ADR 0010/0011's shape and this repo's numbering) recording the two hard-to-reverse, non-obvious decisions made in this map, once tickets 01/02 have actually landed (so the ADR reflects the real, final shape rather than the plan):

1. **Package variants as duplicated per-variant subprojects/build-types**, not a single parameterized build type — because `sameChain()` artifact-dependencies key off which `BuildType` object a `dependency(...)` call targets, not a parameter value, so a single parameterized build type couldn't guarantee a `Debug` build never links against a `Release` upstream. Record the alternative considered (parameterize one build type) and why it was rejected.
2. **Image-naming migration for `main`**: track name moves from the tag into the repository name (`cxxci-build:main-<N>` → `cxxci-main:<N>`), plus the new floating `:latest` tag and the separate `cxxci-main-dev:latest` — record why (uniform naming preference), and explicitly note this is scoped to `main` only for now, with `release_1`/`release_2`/`release_3` deliberately deferred to a separate future map (not because of any real collision — the old scheme's tag already disambiguated tracks fine).

## Answer

`docs/{en,ru,zh}/adr/0013-debug-release-subprojects-and-track-scoped-image-naming.md` — one ADR covering both decisions (they came out of the same charting session and are naturally paired, like several existing ADRs here that cover more than one clause of a single decision). Follows ADR 0012's shape (title-as-sentence, prose paragraphs, no extra sections — the codebase's actual convention runs longer/more detailed than the generic ADR-FORMAT.md skill template, so matched the repo's real precedent instead). Trilingual, all three written directly (not machine-translated afterward).
