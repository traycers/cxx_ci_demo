import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

// Artifact dependency on D via %deps_unpack_all% — see ADR 0009. revisionName=sameChain — the
// paired snapshot dependency on D, inherited from the template, must resolve first in the same
// chain, and both point at the Debug-variant D specifically (never Release's), which is the
// entire reason this build type is duplicated per variant instead of parameterized.
object Main_Debug_ProjectC : BuildType({
    id((Main_DebugId / "ProjectC").toString())
    templates(Main_Debug_BaseBuild)
    name = "project_c"

    params {
        param("build_image_cxx", "cxxci-${MainTrackName}:${Main_BuildCImage.depParamRefs.buildNumber}")
    }

    vcs {
        root(Main_ProjectCVcs, "%vcs_rules%")

        cleanCheckout = true
    }

    triggers {
        vcs {
            id = "TRIGGER_9"
            enableQueueOptimization = false
        }
        finishBuildTrigger {
            id = "TRIGGER_10"
            buildType = "${Main_Debug_ProjectD.id}"
            successfulOnly = true
            branchFilter = ""
        }
    }

    dependencies {
        dependency(Main_Debug_ProjectD) {
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
