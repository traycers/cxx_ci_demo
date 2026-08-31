import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object Release1_ProjectB : BuildType({
    id((Release1Id / "ProjectB").toString())
    templates(Release1_BaseBuild)
    name = "project_b"

    params {
        param("build_image_cxx", "cxxci-build:${Release1ConfigName}-${Release1_BuildCImage.depParamRefs.buildNumber}")
    }

    vcs {
        root(Release1_ProjectBVcs, "%vcs_rules%")

        cleanCheckout = true
    }

    triggers {
        vcs {
            id = "TRIGGER_2"
            enableQueueOptimization = false
        }
        finishBuildTrigger {
            id = "TRIGGER_4"
            buildType = "${Release1_BuildCImage.id}"
            successfulOnly = true
            branchFilter = ""
        }
    }
})
