import jetbrains.buildServer.configs.kotlin.*

val Release2TrackId = CxxCiDemoId / "Release2Track"

// The track's word, doubling as: this directory's name (cxx_ci_demo/release_2/), the docker image
// repository name (buildTypes/BuildCImage.kt, ProjectA.kt, ProjectB.kt —
// cxxci-release_2:%build.number%, so two tracks sharing the one docker daemon per ADR 0002 never
// collide — see ADR 0013), and the base git branch name (branch_default/branch_spec below).
val Release2TrackName = "release_2"

// One "track" / branch-family configuration — see docs/adding-a-track.md.
object Release2Track : Project({
    id(Release2TrackId.toString())
    name = Release2TrackName
    description = """Build for the "release_2" branch and its derivatives. Copyable for the new track."""

    vcsRoot(Release2Track_ProjectBVcs)
    vcsRoot(Release2Track_ProjectAVcs)

    buildType(Release2Track_ProjectB)
    buildType(Release2Track_ProjectA)
    buildType(Release2Track_BuildCImage)
    buildType(Release2Track_ResultBuild)

    template(Release2Track_BaseBuild)

    params {
        password("gitlab_credentials_password", "")
        param("install_dir", "_install")
        param("deps_unpack_all", "%deps_archive_name%!** => %deps_dir%")
        param("ctest_timeout_seconds", "60")
        param("vcs_dir", "repo")
        text("extra_cmake_options", "", description = """Additional parameters for cmake configure command. For example: "--debug-find-pkg==package_name".""", allowEmpty = true)
        param("deps_archive_name", "sdk.zip")
        param("deps_dir", "_deps")
        param("branch_spec", """
            +:refs/heads/(release_2)
            +:refs/heads/(release_2-*)
        """.trimIndent())
        param("vcs_rules", "+:. => %vcs_dir%")
        param("branch_default", "refs/heads/release_2")
        param("cxx_standard", "20")
        param("keep_images_count", "3")
    }
})
