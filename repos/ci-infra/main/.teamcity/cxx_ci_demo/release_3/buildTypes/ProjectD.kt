import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

// Root of the a->c->d chain (vecutils) — no dependencies of its own beyond the template's
// snapshot dependency on BuildCImage, same shape as Release3_ProjectB.
object Release3_ProjectD : BuildType({
    id((Release3Id / "ProjectD").toString())
    templates(Release3_BaseBuild)
    name = "project_d"

    params {
        param("build_image_cxx", "cxxci-build:${Release3ConfigName}-${Release3_BuildCImage.depParamRefs.buildNumber}")
    }

    vcs {
        root(Release3_ProjectDVcs, "%vcs_rules%")

        cleanCheckout = true
    }

    triggers {
        vcs {
            id = "TRIGGER_7"
            enableQueueOptimization = false
        }
        finishBuildTrigger {
            id = "TRIGGER_8"
            buildType = "${Release3_BuildCImage.id}"
            successfulOnly = true
            branchFilter = ""
        }
    }
})
