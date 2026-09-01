Type: task
Status: open
Blocked by: 01, 02, 03, 04

## Question

HITL: the user stands up a clean TeamCity/GitLab stand (`docker compose up` from scratch — sidesteps the current live stand's open, unrelated `dockerPull=false` bug, see map.md `Notes`), pushes/bootstraps the new `main`-track DSL and repo content from this map onto it, and the agent then verifies:

- `Main_Debug`/`Main_Release` subprojects both actually build the chain (`project_a` → `project_c` → `project_d`, `project_e` standalone) end to end, each producing the right `CMAKE_BUILD_TYPE`.
- `Main_Debug`'s `Result` produces a downloadable archive containing the expected unpacked artifact-dependency contents; `Main_Release`'s `Result` still produces `result.zip` as before.
- `Main_BuildCImage` produces both `cxxci-main:<N>` and a correctly-updated `cxxci-main:latest`; `Main_BuildDevImage` builds `cxxci-main-dev:latest` from it; the cleanup step doesn't prune `latest` or misbehave on the new tag pattern.
- A dev container opens successfully against `cxxci-main-dev:latest` in at least one of the four repos, with the debugger able to attach (`--cap-add=SYS_PTRACE` actually working) and `clangd`/`clang-tidy`/`clang-format` functional.

If the previously-seen `docker-pull` bug resurfaces on the clean stand, that's a fact to record (in session memory / a follow-up), not something this ticket is responsible for fixing — record what was and wasn't verifiable and why.
