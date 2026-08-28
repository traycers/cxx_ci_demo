import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs


object Release1_BuildCImage : BuildType({
    id((Release1Id / "BuildCImage").toString())
    name = "Build C++ image"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        script {
            name = "docker build"
            scriptContent = "docker build -t cxxci-build:${Release1ConfigName}-%build.number% -f .teamcity/cxx_ci_demo/${Release1ConfigName}/Dockerfile .teamcity/cxx_ci_demo/${Release1ConfigName}"
        }
        // No registry (ADR 0002) means every image this release ever built stays in the one
        // shared docker daemon forever unless something deletes it — confirmed live: 104 stray
        // tags accumulated before this step existed. Keeps the %keep_images_count% newest
        // cxxci-build:Release1-* images (by build number, not by age — a re-triggered old build
        // number wouldn't get pruned out from under a build that's using it), deletes the rest.
        // Only runs if "docker build" above succeeded (TeamCity's default: a failed required
        // step stops the build), so a failed build never prunes the still-good previous image.
        script {
            name = "cleanup old images"
            scriptContent = """
                docker images --format '{{.Tag}}' 'cxxci-build:${Release1ConfigName}-*' \
                    | sed 's/^${Release1ConfigName}-//' \
                    | sort -n -r \
                    | tail -n +${'$'}(( %keep_images_count% + 1 )) \
                    | while read -r n; do docker rmi -f "cxxci-build:${Release1ConfigName}-${'$'}n"; done
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
