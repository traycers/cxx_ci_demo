Label: research:teamcity-branch-dependency

Resolves: `issues/03-research-teamcity-branch-dependency-resolution.md`

# TeamCity: resolving a build configuration dependency from the triggering build's branch

Sources consulted are all primary: the official TeamCity On-Premises documentation at
`jetbrains.com/help/teamcity/` (current version at time of writing: **2026.1**) and the
official generated Kotlin DSL API reference at `teamcity.jetbrains.com/app/dsl-documentation/`
(package `jetbrains.buildServer.configs.kotlin`). Every claim below is cited inline with the
exact URL it came from. Where a TeamCity support-forum thread is cited, it is explicitly
flagged as secondary/community, not documentation.

## 1. Exact feature name and version

There is **no single named "feature toggle"** for this behavior. It is two different
mechanisms depending on dependency type, and only one of them is user-configurable:

- **Snapshot dependencies**: branch propagation through a build chain is **automatic, built-in
  behavior of the build chain itself** — it is not a setting you turn on. The docs describe it
  under "Working with Feature Branches" as build-chain branch propagation based on matching
  **"logical branch name"**:

  > "If a build configuration with branches has snapshot dependencies on other build
  > configurations with branches, then when a build in a branch is triggered, the other builds
  > in the chain will also get the branch associated... The VCS roots of the builds can point to
  > different repositories, but the logical branch name must be the same. If this condition is
  > met, the branches with this name will be checked out and all the builds down the chain...
  > and all the builds up the chain... will be marked with the same branch."
  > — https://www.jetbrains.com/help/teamcity/working-with-feature-branches.html

  The fallback-to-default and match criteria are formalized in the "Suitable Builds" list on
  the Snapshot Dependencies page, which states a build is only reusable in a chain if:

  > "It must belong to the same or the default branch."
  > — https://www.jetbrains.com/help/teamcity/snapshot-dependencies.html

  This is inherent TeamCity build-chain/branch behavior, present in modern TeamCity (verified
  current as of the 2026.1 docs; branch-aware build chains date back to TeamCity 8–10 per
  community history, e.g. a TeamCity-10 behavior-change discussion —
  https://teamcity-support.jetbrains.com/hc/en-us/community/posts/203332310-Snapshot-Dependency-TeamCity-10-Behavior-Change,
  community/secondary, cited only for version context). There is **no DSL property** on
  `SnapshotDependency` to opt in/out of this — see §3.

- **Artifact dependencies**: the equivalent, user-facing setting is named exactly
  **"Build branch filter"** in the UI/docs:

  > "Build branch filter — allows setting a branch filter to limit source builds only to those
  > in the matching branches. If not specified, the default branch is used. This field appears
  > if the dependency has a branch specified in the VCS root settings."
  > — https://www.jetbrains.com/help/teamcity/artifact-dependencies.html

  In the Kotlin DSL there is no property literally named `branchFilter` on `ArtifactDependency`
  (confirmed against the full member list — see §3). The closest DSL equivalent is the `branch`
  parameter of the `BuildRule` factory functions (`lastFinished(branch)`,
  `lastSuccessful(branch)`, `lastPinned(branch)`, `tag(tag, branch)`) attached to
  `ArtifactDependency.buildRule`. **Caveat:** the fetched DSL doc text describes this parameter
  only as "branch to use" (a name), whereas the UI "Build branch filter" field is described as
  a *filter* ("allows setting a branch filter to limit source builds..."), which in TeamCity's
  general branch-filter syntax elsewhere (VCS triggers, `branchFilter` on `FinishBuildTrigger`)
  supports `+:`/`-:` wildcard rules. None of the fetched pages confirm whether the UI field and
  the DSL `branch` parameter accept the identical syntax, or whether the UI field is exposed in
  the DSL at all as a distinct property under another name. Treat "UI Build branch filter ==
  DSL `branch` parameter of `lastFinished`/`lastSuccessful`/`lastPinned`/`tag`" as a plausible
  but **unverified** mapping — see §3 for the safer, chain-based alternative that avoids relying
  on it.

