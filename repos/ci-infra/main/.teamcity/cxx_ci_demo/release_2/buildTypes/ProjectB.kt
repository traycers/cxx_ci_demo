import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object Release2Track_ProjectB : BuildType({
    id((Release2TrackId / "ProjectB").toString())
    templates(Release2Track_BaseBuild)
    name = "project_b"

    params {
        param("build_image_cxx", "cxxci-build:${Release2TrackName}-${Release2Track_BuildCImage.depParamRefs.buildNumber}")
    }

    vcs {
        root(Release2Track_ProjectBVcs, "%vcs_rules%")

        cleanCheckout = true
    }

    triggers {
        vcs {
            id = "TRIGGER_2"
            enableQueueOptimization = false
        }
        finishBuildTrigger {
            id = "TRIGGER_4"
            buildType = "${Release2Track_BuildCImage.id}"
            successfulOnly = true
            branchFilter = ""
        }
    }
})
