import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs

// Named Release3_ResultBuild, not Result — "Result" would shadow kotlin.Result from the stdlib's
// implicit import.
//
// Aggregation/release-packaging build type: pulls project_a's sdk.zip (which itself already
// covers the whole a->c->d chain via its own flat snapshot+artifact dependencies — see ADR 0009)
// and project_e's sdk.zip (e is self-sufficient — no artifact chain of its own to bring along),
// stages both, and publishes result.zip. project_b isn't part of this release's chain at all (see
// the `paused` comment on Release3_ProjectB) so it has nothing to contribute here. "files
// checking"/"protection of executable files"/"signing files" are still placeholder steps for
// future release-hardening logic.
object Release3_ResultBuild : BuildType({
    id((Release3Id / "Result").toString())
    name = "result"
    description = "Accumulates build results and triggers automatically on VCS changes."

    artifactRules = "%install_dir% => result.zip"

    steps {
        script {
            name = "files checking"
            id = "building"
            scriptContent = """echo "Filtering files for release - selecting only production artifacts""""
        }
        script {
            name = "protection of executable files"
            id = "protection_of_executable_files"
            scriptContent = """echo "protection of executable files""""
        }
        script {
            name = "signing files"
            id = "signing_files"
            scriptContent = """echo "signing files""""
        }
        script {
            name = "creating an installer"
            id = "creating_an_installer"
            scriptContent = """
                echo "creating an installer"
                mkdir -p %install_dir%
                cp -r %deps_dir%/bin/* %install_dir%/
            """.trimIndent()
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
        dependency(Release3_ProjectA) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            // cleanDestination here (same %deps_dir% as the E dependency below) so leftover
            // files from a previous build on this agent can't survive into result.zip — see
            // ProjectA.kt's C/D dependencies for the same pattern. Exactly one dependency in a
            // group sharing a destination may clean it; E deliberately doesn't repeat this.
            artifacts {
                id = "ARTIFACT_DEPENDENCY_1"
                buildRule = sameChain()
                cleanDestination = true
                artifactRules = "%deps_unpack_all%"
            }
        }
        dependency(Release3_ProjectE) {
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
