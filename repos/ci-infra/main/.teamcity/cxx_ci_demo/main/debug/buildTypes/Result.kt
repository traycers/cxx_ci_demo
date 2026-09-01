import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs

// Named Main_Debug_Result, not Result — "Result" would shadow kotlin.Result from the stdlib's
// implicit import.
//
// Aggregation build type: pulls project_a's sdk.zip (covers the whole a->c->d chain via its own
// flat snapshot+artifact dependencies — ADR 0009) and project_e's sdk.zip, into %deps_dir%.
// Unlike Main_Release_Result, there is no release-hardening/install-filtering pipeline here — the
// whole point of the debug package variant (roadmap.md, Phase 1) is to hand a developer
// everything that was resolved for the chain, as-is, so they can drop it into an install
// directory one level above their own checkout and point CMAKE_PREFIX_PATH at it. `echo Hello`
// is a placeholder step (parallel to Main_Release_Result's files-checking/signing placeholders) —
// it does not prepare the archive's contents. install_dir is overridden (below) to the same path
// as deps_dir, purely so `artifactRules = "%install_dir% => result.zip"` archives exactly what
// the artifact-dependencies already unpacked, with no copy step in between.
object Main_Debug_Result : BuildType({
    id((Main_DebugId / "Result").toString())
    name = "result"
    description = "Accumulates build results and triggers automatically on VCS changes."

    artifactRules = "%install_dir% => result.zip"

    params {
        param("install_dir", "%deps_dir%")
    }

    steps {
        script {
            name = "Hello"
            id = "hello"
            scriptContent = """echo "Hello""""
        }
    }

    triggers {
        vcs {
            triggerRules = "+:**"
            branchFilter = ""
            watchChangesInDependencies = true
        }
    }

    dependencies {
        dependency(Main_Debug_ProjectA) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            // cleanDestination here (same %deps_dir% as the E dependency below) so leftover
            // files from a previous build on this agent can't survive into result.zip.
            artifacts {
                id = "ARTIFACT_DEPENDENCY_1"
                buildRule = sameChain()
                cleanDestination = true
                artifactRules = "%deps_unpack_all%"
            }
        }
        dependency(Main_Debug_ProjectE) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            // No cleanDestination here — it would wipe out project_a's just-unpacked files in
            // the same %deps_dir% (cleaned once, above).
            artifacts {
                id = "ARTIFACT_DEPENDENCY_2"
                buildRule = sameChain()
                artifactRules = "%deps_unpack_all%"
            }
        }
    }
})
