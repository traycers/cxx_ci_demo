import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

// Self-sufficient project (unlike A, which artifact-depends on other demo projects) — no
// dependencies block beyond the template's own snapshot dependency on BuildCImage. Still needs
// the same finishBuildTrigger off BuildCImage that B/D carry (CONTEXT.md's "Root image build"
// entry: rebuilding the image rebuilds everything depending on it) — nothing else *chains* into
// E, but the image rebuild still should.
object Release3Track_ProjectE : BuildType({
    id((Release3TrackId / "ProjectE").toString())
    templates(Release3Track_BaseBuild)
    name = "project_e"

    params {
        param("build_image_cxx", "cxxci-build:${Release3TrackName}-${Release3Track_BuildCImage.depParamRefs.buildNumber}")
    }

    vcs {
        root(Release3Track_ProjectEVcs, "%vcs_rules%")

        cleanCheckout = true
    }

    triggers {
        vcs {
            id = "TRIGGER_6"
            enableQueueOptimization = false
        }
        finishBuildTrigger {
            id = "TRIGGER_11"
            buildType = "${Release3Track_BuildCImage.id}"
            successfulOnly = true
            branchFilter = ""
        }
    }
})
