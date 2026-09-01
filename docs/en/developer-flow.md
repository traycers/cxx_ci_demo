🇬🇧 English · [🇷🇺 Русский](../ru/developer-flow.md) · [🇨🇳 中文](../zh/developer-flow.md)

# Developer flow: one directory per task

Whenever a change is needed (a new feature or a bug fix), the developer creates a separate working directory for that task, instead of switching branches inside one shared checkout of the repos.

## Why not a shared directory

Keeping all repos in one shared directory makes developing new features harder: every repo's branch has to be switched at once, and build output from other branches has to be cleaned up by hand — otherwise the build after switching back is both slower (it rebuilds from scratch) and sometimes wrong. For example: a binary references a library the track wouldn't actually ship, but it still runs for the developer, because that library is still sitting on disk from a build of another, or older, branch.

## The fix: one directory per task

Every task (feature or bug fix) gets its own directory, with its own checkout of every repo it needs, on the branches it needs. That gives:

- **Switching tasks = launching the IDE from a different directory.** Nothing to switch by hand.
- **No rebuild needed after switching.** Each task always builds inside its own directory — the previous build is still there and never gets in the way of the next one.
- **No manual cleanup of stale files.** Since the directory is new, there's no leftover junk from other branches/builds in it from the start.
- **Few branches per directory.** Doing every task inside one shared checkout lets its branch list grow without bound and demands discipline to prune it; a directory per task naturally keeps that count small.
- **Trivial cleanup.** Once the task is done, the developer just deletes the whole directory.

## Tooling

The process rests on two scripts:

1. **A fetch/switch script** — clones (or updates) the needed repos into the task directory and switches them to the needed branches.
2. **A build script** — builds the projects inside the task directory.

As of this writing, neither script exists yet (see [`docs/en/tradeoff.md`](tradeoff.md), disadvantage 4) — this describes the target workflow to get to, not the current state. See [`docs/en/roadmap.md`](roadmap.md) for how the build script is planned to evolve — a dev container plus debug package variants.
