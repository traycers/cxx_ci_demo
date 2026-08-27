import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs

// Root of the dependency tree (ADR 0002: no registry, shared docker daemon) — everything
// downstream snapshot-depends on this, directly or via base_build's template dependency.
//
// Dockerfile lives at cxx_ci_demo/main/Dockerfile — i.e. inside this release's own directory, so
// copying main/ to start a new release brings its Dockerfile along too (it can then diverge:
// different base image, different toolchain version, whatever that release needs). Checkout
// stays unscoped (the whole ci-infra tree, as before) — TeamCity rejects custom checkout rules
// on DslContext.settingsRoot for agent-side checkout ("Checkout rules are not supported for vcs
// root ... Unsupported rules for agent-side checkout", confirmed live) — so -f/context point at
// the release's own subdirectory explicitly instead.
object Main_BuildCImage : BuildType({
    id((MainId / "BuildCImage").toString())
    name = "Build C++ image"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        script {
            name = "docker build"
            scriptContent = "docker build -t cxxci-build:${MainConfigName}-%build.number% -f .teamcity/cxx_ci_demo/${MainConfigName}/Dockerfile .teamcity/cxx_ci_demo/${MainConfigName}"
        }
        // No registry (ADR 0002) means every image this release ever built stays in the one
        // shared docker daemon forever unless something deletes it — confirmed live: 104 stray
        // tags accumulated before this step existed. Keeps the %keep_images_count% newest
        // cxxci-build:main-* images (by build number, not by age — a re-triggered old build
        // number wouldn't get pruned out from under a build that's using it), deletes the rest.
        // Only runs if "docker build" above succeeded (TeamCity's default: a failed required
        // step stops the build), so a failed build never prunes the still-good previous image.
        script {
            name = "cleanup old images"
            scriptContent = """
                docker images --format '{{.Tag}}' 'cxxci-build:${MainConfigName}-*' \
                    | sed 's/^${MainConfigName}-//' \
                    | sort -n -r \
                    | tail -n +${'$'}(( %keep_images_count% + 1 )) \
                    | while read -r n; do docker rmi -f "cxxci-build:${MainConfigName}-${'$'}n"; done
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
