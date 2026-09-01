import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs


// Image repository name is track-scoped (cxxci-<track_name>, not a shared "cxxci-build" with the
// track only in the tag) — same rename applied to `main` first (see ADR 0013), extended here to
// this track for naming uniformity. No `:latest` floating tag or dev image here — those exist
// for `main` only, to feed its dev-container image; this track has no dev container.
object Release1Track_BuildCImage : BuildType({
    id((Release1TrackId / "BuildCImage").toString())
    name = "Build C++ image"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        script {
            name = "docker build"
            scriptContent = "docker build -t cxxci-${Release1TrackName}:%build.number% -f .teamcity/cxx_ci_demo/${Release1TrackName}/Dockerfile .teamcity/cxx_ci_demo/${Release1TrackName}"
        }
        // No registry (ADR 0002) means every image this track ever built stays in the one
        // shared docker daemon forever unless something deletes it — confirmed live: 104 stray
        // tags accumulated before this step existed. Keeps the %keep_images_count% newest
        // cxxci-Release1Track:* images (by build number, not by age — a re-triggered old build
        // number wouldn't get pruned out from under a build that's using it), deletes the rest.
        // Only runs if "docker build" above succeeded (TeamCity's default: a failed required
        // step stops the build), so a failed build never prunes the still-good previous image.
        script {
            name = "cleanup old images"
            scriptContent = """
                docker images --format '{{.Tag}}' 'cxxci-${Release1TrackName}:*' \
                    | sort -n -r \
                    | tail -n +${'$'}(( %keep_images_count% + 1 )) \
                    | while read -r n; do docker rmi -f "cxxci-${Release1TrackName}:${'$'}n"; done
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
