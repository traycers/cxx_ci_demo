# CodeLLDB debugger runtime — research findings

Research date: 2026-09-01. Sources consulted: CodeLLDB's own GitHub repo (`vadimcn/codelldb`) — `README.md`, `MANUAL.md`, `CHANGELOG.md`, GitHub Releases assets, and the project wiki (`vadimcn/codelldb.wiki`) — plus VS Code's own docs on extension platform-targeting and the Remote/Dev Containers extension architecture. This repo's own files (`repos/ci-infra/main/.teamcity/cxx_ci_demo/main/Dockerfile`, `repos/ci-infra/main/.teamcity/cxx_ci_demo/main/templates/BaseBuild.kt`, `repos/project_a/main/CMakeLists.txt`, `repos/project_a/main/app_a/CMakeLists.txt`, `repos/project_a/main/cmake/presets/**`) were read to ground the answers in this repo's actual toolchain and build layout rather than generic advice.

## TL;DR

1. **CodeLLDB does not require a system-installed `lldb`/`lldb-server` package.** It embeds/bundles its own LLDB (currently v22.1.8, per `CHANGELOG.md`'s `1.12.3` entry — "Updated bundled LLDB to v22.1.8") and ships a copy of `lldb-server` inside its own extension install directory (`MANUAL.md`: "A copy of lldb-server is provided in this extension's installation directory under `lldb/bin`"). Since VS Code Marketplace's platform-specific-extension mechanism, the current release model publishes one VSIX **per host platform** (`codelldb-linux-x64.vsix`, `codelldb-linux-arm64.vsix`, `codelldb-linux-armhf.vsix`, `codelldb-darwin-x64.vsix`, `codelldb-darwin-arm64.vsix`, `codelldb-win32-x64.vsix` — confirmed via `gh api repos/vadimcn/codelldb/releases/latest`, tag `v1.12.3`) with the matching LLDB binaries **already inside** that VSIX, plus a fallback `codelldb-bootstrap.vsix` for tooling that can't do platform-package negotiation. **Verified empirically, not just inferred**: `gh api .../releases/latest --jq '.assets[] | "\(.name) \(.size)"'` shows every per-platform VSIX is 45–55MB (e.g. `codelldb-linux-x64.vsix` = 55,571,094 bytes) while `codelldb-bootstrap.vsix` is 93,774 bytes — three orders of magnitude smaller, consistent only with the platform VSIXs actually containing the LLDB binaries and the bootstrap one containing none (it fetches them at first activation instead). See §1 for the offline/online split.
2. **Yes, CodeLLDB debugs GCC-compiled (DWARF) binaries fine** — it's LLDB-generic, not Clang/LLVM-binary-specific. This repo's root image (`main/Dockerfile`) installs `build-essential` (GCC/G++) and nothing from LLVM/Clang, so the binaries a devcontainer built from that image will actually produce are GCC/DWARF binaries — exactly the case CodeLLDB is designed for. See §2, with one real toolchain-mismatch flag worth carrying to ticket 02.
3. A minimal, repo-grounded `launch.json` for `project_a`'s `app_a` CMake executable target is given in §3, along with the more portable CMake-Tools-integrated form (`${command:cmake.launchTargetPath}`).
4. **A container-specific `launch.json` gotcha directly relevant to this devcontainer's own run config**: CodeLLDB's own troubleshooting docs flag that launching inside an unprivileged Docker container with ASLR-disabling blocked by the default seccomp profile fails with `process launch failed: 'A' packet returned an error: 8`, with two documented fixes — one of them a `launch.json` field (`"initCommands": ["set set target.disable-aslr false"]`, quoted verbatim from the wiki — likely meant as the `settings set` LLDB command). See §3's closing note; this map already plans `--cap-add=SYS_PTRACE` for the devcontainer, which is a different (also usually-needed) capability, not a substitute for this fix.

---

## 1. Bundled vs. system LLDB, and offline-vs-online conditions

### What CodeLLDB embeds

- **Architecture**: "CodeLLDB embeds LLDB by loading its dynamic library (`liblldb`) and driving the debugger through the API it exports" rather than spawning the `lldb` CLI tool (`MANUAL.md`, "Liblldb" section). It is not a thin wrapper around a system `lldb`.
- **`lldb-server` is bundled too**, not just `liblldb`: "A copy of lldb-server is provided in this extension's installation directory under `lldb/bin`" (`MANUAL.md`, "Debugging as a Different User" section) — used e.g. for CodeLLDB's own remote-debugging feature.
- The wiki's NixOS troubleshooting entry independently confirms the binaries live inside the *installed extension's own directory*, not some separate download cache: it references patching `$VSCODE/extensions/vadimcn.vscode-lldb-1.6.1/adapter/codelldb`, `.../lldb/bin/lldb`, and `.../lldb/bin/lldb-server` directly (`Debugger-startup-problems.md` wiki page).
- `CHANGELOG.md`'s `1.12.3` entry (current latest release) says "Updated bundled LLDB to v22.1.8" — current release terminology is "bundled," not "downloaded."

### How this got packaged: history matters for the offline question

- Originally (circa CodeLLDB v1.2, 2019) the extension shipped as a single generic VSIX and explicitly **downloaded** a platform-targeted native-binary package on first use, to keep the base install small: "native binaries will not be included in the initial installation package published on VS Code Marketplace. Instead, a smaller, platform-targeted package will be downloaded on first use." (`CHANGELOG.md`, "Heads up: CodeLLDB is moving to native code" note under `1.3.0`.) This is also what the wiki's `Setup.md` page still describes today (last edited Nov 2024): "Upon first extension activation (and after upgrades), CodeLLDB will automatically download its platform-specific native binaries (around 50MB). As of v1.6, no further setup should be needed."
- VS Code Marketplace later added first-class **platform-specific extension packages**: "Starting with version `1.61.0`, VS Code looks for the extension package that matches the current platform" and installs that variant directly, with a platform-independent package only used "as a fallback for all platforms that have no platform-specific package" ([Publishing Extensions — Platform-specific extensions](https://code.visualstudio.com/api/working-with-extensions/publishing-extension)).
- CodeLLDB's current GitHub Releases (`v1.12.3`, checked via `gh api repos/vadimcn/codelldb/releases/latest`) ship exactly this shape: `codelldb-linux-x64.vsix`, `codelldb-linux-arm64.vsix`, `codelldb-linux-armhf.vsix`, `codelldb-darwin-x64.vsix`, `codelldb-darwin-arm64.vsix`, `codelldb-win32-x64.vsix`, plus one `codelldb-bootstrap.vsix`. Their asset sizes (§TL;DR above) confirm the per-platform VSIXs contain the LLDB binaries directly, while `codelldb-bootstrap.vsix` (93KB) does not and must fetch them at first activation instead. The `bootstrap.vsix` is the modern equivalent of the old "generic package + download on first use" path — kept for installers/registries that can't do Marketplace-style platform negotiation (e.g. some Open VSX / generic `code --install-extension <file>.vsix` flows).

**Practical conclusion, now verified rather than inferred:** on a normal install (VS Code Marketplace resolves and installs the `linux-x64` or `linux-arm64` platform-specific package — both match this repo's `ubuntu:24.04` root image), no *additional* download of `lldb`/`lldb-server` binaries happens beyond installing the extension itself — they're already in that VSIX. Only the `bootstrap` fallback path (or the pre-2021 generic-package behavior some caches/mirrors may still be serving) does a genuine separate first-activation download.

### Where that leaves the "offline container build" question

The devcontainer's debugger extension runs as a **workspace extension**, not a UI extension: VS Code's own remote-extensions doc distinguishes "UI Extensions" ("always run on the user's local machine") from "Workspace Extensions" ("run on the same machine as where the workspace is located... When in a remote workspace... Workspace Extensions run on the remote machine/environment") ([Extension Host Architecture](https://code.visualstudio.com/api/advanced-topics/remote-extensions)) — and debugger extensions are Workspace Extensions. Concretely, for this repo's devcontainer:

- **At `docker build` time (offline, per the ticket's premise)**: nothing about CodeLLDB needs to be baked into the image. No system `lldb`/`lldb-server` apt package is required in `main/Dockerfile`'s derived dev image for CodeLLDB to work.
- **At container-attach / extension-install time (the actual "online" moment)**: when a developer's local VS Code (with the Dev Containers extension) attaches to a freshly built container and installs `vadimcn.vscode-lldb` from `devcontainer.json`'s `customizations.vscode.extensions`, the Dev Containers tooling downloads the correct platform VSIX **into the container** (a Workspace Extension is installed and runs container-side). This download happens from wherever VS Code Server lands, i.e. **the container itself needs outbound network access at that moment** (once, and again on version upgrades) — it does not need it at `docker build` time. This matches the map's `updateRemoteUserUID: true` / extension-list plan (`.scratch/main-track-package-variants-devcontainer/map.md`) which already assumes attach-time extension installation, not a pre-baked image.
- **If the container is expected to be fully air-gapped even after `docker build`** (no network at attach-time either): none of CodeLLDB's official docs describe a supported offline/pre-baked install path — there's no documented "drop this file at build time and CodeLLDB won't need to download/install anything." The two realistic options if that constraint is hard:
  1. Ensure the container has outbound HTTPS to the VS Code Marketplace/CDN just for that one-time extension install (matches VS Code's own general dev-container connectivity requirement — the Dev Containers FAQ states VS Code Server itself needs "outbound HTTPS (port 443) connectivity" to Microsoft's update/download hosts, and marketplace-sourced extensions need the same class of access), or
  2. Fall back to `lldb.library` pointing at a **system-installed** LLDB (e.g. `apt-get install lldb` in the Dockerfile, baked in at build time) as CodeLLDB's documented "alternate backend" (`MANUAL.md`, "Liblldb" / "Alternate LLDB Backends": "point **lldb.library** at the desired `liblldb` shared library (which must be v15.0 or later)"). This is the one documented way to make CodeLLDB fully independent of any network access at extension-install time — at the cost noted in the wiki: an externally-provided LLDB "will likely be old and won't have any Rust support" (`Debugger-startup-problems.md`) — irrelevant for this repo's pure-C++ demo projects.

None of CodeLLDB's own docs contain the phrase "air-gapped" or "offline" at all — this is inferred from (a) how the extension is packaged/installed (above) and (b) VS Code's general remote-extension and dev-container connectivity docs, not from a CodeLLDB-authored offline guide.

One more platform check worth closing the loop on: CodeLLDB's stated Linux host floor is "glibc 2.18+ ... for x86_64, aarch64 or armhf" (`README.md`, "Supported Platforms"). `main/Dockerfile`'s base, `ubuntu:24.04`, ships glibc 2.39 — comfortably above that floor, so there's no glibc-version blocker for running CodeLLDB inside this repo's dev image.

---

## 2. GCC (DWARF) vs. Clang/LLVM binaries

- CodeLLDB's own README and MANUAL never mention "GCC" or "Clang" by name at all (checked directly — zero occurrences in either file). Its stated language scope is compiler-agnostic: "it is usable with most other compiled languages whose compiler generates compatible debugging information" (`README.md`, "Languages" section) — the qualifier is about **debug-info format compatibility**, not about which compiler produced it.
- This tracks with the underlying architecture: CodeLLDB embeds `liblldb` and drives it through LLDB's own API (§1) — the same LLDB engine used standalone on Linux, which is a general-purpose ELF/DWARF debugger, not a Clang-emitted-DWARF-only tool. LLVM's own LLDB troubleshooting docs describe the debug-info requirement generically as "your source files were compiled with debug information. Typically this means passing `-g` to the compiler" ([LLDB Troubleshooting](https://lldb.llvm.org/use/troubleshooting.html)) — no compiler restriction stated there either.
- The one real-world caveat found (via the Fedora Project wiki's writeup of LLDB's DWARF-index support, not a CodeLLDB or LLVM-project source) is narrow and doesn't apply to default GCC output: LLDB is picky about **accelerator-table formats** some toolchains can optionally emit — GDB's `.gdb_index` "is compatible with GDB but incompatible with LLDB as it is missing essential DIE offsets needed by LLDB due to more effective (faster) reading of DWARF by LLDB," and GDB's `.debug_names` (producible via `gdb-add-index -dwarf-5`) "is non-conforming to DWARF-5 standard. LLDB expects DWARF-5 standard compliant .debug_names and therefore it is incompatible with this format" — its augmentation string is `"GDB\x00"` where Clang's conforming one is `"LLVM0700"` ([Fedora — Changes/DebugInfoLldbIndex](https://fedoraproject.org/wiki/Changes/DebugInfoLldbIndex)). Both of those are opt-in indexing features (`.gdb_index` needs an explicit `-Wl,--gdb-index` link flag or a separate `gdb-add-index` postprocessing step; plain `-g` never emits them), not what plain `gcc -g` / CMake's `RelWithDebInfo`/`Debug` build types produce. Plain GCC DWARF output (no explicit `.gdb_index`/`.debug_names` flags) is unaffected — this repo's CI/CMake presets don't pass any such flags (`repos/project_a/main/cmake/presets/**` only set `CMAKE_CXX_FLAGS`/`CMAKE_BUILD_TYPE`/toolchain file, nothing related to index sections). LLVM's own [LLDB Troubleshooting](https://lldb.llvm.org/use/troubleshooting.html) page, checked directly, states the debug-info requirement generically ("compiled with debug information... passing `-g` to the compiler") and does not itself discuss GCC/Clang or `.gdb_index`/`.debug_names` at all — it's cited above only for the general `-g` requirement, not for the accelerator-table caveat.
- **This repo's actual toolchain, confirmed by reading `main/Dockerfile` directly**:
  ```
  FROM ubuntu:24.04
  RUN apt-get install -y --no-install-recommends build-essential cmake ninja-build ca-certificates libgtest-dev libgmock-dev
  ```
  Only `build-essential` (GCC/G++) is installed — no `clang`/`llvm` package anywhere in this Dockerfile. Since the devcontainer's dev image is planned to be `FROM cxxci-main:latest` (built from this same root image, per `map.md`), binaries built inside that devcontainer using this repo's default toolchain will be **GCC-compiled, DWARF debug info** — precisely the case confirmed debuggable above.

- **One real mismatch to flag for ticket 02, found while grounding this**: `repos/project_a/main/CMakePresets.json`'s *local-dev* configure presets don't map compiler-to-build-type the way you'd expect from the CI image. `cmake/presets/configure/debug.json` inherits `clang.json` (so the `debug-asan`/`debug-tsan` local presets compile with **Clang**), while `cmake/presets/configure/relwithdebinfo.json` inherits `gcc.json` (so `relwithdebinfo-asan`/`relwithdebinfo-tsan` compile with **GCC**). Today this is moot because `BaseBuild.kt` hardcodes `build_type = "RelWithDebInfo"` for all CI builds (`main/templates/BaseBuild.kt`) — CI only ever exercises the GCC path, matching the GCC-only root image. But the map's plan (`map.md` §Destination) adds a `Main_Debug` package-variant subproject with `build_type` hardcoded to `Debug` — if that Debug variant's devcontainer/build environment reuses the local `debug` preset unmodified, it would need Clang, which the root image (and therefore the current dev-image plan, built `FROM cxxci-main:latest` with no separate Clang install called out) does not provide. This doesn't affect CodeLLDB itself (§2's answer holds regardless of compiler), but it is a real toolchain-provisioning gap worth a decision when ticket 02/the `Main_Debug` package variant is actually worked — not resolved here, since it's outside this ticket's three questions.

---

## 3. `launch.json` shape for a locally-built CMake executable

### CodeLLDB's documented minimal shape

`MANUAL.md`'s own minimal example (`# Starting a New Debug Session`):
```jsonc
{
    "name": "Launch",
    "type": "lldb",
    "request": "launch",
    "program": "${workspaceFolder}/<executable file>",
    "args": ["-arg1", "-arg2"],
}
```
Required top-level fields per the manual's attribute table: `name`, `type` (must be `"lldb"`), `request` (`"launch"` to start a new process, which is the case here). For `request: "launch"`, `program` is "Required unless you use `targetCreateCommands` or `cargo`" — a plain path to the executable is exactly what's needed for a normal CMake C++ target. Optional-but-commonly-used: `args`, `cwd` ("Working directory for the debuggee"), `env`, `stopOnEntry`, `terminal`. `sourceMap` is documented separately ("Source Path Remapping" section) for when "the program's source code is located in a different directory than it was at build time (for example, if a build server was used)" — relevant here only if you ever attach CodeLLDB to a binary built by CI's containerized build (`/work_dir`, `/shadow_build` paths baked into its DWARF, per `BaseBuild.kt`) rather than one built locally inside the devcontainer at the workspace's own path; for the normal devcontainer flow (build happens inside the container at the same path VS Code sees), no `sourceMap` is needed.

### Grounded against this repo's actual build layout

Two different "where's the binary" conventions exist in this repo, and they matter for which `program` value is correct:

- **CI's containerized build** (`BaseBuild.kt`, `Main_BaseBuild` template): inside the build container, `install_dir="/host_dir/%install_dir%"` and `build_dir="/shadow_build"`, with `%install_dir%` resolving (per `Main.kt`) to the literal param value `"_install"`. So CI's *install output* — what other package variants/projects consume as `%deps_dir%` via `find_pkgs`/`CMAKE_PREFIX_PATH` — lands at `<checkout>/_install/...`, not inside a `build/` directory at all. Since `install_component()` (`repos/project_a/main/cmake/files/install_component.cmake`) uses plain `install(TARGETS ...)` with no explicit `RUNTIME DESTINATION`, CMake's default `bin` destination applies, so the CI-installed executable would be at `_install/bin/app_a` (this only matters for a devcontainer scenario that debugs an artifact-dependency binary pulled in via `CMAKE_PREFIX_PATH`, not the project you're actively editing).
- **Local devcontainer build via `CMakePresets.json`** (the realistic devcontainer inner-loop case, per this map's plan of building inside the container against `.vscode`/CMake Tools): `project_a/main/CMakeLists.txt` calls `add_executable(${PROJECT_NAME} ...)` inside `add_subdirectory(app_a)`, where `PROJECT_NAME` = `app_a` (the folder name, via `get_filename_component`). No `RUNTIME_OUTPUT_DIRECTORY` is set anywhere in this repo's CMake files, so Ninja mirrors the source-tree layout under the preset's `binaryDir`. Only the *leaf* configure presets set an explicit `binaryDir` — e.g. `cmake/presets/configure/debug/asan.json`: `"binaryDir": "${sourceDir}/build/Debug/asan"`, and `cmake/presets/configure/relwithdebinfo/asan.json`: `"binaryDir": "${sourceDir}/build/RelWithDebInfo/asan"`. So for the `relwithdebinfo-asan` preset (the one that actually matches CI's GCC toolchain, per §2), the built executable ends up at:
  ```
  ${workspaceFolder}/build/RelWithDebInfo/asan/app_a/app_a
  ```
  and for `debug-asan` (Clang-toolchain locally, see §2's mismatch note):
  ```
  ${workspaceFolder}/build/Debug/asan/app_a/app_a
  ```

### Concrete `launch.json` entry for `app_a`, grounded in the above

```jsonc
{
    "name": "Debug app_a (RelWithDebInfo/asan)",
    "type": "lldb",
    "request": "launch",
    "program": "${workspaceFolder}/build/RelWithDebInfo/asan/app_a/app_a",
    "args": [],
    "cwd": "${workspaceFolder}"
}
```

### Container-specific gotcha: ASLR under the container's seccomp profile

CodeLLDB's own wiki (`Debugger-startup-problems.md`, "'A' packet returned an error" entry) documents a launch failure specific to running inside Docker: "Debugging inside an unprivileged Docker container with disabled [ASLR]... may be disallowed by the default container security profile," surfacing as `process launch failed: 'A' packet returned an error: 8`. Two documented fixes, either of which may turn out to be needed for this devcontainer once ticket 04 actually exercises it:
- Relax the container's security profile: `--security-opt seccomp=unconfined` on the container run.
- Or add to the launch config itself: `"initCommands": ["set set target.disable-aslr false"]` (quoted verbatim from the wiki — presumably meant as the `settings set target.disable-aslr false` LLDB command) to stop CodeLLDB from asking to disable ASLR in the first place.

This is a different knob from the `--cap-add=SYS_PTRACE` this map already plans for the devcontainer run (`map.md`) — `SYS_PTRACE` is what lets the debugger attach/ptrace at all; the seccomp/ASLR issue above is a separate, only-sometimes-triggered failure mode worth keeping in mind for ticket 04's actual `launch.json`/devcontainer run-args content, not something this ticket can resolve in the abstract (whether it actually triggers depends on the concrete container runtime/seccomp profile in use at verification time).

### More portable alternative (recommended for ticket 04): CMake Tools integration

Rather than hardcoding a preset-specific path (which changes if the active preset changes, e.g. switching to `debug-asan`), the `ms-vscode.cmake-tools` extension — already on this map's planned devcontainer extension list (`map.md`) — exposes the currently-selected CMake launch target's path as a VS Code command-variable, resolvable from any debugger's `program` field:
```jsonc
{
    "name": "Debug active CMake target",
    "type": "lldb",
    "request": "launch",
    "program": "${command:cmake.launchTargetPath}",
    "args": [],
    "cwd": "${workspaceFolder}"
}
```
This requires the user to have run a successful CMake configure and to have picked a debug target via CMake Tools' "CMake: Select a target to debug" command first ([CMake Tools — Target Debugging and Launching](https://github.com/microsoft/vscode-cmake-tools/blob/main/docs/debug-launch.md), [issue #461](https://github.com/microsoft/vscode-cmake-tools/issues/461)). Note: CMake Tools' own `debug-launch.md` doc's worked example uses `"type": "cppdbg"` with `"MIMode": "lldb"` (Microsoft C/C++ extension's own lldb-mi integration) rather than CodeLLDB's native `"type": "lldb"` — but `${command:cmake.launchTargetPath}` is a generic VS Code command-variable substitution, not something specific to `cppdbg`; CMake Tools' docs separately confirm `cmake.debugConfig` lets you plug in "adapters like `lldb`, `codelldb`, or any other installed debug extension," so pairing `${command:cmake.launchTargetPath}` with `"type": "lldb"` is a supported composition, just not the literal copy-paste example in that one doc.

---

## Sources (primary)

- [`vadimcn/codelldb` — README.md](https://github.com/vadimcn/codelldb/blob/master/README.md)
- [`vadimcn/codelldb` — MANUAL.md](https://github.com/vadimcn/codelldb/blob/master/MANUAL.md) (sections: "Starting a New Debug Session", "Launching a New Process", "Source Path Remapping", "Debugging as a Different User", "Liblldb"/"Alternate LLDB Backends")
- [`vadimcn/codelldb` — CHANGELOG.md](https://github.com/vadimcn/codelldb/blob/master/CHANGELOG.md) (entries `1.12.3`, `1.5.0`, `1.3.0`'s "CodeLLDB is moving to native code" note)
- `gh api repos/vadimcn/codelldb/releases/latest` — release `v1.12.3` asset list (`codelldb-bootstrap.vsix`, `codelldb-darwin-arm64.vsix`, `codelldb-darwin-x64.vsix`, `codelldb-linux-arm64.vsix`, `codelldb-linux-armhf.vsix`, `codelldb-linux-x64.vsix`, `codelldb-win32-x64.vsix`), queried 2026-09-01
- `vadimcn/codelldb.wiki` (cloned via `gh repo clone vadimcn/codelldb.wiki`) — `Setup.md`, `Linux.md`, `Debugger-startup-problems.md`, `Troubleshooting.md`, `How-can-I-tell-if-a-binary-was-compiled-with-debug-symbols?.md`
- [VS Code — Publishing Extensions: Platform-specific extensions](https://code.visualstudio.com/api/working-with-extensions/publishing-extension)
- [VS Code — Extension Host Architecture / Remote Extensions (UI vs. Workspace extensions)](https://code.visualstudio.com/api/advanced-topics/remote-extensions)
- [VS Code — Dev Containers FAQ](https://code.visualstudio.com/docs/devcontainers/faq) (outbound HTTPS connectivity requirement for VS Code Server)
- [LLVM — LLDB Troubleshooting](https://lldb.llvm.org/use/troubleshooting.html)
- [Fedora Project wiki — Changes/DebugInfoLldbIndex](https://fedoraproject.org/wiki/Changes/DebugInfoLldbIndex) (`.gdb_index`/`.debug_names` compatibility caveats between GDB and LLDB)
- [Microsoft — vscode-cmake-tools: Target Debugging and Launching (`debug-launch.md`)](https://github.com/microsoft/vscode-cmake-tools/blob/main/docs/debug-launch.md), [issue #461](https://github.com/microsoft/vscode-cmake-tools/issues/461)
- This repo: `repos/ci-infra/main/.teamcity/cxx_ci_demo/main/Dockerfile`, `repos/ci-infra/main/.teamcity/cxx_ci_demo/main/Main.kt`, `repos/ci-infra/main/.teamcity/cxx_ci_demo/main/templates/BaseBuild.kt`, `repos/project_a/main/CMakeLists.txt`, `repos/project_a/main/app_a/CMakeLists.txt`, `repos/project_a/main/cmake/files/install_component.cmake`, `repos/project_a/main/CMakePresets.json` and `repos/project_a/main/cmake/presets/**`
