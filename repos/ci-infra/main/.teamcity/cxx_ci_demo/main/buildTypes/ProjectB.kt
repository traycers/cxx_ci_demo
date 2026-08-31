import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object Main_ProjectB : BuildType({
    id((MainId / "ProjectB").toString())
    templates(Main_BaseBuild)
    name = "project_b"

    // main's chain is a -> c -> d (see ADR 0009); nothing in this release depends on project_b's
    // artifacts, unlike release_1/release_2 where project_a still depends on it directly. Paused
    // rather than removed so the build type — VCS root, triggers, everything — stays intact and
    // ready: unpause (paused = false) is the entire re-enable step if a consumer shows up again.
    // Paused only stops the triggers below from firing automatically; a snapshot dependency added
    // later would still be able to trigger this build without unpausing it first.
    paused = true

    params {
        param("build_image_cxx", "cxxci-build:${MainConfigName}-${Main_BuildCImage.depParamRefs.buildNumber}")
    }

    vcs {
        root(Main_ProjectBVcs, "%vcs_rules%")

        cleanCheckout = true
    }

    triggers {
        vcs {
            id = "TRIGGER_2"
            enableQueueOptimization = false
        }
        finishBuildTrigger {
            id = "TRIGGER_4"
            buildType = "${Main_BuildCImage.id}"
            successfulOnly = true
            branchFilter = ""
        }
    }
})
