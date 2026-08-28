import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object Release2_DemoProjectB : BuildType({
    id((Release2Id / "DemoProjectB").toString())
    templates(Release2_BaseBuild)
    name = "demo-project-b"

    params {
        param("build_image_cxx", "cxxci-build:${Release2ConfigName}-${Release2_BuildCImage.depParamRefs.buildNumber}")
    }

    vcs {
        root(Release2_DemoProjectBVcs, "%vcs_rules%")

        cleanCheckout = true
    }

    triggers {
        vcs {
            id = "TRIGGER_2"
            enableQueueOptimization = false
        }
        finishBuildTrigger {
            id = "TRIGGER_4"
            buildType = "${Release2_BuildCImage.id}"
            successfulOnly = true
            branchFilter = ""
        }
    }
})
