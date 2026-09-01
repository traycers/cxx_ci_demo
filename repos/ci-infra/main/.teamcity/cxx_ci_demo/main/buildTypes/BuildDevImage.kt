import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.vcs

// Builds the dev-container image FROM this build's own root image (cxxci-main:latest, retagged
// by Main_BuildCImage's "docker build" step) — snapshot dependency below guarantees BuildCImage
// has already run and retagged `latest` in the same chain before this reads it. Layers in a
// debugger + clangd/clang-tidy/clang-format (Dockerfile.dev) for the four demo-project
// devcontainers (ticket 04). Unlike the root image, only ONE version of this is ever kept —
// always overwritten as cxxci-main-dev:latest, no build-number tag — so it needs no cleanup step
// of its own (nothing accumulates).
object Main_BuildDevImage : BuildType({
    id((MainId / "BuildDevImage").toString())
    name = "Build C++ dev image"

    vcs {
        root(DslContext.settingsRoot)
    }

    steps {
        script {
            name = "docker build"
            scriptContent = "docker build -t cxxci-${MainTrackName}-dev:latest -f .teamcity/cxx_ci_demo/${MainTrackName}/Dockerfile.dev .teamcity/cxx_ci_demo/${MainTrackName}"
        }
    }

    triggers {
        vcs {
            enableQueueOptimization = false
        }
    }

    dependencies {
        snapshot(Main_BuildCImage) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
    }
})
