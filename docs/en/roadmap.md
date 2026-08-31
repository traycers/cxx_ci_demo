🇬🇧 English · [🇷🇺 Русский](../ru/roadmap.md) · [🇨🇳 中文](../zh/roadmap.md)

# Roadmap

This page is a vision for where CI could go next — none of it is decided architecture yet. Nothing here should be read as committed; once a real decision with real trade-offs is made on any of these points, it gets its own ADR, and this page gets updated to point at it.

## C++ package managers

Today's dependency setup relies on the system package manager inside the build image (`apt` — see [`tradeoff.md`](tradeoff.md), disadvantage 2), which is also where that disadvantage's environment divergence comes from: CI installs a library via `apt` while a developer might install the same one locally via Conan, and the two environments have already parted ways at that point.

Three candidates are on the table to replace or supplement that: **Conan**, **vcpkg**, and **Nix**. None is chosen. Nix is the current personal favorite, but that's a leaning, not a decision — a proper evaluation could still change the picture. Only after that research happens does this become an ADR.

## Package variants — `optimized` and `debug`

Today's CI already builds every demo project with `CMAKE_BUILD_TYPE=RelWithDebInfo` (see `BaseBuild.kt` in every release template) — but only to run tests, not as a reusable output. The plan is to turn that into a real, reusable **package variant**, and add a second one alongside it:

- **`optimized`** — today's `RelWithDebInfo`, kept for backward compatibility with the current build/test flow. Retains the last **5** builds.
- **`debug`** — a new `CMAKE_BUILD_TYPE=Debug` configuration, built specifically to be consumed by developers. Retains only the **last** build — developers who need an older debug build rebuild it themselves rather than CI storing a deep history nobody but the newest consumer needs.

Deliberately not called `release`, to avoid colliding with the existing **Release** (branch family) term (see `CONTEXT.md`) — `project_a/release` would be ambiguous between "the `optimized` variant" and "the `release_1`/`release_2` branch family."

## Dev container image

A Docker image, built on TeamCity `FROM` the root image build's image (see `CONTEXT.md` — this keeps any change made to the release image from being lost rather than re-derived from scratch), pushed to a Docker registry, and referenced directly in each demo project's `devcontainer.json`. Colleagues get a working dev container without ever building the image themselves — they just point `devcontainer.json` at it.

Which registry (GitLab's built-in Container Registry, since GitLab is already part of the stand, vs. a plain `registry:2` image from Docker Hub) is left open — that's an implementation-time decision, not a vision-level one.

This is the first artifact in this stand planned to go through a registry at all. [ADR 0002](adr/0002-no-registry-shared-docker-daemon.md) deliberately skipped a registry for the root image build, because every build shares one Docker daemon on one agent. That reasoning doesn't extend here: a dev container image has to reach developers' machines, not just sibling builds on the same agent, so daemon-sharing can't substitute for a registry the way it does for the root image build. This doesn't reverse ADR 0002 — the root image build stays registry-free — it's just the first case its reasoning was never meant to cover.

## Two phases of using the dev container

### Phase 1 — without a package manager

Buildable today, independently of any package-manager decision above. The developer creates a task directory (see [`developer-flow.md`](developer-flow.md)) and a script builds the chain of repositories, `cmake install`-ing each into a directory at the root of the task directory — practically a local mirror of how TeamCity itself builds today.

To speed that up, TeamCity publishes the `debug` variant as a downloadable archive — the same idea as today's **Artifact dependency** mechanism (see `CONTEXT.md`), just carrying `debug` binaries instead of being produced as a side effect of a project's own build. The developer downloads it and unpacks it into the task directory, then builds only the repository they actually need to touch.

Crucially, this keeps the flexibility that matters most for day-to-day development: the developer can still walk into any other repository in the chain, build it, and `install` it locally — picking up in-progress changes to a dependency, not just to the repo they started with.

### Phase 2 — with a package manager

Comes after the package-manager research above lands in an ADR. The developer clones only the target repository into the task directory. Inside the dev container, only that repository gets built; its `debug` dependencies come from the package manager instead of being cloned and built locally.

This trades away something Phase 1 has: there's no obvious way yet to override a package-manager-provided dependency with a locally built version when a change needs to span two repositories at once. That's an open problem, not a solved detail — it has to be worked out, on the developer's machine, before Phase 2 can fully replace Phase 1's flexibility.

Whether Phase 2 is even worth building is itself an open question to revisit only once Phase 1 has shipped and is in use — adopting a package manager is not a foregone conclusion.

## See also

- [`tradeoff.md`](tradeoff.md), disadvantage 2 — the environment-divergence problem this roadmap addresses.
- [`developer-flow.md`](developer-flow.md) — the per-task-directory workflow this roadmap's build script extends.
