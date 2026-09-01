import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object Release1Track_ProjectB : BuildType({
    id((Release1TrackId / "ProjectB").toString())
    templates(Release1Track_BaseBuild)
    name = "project_b"

    params {
        param("build_image_cxx", "cxxci-build:${Release1TrackName}-${Release1Track_BuildCImage.depParamRefs.buildNumber}")
    }

    vcs {
        root(Release1Track_ProjectBVcs, "%vcs_rules%")

        cleanCheckout = true
    }

    triggers {
        vcs {
            id = "TRIGGER_2"
            enableQueueOptimization = false
        }
        finishBuildTrigger {
            id = "TRIGGER_4"
            buildType = "${Release1Track_BuildCImage.id}"
            successfulOnly = true
            branchFilter = ""
        }
    }
})
