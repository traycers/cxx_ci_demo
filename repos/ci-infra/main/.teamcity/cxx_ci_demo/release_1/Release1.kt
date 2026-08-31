import jetbrains.buildServer.configs.kotlin.*

val Release1Id = CxxCiDemoId / "Release1"
val Release1ConfigName = "release_1"

// One "release" / branch-family configuration — see docs/adding-a-release.md.
object Release1 : Project({
    id(Release1Id.toString())
    name = Release1ConfigName
    description = """Build for the "Release1" branch and its derivatives. Copyable for the new release."""

    vcsRoot(Release1_ProjectBVcs)
    vcsRoot(Release1_ProjectAVcs)

    buildType(Release1_ProjectB)
    buildType(Release1_ProjectA)
    buildType(Release1_BuildCImage)
    buildType(Release1_ResultBuild)

    template(Release1_BaseBuild)

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
