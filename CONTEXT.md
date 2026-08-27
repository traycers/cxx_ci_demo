🇬🇧 English · [🇷🇺 Русский](docs/ru/CONTEXT.md)

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
The one-shot automated script (and the `bootstrap/` directory, with a subdirectory per repo) that, after `docker compose up`, creates GitLab repos via its API and seeds them with initial content (DSL, demo projects).

**Demo project**:
A minimal skeleton C++ project (CMake) created for this map's end-to-end pipeline verification. One of the two demo projects depends on the other, to verify branch-based dependency resolution.

**Snapshot dependency**:
The TeamCity mechanism that guarantees a dependent build is triggered and taken from the same branch as the triggering build, falling back to the default branch if that branch doesn't exist in the dependency's VCS root.
_Avoid_: build trigger dependency

**Artifact dependency**:
The TeamCity mechanism that passes one C++ project's built binaries/headers to another for linking, without rebuilding from scratch.

**Release** (branch family):
One `cxx_ci_demo/<config_name>/` subtree in `ci-infra` — its own TeamCity subproject, its own VCS roots, its own set of build configurations, but the shared GitLab demo-project repos (`demo-project-a`/`demo-project-b`). Releases differ purely in which branch each VCS root watches (`branch_default`/`branch_spec`). See `docs/en/adding-a-release.md`.
_Avoid_: build configuration (too vague — conflicts with an individual project's build configuration inside a release)

**config_name**:
The release's name, used both as its directory name (`cxx_ci_demo/<config_name>/`) and as the base branch name in the demo projects (`refs/heads/<config_name>`). Branches derived from that release are named `<config_name>-*` (e.g. release `release_2_0` → branches `release_2_0`, `release_2_0-hotfix-1`).
