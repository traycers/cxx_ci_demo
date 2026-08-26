import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs

// Named ResultBuild, not Result — "Result" would shadow kotlin.Result from the stdlib's
// implicit import.
//
// Aggregation/release-packaging build type: pulls demo-project-a's sdk.zip (which itself already
// covers demo-project-b transitively via its own snapshot+artifact chain), stages it, and
// publishes result.zip. "files checking"/"protection of executable files"/"signing files" are
// still placeholder steps for future release-hardening logic.
object ResultBuild : BuildType({
    id((MainId / "Result").toString())
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
        dependency(DemoProjectA) {
            snapshot {
                onDependencyFailure = FailureAction.FAIL_TO_START
            }

            artifacts {
                buildRule = sameChain()
                artifactRules = "%deps_unpack_all%"
            }
        }
    }
})
