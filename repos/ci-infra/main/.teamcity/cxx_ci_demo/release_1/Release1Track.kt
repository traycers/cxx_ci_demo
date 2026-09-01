import jetbrains.buildServer.configs.kotlin.*

val Track1Id = CxxCiDemoId / "Track1"
val Track1TrackName = "track_1"

// One "track" / branch-family configuration — see docs/adding-a-track.md.
object Track1 : Project({
    id(Track1Id.toString())
    name = Track1TrackName
    description = """Build for the "Track1" branch and its derivatives. Copyable for the new track."""

    vcsRoot(Track1_ProjectBVcs)
    vcsRoot(Track1_ProjectAVcs)

    buildType(Track1_ProjectB)
    buildType(Track1_ProjectA)
    buildType(Track1_BuildCImage)
    buildType(Track1_ResultBuild)

    template(Track1_BaseBuild)

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
            +:refs/heads/(track_1)
            +:refs/heads/(track_1-*)
        """.trimIndent())
        param("vcs_rules", "+:. => %vcs_dir%")
        param("branch_default", "refs/heads/track_1")
        param("cxx_standard", "20")
        param("keep_images_count", "3")
    }
})