## 2. Snapshot dependencies vs. artifact dependencies — do both need configuration?

**No — they are handled by two different mechanisms, and for this demo stand's "snapshot +
artifact together" setup, in practice only one explicit setting is usually needed:**

- **Snapshot dependency**: branch-matching is **automatic**. As long as configuration B's VCS
  root has a branch with the same *logical branch name* as A's triggering branch (and that
  branch isn't excluded by the VCS root's branch specification), TeamCity will build/reuse B on
  that branch. If no matching branch exists in B's VCS root, TeamCity falls back to B's default
  branch. No DSL code is required to get this — it is baseline TeamCity build-chain behavior.
  (https://www.jetbrains.com/help/teamcity/working-with-feature-branches.html,
  https://www.jetbrains.com/help/teamcity/snapshot-dependencies.html)

- **Artifact dependency**: **requires separate/additional configuration** to pull artifacts
  from the matching branch — it does **not** automatically inherit the branch the way snapshot
  dependencies do, *unless* the artifact dependency's `buildRule` is left at its default value.
  The DSL default for `ArtifactDependency.buildRule` is `sameChainOrLastFinished()`:

  > "buildRule — Rule for selecting a dependency build, when not specified
  > sameChainOrLastFinished is used"
  > — https://teamcity.jetbrains.com/app/dsl-documentation/root/artifact-dependency/index.html

  > "sameChainOrLastFinished() — Creates a build rule matching the build from the same build
  > chain or last finished build."
  > — https://teamcity.jetbrains.com/app/dsl-documentation/root/artifact-dependency/same-chain-or-last-finished.html

  Practical consequence for this demo stand: **because A also has a snapshot dependency on B
  (same build chain), the default `sameChainOrLastFinished()` artifact-dependency rule will
  pick up the *same build in the chain* that the snapshot dependency already resolved** — i.e.
  branch-matching "just works" for the artifact dependency too, for free, as long as
  `buildRule` is left unset/default. If the artifact dependency is configured standalone
  (no accompanying snapshot dependency, or a different `buildRule`), it must be set explicitly,
  e.g. `buildRule = lastFinished(branch = "%teamcity.build.branch%")`, to approximate the "same
  branch, else default" behavior — see §3 Case B. The plain UI "Build branch filter" field is
  *presumed* to map to this `branch` argument, but that mapping is not confirmed by the fetched
  docs (see caveat in §1) — for the paired snapshot+artifact case this demo stand actually uses,
  prefer §3 Case A (`buildRule = sameChain()`), which sidesteps the question entirely by
  inheriting the snapshot dependency's already-resolved branch.

## 3. Kotlin DSL syntax — ready to paste

Verified against the official generated DSL docs:
- `jetbrains.buildServer.configs.kotlin.SnapshotDependency` —
  https://teamcity.jetbrains.com/app/dsl-documentation/root/snapshot-dependency/index.html
  (properties: `onDependencyCancel`, `onDependencyFailure`, `reuseBuilds`, `runOnSameAgent`,
  `synchronizeRevisions` — **no branch-related property exists on this class**; confirms branch
  handling for snapshot dependencies is implicit build-chain behavior, not DSL-configurable).
- `dependencies { snapshot(...) { ... } }` function signatures —
  https://teamcity.jetbrains.com/app/dsl-documentation/root/dependencies/snapshot.html
- `jetbrains.buildServer.configs.kotlin.ArtifactDependency` —
  https://teamcity.jetbrains.com/app/dsl-documentation/root/artifact-dependency/index.html
- `dependencies { artifacts(...) { ... } }` function signatures —
  https://teamcity.jetbrains.com/app/dsl-documentation/root/dependencies/artifacts.html
- `lastFinished(branch: String? = null): BuildRule` — "branch to use, if not specified only
  builds from the default branch are matched" —
  https://teamcity.jetbrains.com/app/dsl-documentation/root/artifact-dependency/last-finished.html

**Precondition (both cases below) — applies to BOTH A's and B's VCS roots, not just B's:**
matching is on **logical branch name**, per the §1 quote ("the logical branch name must be the
same"). This is a two-sided constraint: if A's VCS root's `branchSpec` yields the logical name
`foo` for the physical branch `refs/heads/foo`, but B's VCS root's `branchSpec` is narrower
(e.g. only matches `refs/heads/feature/*`, or uses a capture group that produces a different
logical name for the same physical branch), the logical names won't match even though the
physical branch genuinely exists in both repos — and TeamCity will silently fall back to B's
default branch, indistinguishable from "the feature doesn't work." Confirmed against the
official DSL property list for `GitVcsRoot`
(https://teamcity.jetbrains.com/app/dsl-documentation/vcs/git-vcs-root/index.html), which shows
the relevant properties are named `branch: String?` ("The default branch name" — **not**
`defaultBranch`) and `branchSpec: String?` ("Branch specification... to use in VCS root"):

```kotlin
vcsRoot(AVcsRoot) {
    // ... url, etc.
    branchSpec = "+:refs/heads/*"   // A's feature branches must be exposed too, with the
                                     // SAME resulting logical branch names as B's, below
    // branch = "refs/heads/master" // GitVcsRoot's default-branch property; left as the
                                     // built-in refs/heads/master unless overridden (see §4)
}

vcsRoot(BVcsRoot) {
    // ... url, etc.
    branchSpec = "+:refs/heads/*"   // must produce the SAME logical names as A's root above
                                     // for the same physical branches, or matching silently
                                     // fails and falls back to B's default branch
    // branch = "refs/heads/master"
}
```

### Case A — A has BOTH a snapshot and an artifact dependency on B (this demo stand's setup)

```kotlin
// In the build configuration for A (the dependent build), e.g. .teamcity/settings.kts

dependencies {
    // Snapshot dependency on B: branch matching (same logical branch name, else B's
    // default branch) is automatic — TeamCity's build-chain behavior. No branch-related
    // property exists on SnapshotDependency; nothing further to configure here for
    // branch resolution itself.
    snapshot(BProject.BuildB) {
        onDependencyFailure = FailureAction.FAIL_TO_START
        onDependencyCancel = FailureAction.FAIL_TO_START
        // reuseBuilds = ReuseBuilds.ANY_BRANCH  // default; keep as-is unless you need
                                                  // to force reuse restricted to a branch set
    }

    // Artifact dependency on B: DO NOT override buildRule with a branch-based rule here —
    // that would make the artifact dependency independently search for "last finished build
    // in branch X", which can resolve to a DIFFERENT build than the one the snapshot
    // dependency above just built/reused, breaking sources/artifacts consistency. Instead,
    // either leave buildRule unset (defaults to sameChainOrLastFinished(), which — per the
    // DSL doc — matches "the build from the same build chain", i.e. exactly the build the
    // snapshot dependency resolved) or pin it to the strict same-chain rule explicitly:
    artifacts(BProject.BuildB) {
        buildRule = sameChain()   // "Creates a build rule matching the build from the same
                                   // build chain (strict)" — inherits whatever branch/default
                                   // the paired snapshot dependency already resolved.
        artifactRules = "+:*.tar.gz => artifacts/b"
        cleanDestination = true
    }
}
```

### Case B — standalone artifact dependency, with NO accompanying snapshot dependency on B

Only in this case does the artifact dependency need its own branch resolution, since there is
no chain build to inherit from:

```kotlin
artifacts(BProject.BuildB) {
    buildRule = lastFinished(branch = "%teamcity.build.branch%")
    // Uses the branch that triggered THIS (A's) build. Per the DSL doc, lastFinished()
    // falls back to "only builds from the default branch" when no branch is given — but
    // the exact behavior when the %teamcity.build.branch% value does NOT correspond to any
    // branch that exists for B's VCS root is not spelled out in the fetched docs; treat as
    // unverified (see §4) and validate empirically before relying on it.
    // NOTE: whether this `branch` parameter accepts the same +:/-: filter syntax as the UI's
    // "Build branch filter" field is unverified — see the caveat in §1.
    artifactRules = "+:*.tar.gz => artifacts/b"
    cleanDestination = true
}
```

`%teamcity.build.branch%` is TeamCity's standard predefined build parameter exposing the
current build's branch name; it is documented generally under Predefined Build Parameters
(`https://www.jetbrains.com/help/teamcity/predefined-build-parameters.html`) rather than on the
artifact-dependency page itself — flagged here since it wasn't quoted verbatim from the
artifact-dependency doc page, but its use as a parameter reference inside DSL string fields is
standard TeamCity parameter-reference syntax.

This demo stand (A snapshot+artifact depends on B, per `map.md`) should use **Case A**.

## 4. Edge case: no matching branch AND no default branch configured on B's VCS root

**This exact edge case is effectively unreachable through normal configuration, and the docs do
not explicitly spell out an error path for it** — flagged as ambiguous/undocumented:

- The VCS root "Default branch" field is **optional but always has a value** — TeamCity ships
  it with a built-in default of `refs/heads/master` if you don't set one explicitly:

  > "the default branch. Parameter references are supported here. Default value is
  > `refs/heads/master`."
  > — https://www.jetbrains.com/help/teamcity/git.html

  So in ordinary UI/DSL configuration there is always *some* default branch value configured
  on a VCS root — you cannot leave it truly empty through supported configuration paths.
  (A community thread mentions manually stripping it from XML config is possible but explicitly
  unsupported/not recommended by JetBrains —
  https://teamcity-support.jetbrains.com/hc/en-us/community/posts/360000116570-Leaving-out-Default-branch-when-creating-editing-a-VCS-Root,
  secondary source, cited only to note this isn't an officially supported path.)

- There is also a distinct, adjacent failure mode worth flagging: the default branch value is
  *configured* as a string (e.g. `refs/heads/master`, whether left at its built-in default or
  set explicitly) but does **not** correspond to an actual branch that exists in B's
  repository. This is likewise **undocumented** in the pages checked: neither
  `working-with-feature-branches.html`, `snapshot-dependencies.html`, nor
  `artifact-dependencies.html` describes the resulting error text or whether TeamCity fails the
  configuration, fails the build at trigger/checkout time, or silently skips the dependency —
  none of the fetched pages contain wording like "error", "fails to start", or "skipped" tied to
  either a totally-absent-default-branch situation or a configured-but-nonexistent one.

**Conclusion for the demo stand:** because a default branch is always present by default
(`refs/heads/master` unless overridden), the realistic edge case to actually test in ticket 09
is "triggering branch doesn't exist in B's VCS root, default branch does exist and gets used" —
which IS documented and is the fallback behavior described in §1/§2. The stricter case ("no
default branch at all") is not a configuration TeamCity's UI/DSL naturally produces, and its
behavior is undocumented in the primary sources checked here.

## Summary for ticket 07 (DSL) and ticket 09 (validation)

- Ticket 07: use the DSL snippet in §3 Case A as-is (plus the `branchSpec` precondition on B's
  VCS root); no extra property needed on `SnapshotDependency` for branch matching (it's
  automatic); only reach for Case B's `buildRule = lastFinished(branch = ...)` if an artifact
  dependency is ever added without an accompanying snapshot dependency on the same buildType.
- Ticket 09: the fallback scenario to validate end-to-end is "A runs on a feature branch that
  doesn't exist in B's VCS root → B's snapshot+artifact dependency both resolve to B's default
  branch build," per §1–§2. Do not attempt to validate the "no default branch at all" case — it
  isn't reachable via normal VCS root configuration (§4).
