import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object Track2_ProjectB : BuildType({
    id((Track2Id / "ProjectB").toString())
    templates(Track2_BaseBuild)
    name = "project_b"

    params {
        param("build_image_cxx", "cxxci-build:${Track2TrackName}-${Track2_BuildCImage.depParamRefs.buildNumber}")
    }

    vcs {
        root(Track2_ProjectBVcs, "%vcs_rules%")

        cleanCheckout = true
    }

    triggers {
        vcs {
            id = "TRIGGER_2"
            enableQueueOptimization = false
        }
        finishBuildTrigger {
            id = "TRIGGER_4"
            buildType = "${Track2_BuildCImage.id}"
            successfulOnly = true
            branchFilter = ""
        }
    }
})
