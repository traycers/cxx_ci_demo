Type: task
Status: resolved

## Question

Add `.devcontainer/` (and `.vscode/launch.json`) to each of `repos/project_a/main/`, `repos/project_c/main/`, `repos/project_d/main/`, `repos/project_e/main/` (the bootstrap seed content for `main`'s branch — see ADR 0007; this reaches the live GitLab repos only on re-bootstrap or a manual push, not immediately).

Required shape, per the original ask:

- `devcontainer.json` references image `cxxci-main-dev:latest` (from ticket 02) — assumes the developer's Docker context is the same shared daemon TeamCity builds on (this demo stand's existing ADR 0002 assumption, extended to the dev image).
- `"updateRemoteUserUID": true`.
- Container run with `--cap-add=SYS_PTRACE` (needed for the debugger to attach).
- Extensions (exact list — see map.md `Notes`/Destination): `mikestead.dotenv`, `usernamehw.errorl`, `earshinov.filter-lines`, `pkief.material-icon-theme`, `llvm-vs-code-extensions.vscode-clangd`, `vadimcn.vscode-lldb`, `ms-vscode.cmake-tools`, `akiramiyakoda.cppincludeguard`, `ajshort.include-autocomplete`, `danielpinto8zz6.c-cpp-compile-run`, `xaver.clang-format`.
- An install directory mounted **one level above the repo checkout** inside the container, added to `CMAKE_PREFIX_PATH` — this is where a developer would extract the CI-published debug archive (ticket 01's `Main_Debug`'s `Result`) to resolve the artifact-dependency chain locally, mirroring how CI's own `%deps_dir%` works. The actual download/extract step is a manual action for now (the automated clone/build scripts from `developer-flow.md` don't exist yet — out of scope here, see map.md). Decide bind-mount vs. named volume, and the exact mechanism for wiring `CMAKE_PREFIX_PATH` (env var via `containerEnv` — preferred, leaves the existing per-project `CMakePresets.json`/Conan flow untouched — vs. editing the presets); record the choice.
- `.vscode/launch.json` — a debug config using CodeLLDB (`vadimcn.vscode-lldb`, type `lldb`) targeting each repo's actual executable(s). `project_a` builds `app_a` (see its `CMakeLists.txt`/`app_a/` subdir); check `project_e` similarly (`app_e`/`app_e_core` — inspect its `CMakeLists.txt`). `project_c`/`project_d` need checking too — confirm whether they're library-only (no natural debug target, launch.json may not apply or should target their test binaries instead) before writing configs for them.
- The repo's existing per-project `.clangd`/`.clang-tidy`/`.clang-format` files already hold the actual tool configuration — this ticket only needs the devcontainer to have those tools installed (via ticket 02's dev Dockerfile) and VS Code wired to use them; it doesn't author new lint/format rules.

Depends on knowing the debugger backend from ticket 08's research for the exact `launch.json` config shape.

## Answer

Added `.devcontainer/devcontainer.json` and `.vscode/launch.json` to all four repos (`repos/project_{a,c,d,e}/main/`).

- **Investigated target shapes first**: `project_a`/`project_e` each have a real executable (`app_a`/`app_e`, `add_executable`) plus a test executable (`t_app_a`/presumably `t_app_e`); `project_c`/`project_d` are library-only (`vecopscale`/`vecutils`, `add_library(... STATIC ...)`) with only a test executable (`t_vecopscale`/`t_vecutils`) to actually run/debug — confirmed by reading each repo's `CMakeLists.txt` tree, not assumed.
- **`launch.json`** uses `"program": "${command:cmake.launchTargetPath}"` (per ticket 08's research) instead of hardcoding a preset-specific binary path — this resolves through the already-required `ms-vscode.cmake-tools` extension's active-target selector, so the *same* minimal config works uniformly whether the developer picks `app_a`, `t_app_a`, `t_vecopscale`, etc., without hardcoding which preset/binaryDir they're using.
- **`devcontainer.json`**: `"image": "cxxci-main-dev:latest"` (ticket 02), the exact given extension list, `"updateRemoteUserUID": true`, `"runArgs"` with `--cap-add=SYS_PTRACE` **and** `--security-opt seccomp=unconfined` — the latter wasn't in the original ask but ticket 08's research flagged a documented CodeLLDB-in-unprivileged-Docker failure (`'A' packet returned an error: 8`, an ASLR/seccomp issue) with that as one of the two documented fixes; added proactively rather than waiting to hit it live at ticket 07.
- **Install dir**: a named Docker volume (`cxxci-main-debug-install`, shared across all four repos' devcontainers) mounted at `/workspaces/_install` — a sibling of the default `/workspaces/<repo>` checkout location, i.e. "one level above the repo" inside `/workspaces/`, matching `roadmap.md` Phase 1's "install each into a directory at the root of the task directory". `containerEnv.CMAKE_PREFIX_PATH` points at it. Chose a named volume over a bind mount (no host path needs to exist in advance, portable) and `containerEnv` over editing `CMakePresets.json` (non-invasive — the existing `debug-asan`/Conan-based local preset flow in each repo is completely untouched).
- **Debugger tooling** (`clangd`/`clang-tidy`/`clang-format`/`lldb` vs `clang` for the compiler) lives in `Dockerfile.dev`, not here — this ticket only wires the devcontainer to use it; per-project `.clangd`/`.clang-tidy`/`.clang-format` config files already existed and weren't touched.

All 8 new JSON(C) files validated for syntax (comments stripped, parsed as JSON) — no errors.

Not validated live: whether VS Code Dev Containers can actually reach `cxxci-main-dev:latest` on the shared daemon from outside the TeamCity agent context, whether the named volume mounts cleanly, or whether the seccomp/ASLR fix is actually sufficient — all deferred to ticket 07.
