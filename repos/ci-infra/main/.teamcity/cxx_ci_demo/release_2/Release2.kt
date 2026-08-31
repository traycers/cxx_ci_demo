import jetbrains.buildServer.configs.kotlin.*

val Release2Id = CxxCiDemoId / "Release2"

// The release's word, doubling as: this directory's name (cxx_ci_demo/release_2/), the docker image
// tag prefix (buildTypes/BuildCImage.kt, ProjectA.kt, ProjectB.kt —
// cxxci-build:release_2-%build.number%, so two releases sharing the one docker daemon per ADR 0002
// never collide on a tag), and the base git branch name (branch_default/branch_spec below).
val Release2ConfigName = "release_2"

// One "release" / branch-family configuration — see docs/adding-a-release.md.
object Release2 : Project({
    id(Release2Id.toString())
    name = Release2ConfigName
    description = """Build for the "release_2" branch and its derivatives. Copyable for the new release."""

    vcsRoot(Release2_ProjectBVcs)
    vcsRoot(Release2_ProjectAVcs)

    buildType(Release2_ProjectB)
    buildType(Release2_ProjectA)
    buildType(Release2_BuildCImage)
    buildType(Release2_ResultBuild)

    template(Release2_BaseBuild)

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
