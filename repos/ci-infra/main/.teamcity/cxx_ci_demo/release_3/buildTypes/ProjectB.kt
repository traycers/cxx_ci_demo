import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object Release3Track_ProjectB : BuildType({
    id((Release3TrackId / "ProjectB").toString())
    templates(Release3Track_BaseBuild)
    name = "project_b"

    // release_3's chain is a -> c -> d (see ADR 0009); nothing in this track depends on project_b's
    // artifacts, unlike release_1/release_2 where project_a still depends on it directly. Paused
    // rather than removed so the build type — VCS root, triggers, everything — stays intact and
    // ready: unpause (paused = false) is the entire re-enable step if a consumer shows up again.
    // Paused only stops the triggers below from firing automatically; a snapshot dependency added
    // later would still be able to trigger this build without unpausing it first.
    paused = true

    params {
        param("build_image_cxx", "cxxci-${Release3TrackName}:${Release3Track_BuildCImage.depParamRefs.buildNumber}")
    }

    vcs {
        root(Release3Track_ProjectBVcs, "%vcs_rules%")

        cleanCheckout = true
    }

    triggers {
        vcs {
            id = "TRIGGER_2"
            enableQueueOptimization = false
        }
        finishBuildTrigger {
            id = "TRIGGER_4"
            buildType = "${Release3Track_BuildCImage.id}"
            successfulOnly = true
            branchFilter = ""
        }
    }
})
