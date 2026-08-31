import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.XmlReport
import jetbrains.buildServer.configs.kotlin.buildFeatures.xmlReport
import jetbrains.buildServer.configs.kotlin.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.script

// The build script is the single source of truth for how every C++ project in this release
// builds/tests/installs — edit it here, not in a separate shell script (see ADR 0004: the old
// docs/build.sh was removed once this became the only place it actually lived).
object Main_BaseBuild : Template({
    id((MainId / "BaseBuild").toString())
    name = "base_build"

    artifactRules = "%install_dir% => %deps_archive_name%"

    steps {
        script {
            name = "building"
            id = "building"
            scriptContent = """
                #!/bin/bash
                set -e


                work_dir="/work_dir"
                build_type="RelWithDebInfo"
                install_dir="/host_dir/%install_dir%"
                deps_dir="/host_dir/%deps_dir%"
                build_dir="/shadow_build"


                mkdir -p "${'$'}work_dir"
                mkdir -p "${'$'}build_dir"
                # Copy all project files from /host_dir volume to a temporary working directory inside the container.
                # This is critical for performance, especially when using Docker on Windows,
                # where file operations through volumes (bind mounts) are significantly slower compared to the container's native filesystem. Copying helps avoid
                # performance issues when building C++ projects with a large number of files.
                cp -r /host_dir/* "${'$'}work_dir"
                cd "${'$'}work_dir"



                echo "##teamcity[blockOpened name='cmake configure' description='configure project']"
                cmake \
                    -GNinja \
                    -DCMAKE_BUILD_TYPE="${'$'}build_type" \
                    -DCMAKE_CXX_STANDARD="%cxx_standard%" \
                    -DCMAKE_PREFIX_PATH="${'$'}deps_dir" \
                    -DCMAKE_INSTALL_PREFIX="${'$'}install_dir" \
                    -DCMAKE_POSITION_INDEPENDENT_CODE="ON" \
                    %extra_cmake_options% \
                    -S "${'$'}work_dir/%vcs_dir%" \
                    -B "${'$'}build_dir"
                echo "##teamcity[blockClosed name='cmake configure']"


                echo "##teamcity[blockOpened name='cmake build' description='build project']"
                cmake \
                    --build "${'$'}build_dir" \
                    --target install \
                    --config "${'$'}build_type" \
                    --parallel
                echo "##teamcity[blockClosed name='cmake build']"


                set +e
                echo "##teamcity[blockOpened name='cmake test' description='execute all test for project']"
                ctest \
                    --test-dir "${'$'}build_dir" \
                    -VV \
                    -T Test \
                    --verbose \
                    --output-on-failure \
                    --timeout %ctest_timeout_seconds% \
                    -C ${'$'}build_type
                echo "##teamcity[blockClosed name='cmake test']"


                echo "##teamcity[blockOpened name='copy test result' description='copy test result']"
                mkdir -p /host_dir/test_result
                cp -r "${'$'}build_dir"/Testing/* /host_dir/test_result
                echo "##teamcity[blockClosed name='copy test result']"
            """.trimIndent()
            dockerImage = "%build_image_cxx%"
            dockerImagePlatform = ScriptBuildStep.ImagePlatform.Linux
            dockerPull = false
            dockerRunParameters = "-v %teamcity.build.checkoutDir%:/host_dir"
            // dockerPull=false above serializes to an EMPTY property value (plugin.docker.pull.enabled=""),
            // not the string "false" — TeamCity's runtime treats empty/unset as its own historical
            // default, which is to pull. Forcing the literal string here is what actually disables it.
            // Confirmed live: without this line the agent ran `docker pull` and failed with
            // "pull access denied" against an image that only ever exists locally (see ADR 0002).
            param("plugin.docker.pull.enabled", "false")
        }
    }

    features {
        xmlReport {
            id = "BUILD_EXT_1"
            reportType = XmlReport.XmlReportType.CTEST
            rules = "+:test_result/**/*.xml"
        }
    }

    dependencies {
        snapshot(Main_BuildCImage) {
            onDependencyFailure = FailureAction.FAIL_TO_START
        }
    }
})
