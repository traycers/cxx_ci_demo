🇬🇧 English · [🇷🇺 Русский](docs/ru/CONTEXT.md) · [🇨🇳 中文](docs/zh/CONTEXT.md)

# CI CXX

Docker-compose demo CI stand: GitLab (VCS) + TeamCity (build server, single agent) build C++ projects inside Docker containers, with a root Docker-image build as the base dependency for the whole build tree.

## Language

**ci_cxx**:
The host-side repo that holds docker-compose and the bootstrap wiring for standing up the stand (GitLab + TeamCity). Contains no C++ code itself and is not TeamCity's CI configuration.
_Avoid_: project, monorepo

**ci-infra**:
The central repo inside GitLab holding TeamCity's Kotlin DSL (versioned settings) for the whole build tree — including the root image build's build configuration — and that image's Dockerfile.
_Avoid_: settings repo, teamcity repo

**Root image build**:
The TeamCity build configuration that builds the Docker image used for building C++ (from `ci-infra`'s Dockerfile) and is the root-most dependency of the entire build tree; rebuilding it triggers a rebuild of everything that depends on it.
_Avoid_: base build, image job

**Image tag**:
The TeamCity configuration parameter (`%build_image_cxx%`) holding the Docker image tag that downstream C++ project builds use to run their build container.

**Bootstrap**:
The one-shot provisioning container (`scripts/bootstrap/`, run via `docker compose run --rm bootstrap` — see ADR 0008) that, after `docker compose up`, creates GitLab repos via its API and seeds each one's branches with their initial content (DSL, demo projects) from `repos/<repo>/<branch>/` (one subdirectory per repo, each holding one subdirectory per pre-made branch — see ADR 0007).

**Demo project**:
A minimal skeleton C++ project (CMake) created for this map's end-to-end pipeline verification. Five exist (`a`–`e`): `a` chains through `c` into `d` to exercise multi-hop `install_package_config` resolution (see ADR 0009), `b` and `e` stand alone, with `e` deliberately self-sufficient (no dependency on any other demo project).

**Snapshot dependency**:
The TeamCity mechanism that guarantees a dependent build is triggered and taken from the same branch as the triggering build, falling back to the default branch if that branch doesn't exist in the dependency's VCS root.
_Avoid_: build trigger dependency

**Artifact dependency**:
The TeamCity mechanism that passes one C++ project's built binaries/headers to another for linking, without rebuilding from scratch.

**Track** (branch family):
One `cxx_ci_demo/<config_name>/` subtree in `ci-infra` — its own TeamCity subproject, its own VCS roots, its own set of build configurations, but the shared GitLab repos (`project_a` through `project_e`). Tracks differ purely in which branch each VCS root watches (`branch_default`/`branch_spec`). See `docs/en/adding-a-track.md`.
_Avoid_: `release` as a stand-in for this concept itself — that bare word now names the package variant (see below). A specific track may still be *named* `release_1`, `release_2`, etc. (see [ADR 0012](docs/en/adr/0012-release-instance-names-restored.md)) — the `track`/`repo`/`variant` path position disambiguates the two, not the word. Also avoid: build configuration (too vague — conflicts with an individual project's build configuration inside a track)

**config_name**:
The track's name, used both as its directory name (`cxx_ci_demo/<config_name>/`) and as the base branch name in the demo projects (`refs/heads/<config_name>`). Branches derived from that track are named `<config_name>-*` (e.g. track `track_2_0` → branches `track_2_0`, `track_2_0-hotfix-1`).

**Package variant** (implemented for track `main`; still planned for `release_1`/`release_2`/`release_3` — see [`docs/en/roadmap.md`](docs/en/roadmap.md) and [ADR 0013](docs/en/adr/0013-debug-release-subprojects-and-track-scoped-image-naming.md)):
The build-type qualifier — `release` or `debug` — that distinguishes a demo project's reusable build outputs, whether consumed as a downloadable archive (roadmap Phase 1) or, later, as a package manager reference (roadmap Phase 2, still unimplemented). `release` maps to `CMAKE_BUILD_TYPE=RelWithDebInfo` (per `BaseBuild.kt` — not the same as CMake's own `CMAKE_BUILD_TYPE=Release` value, despite the identical name), `debug` to `CMAKE_BUILD_TYPE=Debug`. On `main`, each variant is a full child TeamCity subproject (`Main_Debug`/`Main_Release`), not a parameter — see ADR 0013 for why.

**Dev container image** (implemented for track `main`; still planned for `release_1`/`release_2`/`release_3` — see [`docs/en/roadmap.md`](docs/en/roadmap.md)):
A Docker image, built `FROM` the root image build's image, that a demo project's `devcontainer.json` references directly so developers don't have to build it themselves. On `main`, built by `Main_BuildDevImage` as `cxxci-main-dev:latest` and consumed straight off the shared host Docker daemon (see ADR 0002) — no registry needed for this demo stand, since the developer and the TeamCity agent share the one daemon. A real registry stays a documented future option (`roadmap.md`), not a current gap — it would only matter if images ever needed to reach a machine outside that shared daemon.
