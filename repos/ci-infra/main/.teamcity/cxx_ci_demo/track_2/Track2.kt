import jetbrains.buildServer.configs.kotlin.*

val Track2Id = CxxCiDemoId / "Track2"

// The track's word, doubling as: this directory's name (cxx_ci_demo/track_2/), the docker image
// tag prefix (buildTypes/BuildCImage.kt, ProjectA.kt, ProjectB.kt —
// cxxci-build:track_2-%build.number%, so two tracks sharing the one docker daemon per ADR 0002
// never collide on a tag), and the base git branch name (branch_default/branch_spec below).
val Track2TrackName = "track_2"

// One "track" / branch-family configuration — see docs/adding-a-track.md.
object Track2 : Project({
    id(Track2Id.toString())
    name = Track2TrackName
    description = """Build for the "track_2" branch and its derivatives. Copyable for the new track."""

    vcsRoot(Track2_ProjectBVcs)
    vcsRoot(Track2_ProjectAVcs)

    buildType(Track2_ProjectB)
    buildType(Track2_ProjectA)
    buildType(Track2_BuildCImage)
    buildType(Track2_ResultBuild)

    template(Track2_BaseBuild)

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
            +:refs/heads/(track_2)
            +:refs/heads/(track_2-*)
        """.trimIndent())
        param("vcs_rules", "+:. => %vcs_dir%")
        param("branch_default", "refs/heads/track_2")
        param("cxx_standard", "20")
        param("keep_images_count", "3")
    }
})
