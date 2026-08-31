import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

// Artifact dependency on D via %deps_unpack_all% (vecopscale's PUBLIC dependency on vecutils —
// see ADR 0009 for why this build still needs D's own artifacts directly, flat, rather than
// relying on some transitive re-packaging). revisionName=sameChain — the paired snapshot
// dependency on D, inherited from the template, must resolve first in the same chain.
object Main_ProjectC : BuildType({
    id((MainId / "ProjectC").toString())
    templates(Main_BaseBuild)
    name = "project_c"

    params {
        param("build_image_cxx", "cxxci-build:${MainConfigName}-${Main_BuildCImage.depParamRefs.buildNumber}")
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
            buildType = "${Main_ProjectD.id}"
            successfulOnly = true
            branchFilter = ""
        }
    }

    dependencies {
        dependency(Main_ProjectD) {
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
