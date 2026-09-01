import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object Track1_ProjectB : BuildType({
    id((Track1Id / "ProjectB").toString())
    templates(Track1_BaseBuild)
    name = "project_b"

    params {
        param("build_image_cxx", "cxxci-build:${Track1TrackName}-${Track1_BuildCImage.depParamRefs.buildNumber}")
    }

    vcs {
        root(Track1_ProjectBVcs, "%vcs_rules%")

        cleanCheckout = true
    }

    triggers {
        vcs {
            id = "TRIGGER_2"
            enableQueueOptimization = false
        }
        finishBuildTrigger {
            id = "TRIGGER_4"
            buildType = "${Track1_BuildCImage.id}"
            successfulOnly = true
            branchFilter = ""
        }
    }
})
