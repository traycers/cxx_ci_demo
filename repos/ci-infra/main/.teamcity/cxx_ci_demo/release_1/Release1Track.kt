import jetbrains.buildServer.configs.kotlin.*

val Release1TrackId = CxxCiDemoId / "Release1Track"
val Release1TrackName = "release_1"

// One "track" / branch-family configuration — see docs/adding-a-track.md.
object Release1Track : Project({
    id(Release1TrackId.toString())
    name = Release1TrackName
    description = """Build for the "Release1Track" branch and its derivatives. Copyable for the new track."""

    vcsRoot(Release1Track_ProjectBVcs)
    vcsRoot(Release1Track_ProjectAVcs)

    buildType(Release1Track_ProjectB)
    buildType(Release1Track_ProjectA)
    buildType(Release1Track_BuildCImage)
    buildType(Release1Track_ResultBuild)

    template(Release1Track_BaseBuild)

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
            +:refs/heads/(release_1)
            +:refs/heads/(release_1-*)
        """.trimIndent())
        param("vcs_rules", "+:. => %vcs_dir%")
        param("branch_default", "refs/heads/release_1")
        param("cxx_standard", "20")
        param("keep_images_count", "3")
    }
})
