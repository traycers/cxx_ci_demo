import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

// Root of the a->c->d chain (vecutils) — no dependencies of its own beyond the template's
// snapshot dependency on BuildCImage, same shape as Main_ProjectB.
object Main_ProjectD : BuildType({
    id((MainId / "ProjectD").toString())
    templates(Main_BaseBuild)
    name = "project_d"

    params {
        param("build_image_cxx", "cxxci-build:${MainTrackName}-${Main_BuildCImage.depParamRefs.buildNumber}")
    }

    vcs {
        root(Main_ProjectDVcs, "%vcs_rules%")

        cleanCheckout = true
    }

    triggers {
        vcs {
            id = "TRIGGER_7"
            enableQueueOptimization = false
        }
        finishBuildTrigger {
            id = "TRIGGER_8"
            buildType = "${Main_BuildCImage.id}"
            successfulOnly = true
            branchFilter = ""
        }
    }
})
