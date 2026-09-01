import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

// Root of the a->c->d chain (vecutils) — no dependencies of its own beyond the template's
// snapshot dependency on BuildCImage, same shape as Track3_ProjectB.
object Track3_ProjectD : BuildType({
    id((Track3Id / "ProjectD").toString())
    templates(Track3_BaseBuild)
    name = "project_d"

    params {
        param("build_image_cxx", "cxxci-build:${Track3TrackName}-${Track3_BuildCImage.depParamRefs.buildNumber}")
    }

    vcs {
        root(Track3_ProjectDVcs, "%vcs_rules%")

        cleanCheckout = true
    }

    triggers {
        vcs {
            id = "TRIGGER_7"
            enableQueueOptimization = false
        }
        finishBuildTrigger {
            id = "TRIGGER_8"
            buildType = "${Track3_BuildCImage.id}"
            successfulOnly = true
            branchFilter = ""
        }
    }
})
