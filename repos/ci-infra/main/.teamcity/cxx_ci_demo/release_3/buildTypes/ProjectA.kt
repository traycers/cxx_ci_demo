import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

// Flat artifact dependency on BOTH C and D (not just C) — see ADR 0009. A links app_a_core ->
// vecopscale (C) -> vecutils (D); install_component/install_package_config only ever package a
// project's own files, never a dependency's, so C's sdk.zip does not carry D's files along with
// it. A's own find_package(project_c)/find_package(project_d) resolution and final static link
// both need every package's real files sitting in %deps_dir% directly, hence one artifact
// dependency per package in the chain, not one per hop. revisionName=sameChain on each — the
// paired snapshot dependencies, inherited from the template plus the explicit ones below, must
// resolve first in the same chain; no independent branch-based fallback here (Case A from ticket
// 03's research).
object Release3Track_ProjectA : BuildType({
    id((Release3TrackId / "ProjectA").toString())
    templates(Release3Track_BaseBuild)
    name = "project_a"

    params {
        param("build_image_cxx", "cxxci-build:${Release3TrackName}-${Release3Track_BuildCImage.depParamRefs.buildNumber}")
    }

    vcs {
        root(Release3Track_ProjectAVcs, "%vcs_rules%")

        cleanCheckout = true
    }

    triggers {
        vcs {
            id = "TRIGGER_3"
            enableQueueOptimization = false
        }
        finishBuildTrigger {
            id = "TRIGGER_5"
            buildType = "${Release3Track_ProjectC.id}"
            successfulOnly = true
            branchFilter = ""
        }
    }

    dependencies {
        dependency(Release3Track_ProjectC) {
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
        dependency(Release3Track_ProjectD) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            // No cleanDestination here — it targets the same %deps_dir% as the C dependency
            // above, and cleaning it a second time would wipe out C's just-unpacked files.
            // Exactly one dependency in a group sharing a destination may clean it.
            artifacts {
                id = "ARTIFACT_DEPENDENCY_2"
                buildRule = sameChain()
                artifactRules = "%deps_unpack_all%"
            }
        }
    }
})
