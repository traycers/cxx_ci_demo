import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs

// Root of the dependency tree (ADR 0002: no registry, shared docker daemon) — everything
// downstream snapshot-depends on this, directly or via base_build's template dependency.
//
// Dockerfile lives at cxx_ci_demo/main/Dockerfile — i.e. inside this track's own directory, so
// copying main/ to start a new track brings its Dockerfile along too (it can then diverge:
// different base image, different toolchain version, whatever that track needs). Checkout
// stays unscoped (the whole ci-infra tree, as before) — TeamCity rejects custom checkout rules
// on DslContext.settingsRoot for agent-side checkout ("Checkout rules are not supported for vcs
// root ... Unsupported rules for agent-side checkout", confirmed live) — so -f/context point at
// the track's own subdirectory explicitly instead.
//
// Image repository name is track-scoped (cxxci-<track_name>, not a shared "cxxci-build" with the
// track only in the tag) — see ADR 0013; `release_1`/`release_2`/`release_3` have since been
// migrated to the same `cxxci-<track_name>:...` scheme too (no `:latest`/dev image there — those
// are `main`-specific, see below). The floating `:latest` tag here is what Main_BuildDevImage's
// Dockerfile (main/Dockerfile.dev) builds FROM, so it needs to always point at this build's own
// image, not an older one from a previous chain run.
object Main_BuildCImage : BuildType({
    id((MainId / "BuildCImage").toString())
    name = "Build C++ image"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        script {
            name = "docker build"
            scriptContent = """
                docker build -t cxxci-${MainTrackName}:%build.number% -f .teamcity/cxx_ci_demo/${MainTrackName}/Dockerfile .teamcity/cxx_ci_demo/${MainTrackName}
                docker tag cxxci-${MainTrackName}:%build.number% cxxci-${MainTrackName}:latest
            """.trimIndent()
        }
        // No registry (ADR 0002) means every image this track ever built stays in the one
        // shared docker daemon forever unless something deletes it — confirmed live: 104 stray
        // tags accumulated before this step existed. Keeps the %keep_images_count% newest
        // cxxci-<track>:<N> images (by build number, not by age — a re-triggered old build
        // number wouldn't get pruned out from under a build that's using it), deletes the rest.
        // `latest` is explicitly excluded from the numeric sort (it's the floating alias to
        // whichever numbered image is newest, not itself a build number — sort -n would treat it
        // as 0 and either mis-rank or prune it, and it must never be pruned as if it were stale).
        // Only runs if "docker build" above succeeded (TeamCity's default: a failed required
        // step stops the build), so a failed build never prunes the still-good previous image.
        script {
            name = "cleanup old images"
            scriptContent = """
                docker images --format '{{.Tag}}' 'cxxci-${MainTrackName}:*' \
                    | grep -v '^latest${'$'}' \
                    | sort -n -r \
                    | tail -n +${'$'}(( %keep_images_count% + 1 )) \
                    | while read -r n; do docker rmi -f "cxxci-${MainTrackName}:${'$'}n"; done
                exit 0
            """.trimIndent()
        }
    }

    triggers {
        vcs {
            enableQueueOptimization = false
        }
    }
})
