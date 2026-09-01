import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

// Artifact dependency on B via %deps_unpack_all% (sdk.zip!** => %deps_dir%, itself a project
// parameter — see ticket 09). revisionName=sameChain — the paired snapshot dependency on B,
// inherited from the template, must resolve first in the same chain; no independent branch-based
// fallback here (Case A from ticket 03's research).
object Release2Track_ProjectA : BuildType({
    id((Release2TrackId / "ProjectA").toString())
    templates(Release2Track_BaseBuild)
    name = "project_a"

    params {
        param("build_image_cxx", "cxxci-${Release2TrackName}:${Release2Track_BuildCImage.depParamRefs.buildNumber}")
    }

    vcs {
        root(Release2Track_ProjectAVcs, "%vcs_rules%")

        cleanCheckout = true
    }

    triggers {
        vcs {
            id = "TRIGGER_3"
            enableQueueOptimization = false
        }
        finishBuildTrigger {
            id = "TRIGGER_5"
            buildType = "${Release2Track_ProjectB.id}"
            successfulOnly = true
            branchFilter = ""
        }
    }

    dependencies {
        dependency(Release2Track_ProjectB) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                id = "ARTIFACT_DEPENDENCY_1"
                buildRule = sameChain()
                cleanDestination = true
                artifactRules = "%deps_unpack_all%"
            }
        }
    }
})
