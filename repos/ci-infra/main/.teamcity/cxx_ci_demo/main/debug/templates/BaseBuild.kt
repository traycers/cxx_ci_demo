import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildFeatures.XmlReport
import jetbrains.buildServer.configs.kotlin.buildFeatures.xmlReport
import jetbrains.buildServer.configs.kotlin.buildSteps.ScriptBuildStep
import jetbrains.buildServer.configs.kotlin.buildSteps.script

// Same script as Main_BaseBuild (main/templates/BaseBuild.kt, still used by paused Main_ProjectB)
// — copied rather than shared so build_type can be hardcoded per package-variant subproject
// without a parameter (see map.md's decision). Only line that actually differs from
// release/templates/BaseBuild.kt: build_type="Debug" instead of "RelWithDebInfo".
object Main_Debug_BaseBuild : Template({
    id((Main_DebugId / "BaseBuild").toString())
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
                build_type="Debug"
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
            // See main/templates/BaseBuild.kt for why this line is required alongside dockerPull.
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
