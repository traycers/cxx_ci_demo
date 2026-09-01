import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.triggers.vcs

// Flat artifact dependency on BOTH C and D (not just C) — see ADR 0009. install_component/
// install_package_config only ever package a project's own files, never a dependency's, so C's
// sdk.zip does not carry D's files along with it — hence one artifact dependency per package in
// the chain, not one per hop. Both dependencies point at the Debug-variant C/D specifically
// (never Release's) — the entire reason this build type is duplicated per variant instead of
// parameterized (see map.md's decision).
object Main_Debug_ProjectA : BuildType({
    id((Main_DebugId / "ProjectA").toString())
    templates(Main_Debug_BaseBuild)
    name = "project_a"

    params {
        param("build_image_cxx", "cxxci-${MainTrackName}:${Main_BuildCImage.depParamRefs.buildNumber}")
    }

    vcs {
        root(Main_ProjectAVcs, "%vcs_rules%")

        cleanCheckout = true
    }

    triggers {
        vcs {
            id = "TRIGGER_3"
            enableQueueOptimization = false
        }
        finishBuildTrigger {
            id = "TRIGGER_5"
            buildType = "${Main_Debug_ProjectC.id}"
            successfulOnly = true
            branchFilter = ""
        }
    }

    dependencies {
        dependency(Main_Debug_ProjectC) {
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
        dependency(Main_Debug_ProjectD) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            // No cleanDestination here — it targets the same %deps_dir% as the C dependency
            // above, and cleaning it a second time would wipe out C's just-unpacked files.
            artifacts {
                id = "ARTIFACT_DEPENDENCY_2"
                buildRule = sameChain()
                artifactRules = "%deps_unpack_all%"
            }
        }
    }
})
