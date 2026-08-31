🇬🇧 English · [🇷🇺 Русский](../ru/tradeoff.md) · [🇨🇳 中文](../zh/tradeoff.md)

# Tradeoff

The current way of building the C++ projects has the following characteristics.

##### Advantages

1. A clear feature-branch model: switch the repos to the new branch and the release "slice" is ready.
2. Environment setup via the system package manager.
3. Future releases can change their own dependency tree.
4. Fast creation of new releases, by copying the previous one.
5. Uniform build setup across projects, thanks to CMake.
6. TeamCity dependencies pass one project's built binaries/headers straight to another, without rebuilding from scratch, with fallback to the default branch.

##### Disadvantages

1. The feature-branch model degenerates into needing a branch in every repo the feature touches. In principle, branches are needed in every one of them anyway — while the feature is being built, CI is building both the feature branches and new branches cut from the default branch. Feature branches end up "aging" relative to the default branch, which risks build problems, mitigated by good backward compatibility.
2. Not every library ships through the system package manager. Such libraries need their own packages built by hand, and developers end up with differing environments: for example, CI installs gtest via `apt` in the build image, while a developer installs it locally via Conan — the environments already diverge at this step. Without alignment, static libraries built on CI may not fit a developer's environment. Developers need to configure an environment as close to CI's as possible. This is solved with Docker and a dev container, or a prepared virtual machine (Vagrant).
3. Keeping all repos in one directory makes developing new features harder — branches have to be switched across repos and build output from other branches cleaned up, which slows the build down after switching back. Solved with a per-task directory plus two helper scripts — see [`docs/en/developer-flow.md`](developer-flow.md) for details.
4. Sustaining this process will require writing and maintaining scripts (fetching and building the repos) across every branch, including old releases. There's a real chance these scripts degenerate into writing a package manager of their own. Solved by keeping them in one shared repo.
5. Install setup is done separately in every repo. With that many of them, making changes is poorly controlled. Solved by extracting the functions into a separate repo the others reference — but that adds developers one more external dependency to configure when setting up a CMake build. Ugly, but uniform!
6. TeamCity artifact dependencies are flat, not transitive: a build has to declare an explicit artifact dependency on every package it needs, not just the one it links directly — because `install_component`/`install_package_config` only ever package a project's own files, never a dependency's (see ADR 0009). Concretely, `project_a`'s build depends on artifacts from both `project_c` and `project_d`, even though `project_a` only calls into `project_c` directly. This has a sharp edge too: `cleanDestination` is a per-dependency flag, and the official docs don't spell out how TeamCity behaves when several dependencies with this flag share one `%deps_dir%`. So the convention (including in practice) is: enable the flag on exactly one dependency, not on every dependency sharing a destination. Solved by declaring one artifact dependency per package actually needed, with `cleanDestination` enabled on only one of them.
7. Releases are independent of each other: even a repo that hasn't changed from one release to the next still gets its own build run in every release. Builds can't be reused across releases, which means storage has to be sized for every release's build artifacts, not just the ones that changed.
