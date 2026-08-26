# Adding a new release

A **release** (see `CONTEXT.md`) is one `cxx_ci_demo/<config_name>/` directory in `ci-infra` —
its own TeamCity subproject, its own VCS roots, its own set of build configurations, its own
`Dockerfile`, but the *same* GitLab repos as every other release (`demo-project-a`,
`demo-project-b`). Releases are distinguished by which branch each one's VCS roots watch, and by
their own docker image tag prefix (below) — nothing is copied or forked on the GitLab side.

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
that's what keeps multiple releases from stepping on each other's branches in the same two
demo-project repos.

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

## Steps

1. **Copy the directory.** `cxx_ci_demo/main/` → `cxx_ci_demo/<config_name>/` (e.g.
   `cxx_ci_demo/release_2_0/`), everything inside it included — this brings the `Dockerfile`
   along too, no separate step needed.

2. **Rename the project file to a unique basename.** The project file (`Main.kt` in `main/`) has
   a top-level `val ...Id = ...` line, not just an `object`. Kotlin wraps top-level
   vals/functions in a synthetic class named after the **file**, not its directory — two files
   both called `project.kt` (or both called `Main.kt`) in different directories will fail to
   compile with `Duplicate JVM class name`. Rename it to match the release, e.g. `Release2_0.kt`.
   Files that contain *only* `object` declarations (every other file in the tree) don't have this
   problem — their compiled class name is the object's name, already unique — so they can keep
   their generic basenames (`DemoProjectA.kt` etc.) across every release directory.

3. **Rename every object in the copy**, prefixing each with the release's word — Kotlin objects
   in this DSL all share one default package (no `package` declarations anywhere under
   `.teamcity/`, on purpose — see `IdPath.kt`), so `main`'s `DemoProjectA`, `BuildCImage`,
   `ResultBuild`, `BaseBuild`, `DemoProjectAVcs`, `DemoProjectBVcs` are already taken. This is the
   same constraint CMake's `add_library`/`add_executable` target names have — one flat global
   namespace, must be unique. For `release_2_0`:

   | main/ (object name)  | release_2_0/ (object name)        |
   |-----------------------|------------------------------------|
   | `Main`                | `Release2_0`                      |
   | `MainId`              | `Release2_0Id`                    |
   | `MainConfigName`      | `Release2_0ConfigName`            |
   | `DemoProjectA`        | `Release2_0_DemoProjectA`         |
   | `DemoProjectB`        | `Release2_0_DemoProjectB`         |
   | `DemoProjectAVcs`     | `Release2_0_DemoProjectAVcs`      |
   | `DemoProjectBVcs`     | `Release2_0_DemoProjectBVcs`      |
   | `BuildCImage`         | `Release2_0_BuildCImage`          |
   | `ResultBuild`         | `Release2_0_ResultBuild`          |
   | `BaseBuild`           | `Release2_0_BaseBuild`            |

   **Do not** touch the plain word strings passed to `id(...)` calls (e.g.
   `id((MainId / "DemoProjectA").toString())`) beyond swapping `MainId` for `Release2_0Id` — that
   string is what `IdPath` composes into the actual TeamCity id (`CxxCiDemo_Release2_0_DemoProjectA`).
   Prefixing it too doubles up the word in the id (a real mistake made and caught while verifying
   this procedure live: `CxxCiDemo_Release2_0_Release2_0_DemoProjectA`).

   Likewise **don't** touch the literal `.teamcity/cxx_ci_demo/` path segments in
   `buildTypes/BuildCImage.kt`'s `docker build -f .teamcity/cxx_ci_demo/${...ConfigName}/Dockerfile
   .teamcity/cxx_ci_demo/${...ConfigName}` — only the `${MainConfigName}` reference in there
   becomes `${Release2_0ConfigName}`, matching whatever you renamed the val to. The
   `.teamcity/cxx_ci_demo/` part is fixed (it's where every release directory actually lives
   relative to the repo root TeamCity checks out — `DslContext.settingsRoot` doesn't support
   custom checkout rules for agent-side checkout, confirmed live, so the checkout always is the
   whole `ci-infra` repo and the path into it has to be spelled out).

4. **In the project file** (`Release2_0.kt`): set `val Release2_0Id = CxxCiDemoId / "Release2_0"`,
   `val Release2_0ConfigName = "release_2_0"`, `name = Release2_0ConfigName`, and the
   `branch_default`/`branch_spec` params to the new `config_name` (see above).
   `gitlab_credentials_password` stays `password("gitlab_credentials_password", "")` — empty
   default, exactly like `main`'s. Don't hand-write a `credentialsJSON:...` value; that's
   server-specific and gets baked in by TeamCity itself, not something to copy between releases.

5. **Register it**: add `subProject(Release2_0)` in `cxx_ci_demo/CxxCiDemo.kt`.

6. **Push to `ci-infra`, watch it apply**, then run `bootstrap.sh` once so it injects the GitLab
   credential into the new release's VCS roots too (it loops over
   `CxxCiDemo_Main_DemoProjectA`/`B` today — extend that loop, or add the new release's VCS root
   ids, when this stops being a one-release demo).

7. **Create the actual branches** in `demo-project-a`/`demo-project-b` GitLab repos:
   `refs/heads/<config_name>` at minimum, so the release's VCS roots have something to build.

## Verifying it worked

`GET /app/rest/projects/id:_Root/versionedSettings/status` should end on
`"Changes from VCS are applied to project settings"` for your commit's message/revision, not a
`Kotlin DSL compilation errors` warning. A failed compile does **not** touch the already-applied
tree — the previous release configurations keep running builds — so this is safe to iterate on
directly against the live `ci-infra` repo rather than a scratch project, and worth doing before
trusting a written-but-unpushed change.
