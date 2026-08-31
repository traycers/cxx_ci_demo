🇬🇧 English · [🇷🇺 Русский](../ru/adding-a-release.md) · [🇨🇳 中文](../zh/adding-a-release.md)

# Adding a new release

A **release** (see `CONTEXT.md`) is one `cxx_ci_demo/<config_name>/` directory in `ci-infra` —
its own TeamCity subproject, its own VCS roots, its own set of build configurations, its own
`Dockerfile`, but the *same* GitLab repos as every other release (`project_a` through
`project_e`). Releases are distinguished by which branch each one's VCS roots watch, and by
their own docker image tag prefix (below) — nothing is copied or forked on the GitLab side. See
`releases.md` for the concrete releases that exist right now.

## The quick way: `scripts/new-release.sh`

```
scripts/new-release.sh <new_config_name> [source_config_name]
```

`source_config_name` defaults to `main`; pass an existing release's config name instead to branch
off it rather than off `main`. This does everything in "Steps" below mechanically — copies the
directory, renames every identifier and every string that needs it, and registers the result in
`cxx_ci_demo/CxxCiDemo.kt` — then prints what's still yours to do (push, run `bootstrap.sh`,
create the actual git branch, review the new `Dockerfile` if this release needs a different build
environment). It exists because doing the rename by hand is genuinely error-prone: writing this
procedure down and following it once already produced the exact "doubled prefix" mistake
described below, caught only by pushing to a live server and reading the compiled result.

