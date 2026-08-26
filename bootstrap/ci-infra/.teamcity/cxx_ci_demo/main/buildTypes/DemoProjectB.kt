import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

object DemoProjectB : BuildType({
    id((MainId / "DemoProjectB").toString())
    templates(BaseBuild)
    name = "demo-project-b"

    params {
        param("build_image_cxx", "cxxci-build:${MainConfigName}-${BuildCImage.depParamRefs.buildNumber}")
    }

    vcs {
        root(DemoProjectBVcs, "%vcs_rules%")

        cleanCheckout = true
    }

    triggers {
        vcs {
            id = "TRIGGER_2"
            enableQueueOptimization = false
        }
        finishBuildTrigger {
            id = "TRIGGER_4"
            buildType = "${BuildCImage.id}"
            successfulOnly = true
            branchFilter = ""
        }
    }
})
