Type: task
Status: resolved

## Question

For `main` track only:

1. **Rename the root image scheme**: `Main_BuildCImage` currently does `docker build -t cxxci-build:${MainTrackName}-%build.number% ...`. Change to `cxxci-${MainTrackName}:%build.number%` (i.e. `cxxci-main:<N>`) — the track name moves from the tag into the repository name. Update every downstream `build_image_cxx` param reference (`ProjectA`/`ProjectB`/`ProjectC`/`ProjectD`/`ProjectE` — in both new `Main_Debug`/`Main_Release` subprojects from ticket 01) accordingly. `release_1`/`release_2`/`release_3` are untouched — they keep `cxxci-build:release_N-*` (separate future map).
2. **Add a floating `latest` tag**: after the `docker build` step, `Main_BuildCImage` also retags the image it just built as `cxxci-main:latest` (`docker tag cxxci-main:%build.number% cxxci-main:latest`) on every successful run.
3. **Fix the cleanup step** for the new pattern: today's `docker images --format '{{.Tag}}' 'cxxci-build:main-*' | sed 's/^main-//' | sort -n -r | ...` needs to (a) match `cxxci-main:*` instead, and (b) explicitly exclude the `latest` tag from the numeric sort/prune (it's not a build number — `sort -n` will not treat it usefully and it must never be pruned as if it were an old numbered build).
4. **New build type `Main_BuildDevImage`**, at the `Main` level next to `Main_BuildCImage` (snapshot-dependency on it): builds a Dockerfile `FROM cxxci-main:latest` that layers in a debugger, `clangd`, `clang-tidy`, `clang-format` (see ticket 08's research for the exact debugger package/backend), tags the result `cxxci-main-dev:latest` — single retained version, always overwritten, no build-number variant. Decide where the Dockerfile itself lives (likely alongside `cxx_ci_demo/main/Dockerfile`, e.g. `cxx_ci_demo/main/Dockerfile.dev`, given the existing convention that a track's Dockerfile lives inside its own directory).

Validate without a live stand: `docker build`/`docker tag` commands run correctly by hand against a locally-built image; DSL compiles; cleanup script logic reviewed/dry-run against a synthetic list of tags (including a `latest` entry) to confirm it survives.

Blocked by: nothing (can run in parallel with ticket 01, though both touch `main`'s DSL tree — coordinate/merge carefully if run concurrently).

## Answer

`Main_BuildCImage` (`buildTypes/BuildCImage.kt`) now builds `cxxci-main:%build.number%` and immediately `docker tag`s it `cxxci-main:latest` in the same step. The cleanup step matches `cxxci-main:*`, excludes `latest` via `grep -v '^latest$'` before the numeric sort/prune. All 9 `build_image_cxx` param definitions across `main/` (both `Main_Debug_*`/`Main_Release_*` project pairs, plus `Main_ProjectB`) updated from `cxxci-build:${MainTrackName}-...` to `cxxci-${MainTrackName}:...` via the same mechanical substitution — verified no leftover old-scheme references remain in `main/`. `release_1`/`release_2`/`release_3` untouched (still `cxxci-build:release_N-*`).

New `Main_BuildDevImage` build type (`buildTypes/BuildDevImage.kt`), snapshot-dependent on `Main_BuildCImage`, builds `cxx_ci_demo/main/Dockerfile.dev` (`FROM cxxci-main:latest`, adds `clangd`/`clang-tidy`/`clang-format`/`lldb`, creates a non-root `vscode` user with passwordless sudo for `updateRemoteUserUID` to remap) and tags the result `cxxci-main-dev:latest` — always overwritten, no build-number tag, no cleanup step needed since nothing accumulates. Registered in `Main.kt`.

Caught and fixed a real bug in my own first pass: initially wrote `${'$'}{MainTrackName}` (a literal-`$`-escape) instead of plain `${MainTrackName}` (actual Kotlin interpolation) in the new `docker build`/`docker tag` step — would have emitted the literal string `cxxci-${MainTrackName}:...` instead of `cxxci-main:...`. Fixed before considering this resolved.

Did not install `gdb` alongside `lldb` — went with just `lldb` to match the `vadimcn.vscode-lldb` extension, pending ticket 08's research (still running at the time of this ticket) confirming that's sufficient. Revisit `Dockerfile.dev` if ticket 08 finds otherwise.

**Addendum, after ticket 08 landed**: dropped the `lldb` apt package entirely — CodeLLDB bundles its own LLDB, no system package needed. Added `clang` instead (not just `clangd`/`clang-tidy`/`clang-format`) — `project_a`'s default local preset (`debug-asan`, `taskfile.yml`) inherits `clang.json` and needs a real `clang`/`clang++` compiler to configure, which this GCC-only-derived dev image otherwise wouldn't have.

Not validated against a live Docker daemon (none available in this environment) — `docker build`/`docker tag` command syntax reviewed by hand, deferred to ticket 07 for real execution.