Verified live end to end: ran the script to generate a `release_2_0` release from `main`, pushed
it, confirmed a clean compile, confirmed the resulting build types
(`CxxCiDemo_Release20_BuildCImage` etc.) were exactly right, built its image
(`cxxci-build:release_2_0-1`, correctly separate from `main`'s tags), then reverted — this doc
describes what the script does, not a theoretical procedure.

The rest of this document explains what the script automates, for when you need to do part of it
by hand or want to understand what changed.

## Branch naming convention

`config_name` is both the release's directory name and the base branch name in the demo
projects:

- The release's own branch: `refs/heads/<config_name>` (e.g. `refs/heads/release_2_0`) — this is
  what `branch_default` points at.
- Any derivative/feature branch for that release: `<config_name>-*` (e.g.
  `release_2_0-hotfix-1`, `release_2_0-new-cmake-flag`) — this is the second alternative in
  `branch_spec`.

So for a release named `release_2_0`, the two build-convention parameters read:

```kotlin
param("branch_spec", """
    +:refs/heads/(release_2_0)
    +:refs/heads/(release_2_0-*)
""".trimIndent())
param("branch_default", "refs/heads/release_2_0")
```

A branch that doesn't match either pattern isn't picked up by this release's VCS roots at all —
that's what keeps multiple releases from stepping on each other's branches in the same shared
project repos.

## Docker image tag convention

Every release builds its own root C++ image into the *same* shared docker daemon (ADR 0002 — no
registry). `%build.number%` alone isn't unique across releases, so `BuildCImage` tags its image
`cxxci-build:<config_name>-%build.number%` (e.g. `cxxci-build:release_2_0-14`), and every
downstream build type in that release builds the same prefixed tag to consume it
(`build_image_cxx` param). This is also why the `Dockerfile` lives *inside* the release's own
directory (`cxx_ci_demo/<config_name>/Dockerfile`) rather than at the repo root: each release can
diverge it — different base image, different toolchain — same as everything else about it.

No registry means no automatic garbage collection either — `BuildCImage`'s "cleanup old images"
step keeps the `keep_images_count` project parameter's newest tags for its own release's prefix
(`cxxci-build:<config_name>-*`) and deletes the rest after each successful image build. It's
scoped to the release's own prefix, so it never touches another release's images; the parameter
comes along automatically when you copy `main/`'s `params { }` block, nothing to rename there.

## What the script does (the manual procedure)

1. **Copy the directory.** `cxx_ci_demo/main/` → `cxx_ci_demo/<config_name>/` (e.g.
   `cxx_ci_demo/release_2_0/`), everything inside it included — this brings the `Dockerfile`
   along too, no separate step needed.

2. **Rename the project file to a unique basename.** The project file (`Main.kt` in `main/`) has
   a top-level `val ...Id = ...` line, not just an `object`. Kotlin wraps top-level
   vals/functions in a synthetic class named after the **file**, not its directory — two files
   both called `Main.kt` in different directories will fail to compile with `Duplicate JVM class
   name`. Rename it to the release's word in PascalCase, e.g. `Release20.kt` for `release_2_0`
   (snake_case → PascalCase: capitalize each `_`-separated segment, then join with no separator —
   that's what `scripts/new-release.sh`'s `to_pascal_case` does; keep any manual renaming
   consistent with it). Files that contain *only* `object` declarations (every other file in the
   tree) don't have this problem — their compiled class name is the object's name, already
   unique — so they can keep their generic basenames (`ProjectA.kt` etc.) across every
   release directory.

3. **Rename every object in the copy**, prefixing each with the release's word — Kotlin objects
   in this DSL all share one default package (no `package` declarations anywhere under
   `.teamcity/`, on purpose — see `IdPath.kt`), so `main`'s `Main_ProjectA`,
   `Main_BuildCImage`, `Main_ResultBuild`, `Main_BaseBuild`, `Main_ProjectAVcs`,
   `Main_ProjectBVcs` are already taken. This is the same constraint CMake's
   `add_library`/`add_executable` target names have — one flat global namespace, must be unique.
   `main`'s own objects are prefixed too (not left bare) specifically so that copying *from* any
   release works identically, `main` included — there's no special case. For `release_2_0`:

   | main/ (object name)     | release_2_0/ (object name)         |
   |--------------------------|-------------------------------------|
   | `Main`                   | `Release20`                        |
   | `MainId`                 | `Release20Id`                      |
   | `MainConfigName`         | `Release20ConfigName`              |
   | `Main_ProjectA`      | `Release20_ProjectA`           |
   | `Main_ProjectB`      | `Release20_ProjectB`           |
   | `Main_ProjectAVcs`   | `Release20_ProjectAVcs`        |
   | `Main_ProjectBVcs`   | `Release20_ProjectBVcs`        |
   | `Main_BuildCImage`       | `Release20_BuildCImage`            |
   | `Main_ResultBuild`       | `Release20_ResultBuild`            |
   | `Main_BaseBuild`         | `Release20_BaseBuild`              |

   **Do not** touch the plain word strings passed to `id(...)` calls (e.g.
   `id((MainId / "ProjectA").toString())`) beyond swapping `MainId` for `Release20Id` — that
   string is what `IdPath` composes into the actual TeamCity id (`CxxCiDemo_Release20_ProjectA`).
   Prefixing it too doubles up the word in the id (a real mistake made and caught while verifying
   this procedure live, twice: `CxxCiDemo_Main_Main_ProjectA` the first time round, matched
   again by `scripts/new-release.sh`'s own sanity check the second time). If you're renaming by
   hand, a plain `sed` pass over the whole word (`s/\bMain\b/Release20/g`) will hit these string
   literals too, because sed can't tell a Kotlin identifier from a string — that's exactly why the
   script does the `*Id`/`*ConfigName`/`<word>_`-prefix substitutions as separate, narrower passes
   instead of one blanket word replacement.

   Likewise **don't** touch the literal `.teamcity/cxx_ci_demo/` path segments in
   `buildTypes/BuildCImage.kt`'s `docker build -f .teamcity/cxx_ci_demo/${...ConfigName}/Dockerfile
   .teamcity/cxx_ci_demo/${...ConfigName}` — only the `${MainConfigName}` reference in there
   becomes `${Release20ConfigName}`, matching whatever you renamed the val to. The
   `.teamcity/cxx_ci_demo/` part is fixed (it's where every release directory actually lives
   relative to the repo root TeamCity checks out — `DslContext.settingsRoot` doesn't support
   custom checkout rules for agent-side checkout, confirmed live, so the checkout always is the
   whole `ci-infra` repo and the path into it has to be spelled out).

4. **In the project file** (`Release20.kt`): set `val Release20Id = CxxCiDemoId / "Release20"`,
   `val Release20ConfigName = "release_2_0"`, `name = Release20ConfigName`, and the
   `branch_default`/`branch_spec` params to the new `config_name` (see above).
   `gitlab_credentials_password` stays `password("gitlab_credentials_password", "")` — empty
   default, exactly like `main`'s. Don't hand-write a `credentialsJSON:...` value; that's
   server-specific and gets baked in by TeamCity itself, not something to copy between releases.

5. **Register it**: add `subProject(Release20)` in `cxx_ci_demo/CxxCiDemo.kt`.

6. **Push to `ci-infra`, watch it apply**, then run `bootstrap.sh` once so it injects the GitLab
   credential into the new release's VCS roots too (it loops over
   `CxxCiDemo_Main_ProjectA`/`B`/`C`/`D`/`E` today — extend that loop, or add the new
   release's VCS root ids, when this stops being a one-release demo).

7. **Create the actual branches** in the `project_a` through `project_e` GitLab repos:
   `refs/heads/<config_name>` at minimum, so the release's VCS roots have something to build.

## Verifying it worked

`GET /app/rest/projects/id:_Root/versionedSettings/status` should end on
`"Changes from VCS are applied to project settings"` for your commit's message/revision, not a
`Kotlin DSL compilation errors` warning. A failed compile does **not** touch the already-applied
tree — the previous release configurations keep running builds — so this is safe to iterate on
directly against the live `ci-infra` repo rather than a scratch project, and worth doing before
trusting a written-but-unpushed change.

If you ever remove a release (as this doc's own testing did, twice), also remove any
`.teamcity/patches/vcsRoots/<Prefix>_*.kts` / `.teamcity/patches/projects/<Prefix>*.kts` files
referencing it — TeamCity auto-commits these when a secret gets injected directly onto a VCS root
(see ADR 0004), and a stale one pointing at a now-deleted VCS root fails the *next* sync with
`Expected VCS root with id '...' not found`, blocking even an unrelated change until it's cleared.
