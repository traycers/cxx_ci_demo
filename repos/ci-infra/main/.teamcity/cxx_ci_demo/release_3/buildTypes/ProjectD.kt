import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

// Root of the a->c->d chain (vecutils) — no dependencies of its own beyond the template's
// snapshot dependency on BuildCImage, same shape as Release3Track_ProjectB.
object Release3Track_ProjectD : BuildType({
    id((Release3TrackId / "ProjectD").toString())
    templates(Release3Track_BaseBuild)
    name = "project_d"

    params {
        param("build_image_cxx", "cxxci-build:${Release3TrackName}-${Release3Track_BuildCImage.depParamRefs.buildNumber}")
    }

    vcs {
        root(Release3Track_ProjectDVcs, "%vcs_rules%")

        cleanCheckout = true
    }

    triggers {
        vcs {
            id = "TRIGGER_7"
            enableQueueOptimization = false
        }
        finishBuildTrigger {
            id = "TRIGGER_8"
            buildType = "${Release3Track_BuildCImage.id}"
            successfulOnly = true
            branchFilter = ""
        }
    }
})
