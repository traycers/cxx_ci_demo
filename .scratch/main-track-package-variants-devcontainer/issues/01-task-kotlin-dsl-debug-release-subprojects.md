Type: task
Status: resolved

## Question

Restructure `main` track's Kotlin DSL (`repos/ci-infra/main/.teamcity/cxx_ci_demo/main/`) so the active chain (`project_a`, `project_c`, `project_d`, `project_e`) builds in two package variants, each as its own child TeamCity subproject of `Main`:

- `Main_Debug` and `Main_Release` — each gets its own `BaseBuild` template (copy of today's, with `build_type` hardcoded to `"Debug"`/`"RelWithDebInfo"` respectively instead of the current single hardcoded `"RelWithDebInfo"`), and its own `ProjectA`/`ProjectC`/`ProjectD`/`ProjectE`/`Result` build types.
- Every artifact-dependency inside a subproject points at the variant-matched upstream build type only (e.g. `Main_Debug_ProjectA` depends on `Main_Debug_ProjectC`/`Main_Debug_ProjectD`, never the `Release` ones) — this is the entire reason for duplicating build types instead of parameterizing one, since `buildRule = sameChain()` disambiguates by which `BuildType` object the `dependency(...)` call targets, not by a parameter value.
- `Main_BuildCImage` is **not** duplicated — stays a single build type at the `Main` level, shared by both subprojects (the root image doesn't vary by package variant; see ticket 02 for its own naming-scheme changes).
- `Main_Release`'s `Result`: same shape as today's `Main_ResultBuild` (files-checking/protection/signing placeholder steps + "creating an installer" copy of `deps_dir/bin/*` into `install_dir` + `artifactRules = "%install_dir% => result.zip"`).
- `Main_Debug`'s `Result`: a single placeholder step (`echo "Hello"`), and `install_dir` for this subproject set to the same path as `deps_dir` (not the shared `_install`/`_deps` split `Main` uses today) so `artifactRules = "%install_dir% => result.zip"` archives everything the artifact-dependencies already unpacked there, with no copy step.
- Registration: `Main.kt` needs `subProject(...)` entries for the two new child projects instead of (or alongside — check whether any of today's un-varianted build types still make sense to keep registered directly under `Main`) today's flat `buildType(...)` list.
- Naming/id convention: follow this repo's existing pattern for nested subprojects (see how `CxxCiDemo` → `Main` itself is structured) — likely `Main_Debug_ProjectA` etc. for ids/object names, `MainDebugId`/`MainReleaseId` val-style prefixes. Watch for the doubled-prefix bug this repo has hit twice before (see `adding-a-track.md`'s warning about `IdPath`/literal `id(...)` string args vs. renamed vals).

Validate what can be validated without a live stand: Kotlin DSL compiles (`.teamcity` local compile check, same mechanism used in the prior maps here), object/id names don't collide, `sameChain()`/artifact-dependency wiring reads correctly project-by-project.

## Answer

Implemented under `repos/ci-infra/main/.teamcity/cxx_ci_demo/main/`:

- **`debug/`** and **`release/`** subdirectories, each with `Main{Debug,Release}.kt` (the `Project` object, id `MainId / "Debug"` / `MainId / "Release"`), `templates/BaseBuild.kt` (`build_type="Debug"`/`"RelWithDebInfo"`, otherwise identical script), and `buildTypes/{ProjectA,ProjectC,ProjectD,ProjectE,Result}.kt`. Every dependency inside a subproject points at its own variant's sibling (`Main_Debug_ProjectA` depends on `Main_Debug_ProjectC`/`Main_Debug_ProjectD`, never `Release`'s).
- `Main.kt` now registers `subProject(Main_Debug)`/`subProject(Main_Release)` instead of the old flat `buildType(ProjectA/C/D/E/Result)` list; `Main_ProjectB` (paused) and `Main_BuildCImage` (shared root image, ticket 02's territory) stay directly under `Main`, along with the original `templates/BaseBuild.kt` (now only used by `Main_ProjectB`).
- `Main_Release_Result`: unchanged shape from today's `Main_ResultBuild`. `Main_Debug_Result`: single `echo "Hello"` placeholder step, `install_dir` overridden **at the BuildType level** (not the `Main_Debug` Project level — that would've leaked into `ProjectA/C/D/E` and broken ADR 0009's flat-not-transitive per-project sdk.zip separation) to `%deps_dir%`, so `artifactRules = "%install_dir% => result.zip"` archives exactly what the artifact-dependencies unpacked, no copy step.
- Old flat `buildTypes/{ProjectA,ProjectC,ProjectD,ProjectE,Result}.kt` deleted (`git rm`) — superseded by the subproject copies.

Validated without a live stand: no Java/Maven toolchain available in this environment to run the actual Kotlin compiler (the repo's `.teamcity/pom.xml` resolves its DSL plugin repository from a live TeamCity server, per ADR 0004 — nothing to substitute locally). Instead did a full manual pass: every `object` name and every top-level `val`-bearing filename checked unique across the *entire* `.teamcity/` tree (not just `main/`) — no collisions. Real Kotlin-compiler validation is deferred to ticket 07 (live verification).

One thing flagged for ticket 03: `main/` now has **three** top-level-`val` files (`Main.kt`, `debug/MainDebug.kt`, `release/MainRelease.kt`), not one — `new-track.sh` needs to rename all three for a clone to avoid a duplicate-JVM-class-name collision with `main/`'s own files. Ticket 03 updated with this detail.
