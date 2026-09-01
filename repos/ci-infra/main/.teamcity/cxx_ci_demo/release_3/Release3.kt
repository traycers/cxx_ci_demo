import jetbrains.buildServer.configs.kotlin.*

val Release3Id = CxxCiDemoId / "Release3"

// The release's word, doubling as: this directory's name (cxx_ci_demo/release_3/), the docker image
// tag prefix (buildTypes/BuildCImage.kt, ProjectA.kt, ProjectB.kt —
// cxxci-build:release_3-%build.number%, so two releases sharing the one docker daemon per ADR 0002
// never collide on a tag), and the base git branch name (branch_default/branch_spec below).
val Release3ConfigName = "release_3"

// One "release" / branch-family configuration — see docs/adding-a-release.md.
object Release3 : Project({
    id(Release3Id.toString())
    name = Release3ConfigName
    description = """Build for the "release_3" branch and its derivatives. Copyable for the new release."""

    vcsRoot(Release3_ProjectBVcs)
    vcsRoot(Release3_ProjectDVcs)
    vcsRoot(Release3_ProjectCVcs)
    vcsRoot(Release3_ProjectAVcs)
    vcsRoot(Release3_ProjectEVcs)

    buildType(Release3_ProjectB)
    buildType(Release3_ProjectD)
    buildType(Release3_ProjectC)
    buildType(Release3_ProjectA)
    buildType(Release3_ProjectE)
    buildType(Release3_BuildCImage)
    buildType(Release3_ResultBuild)

    template(Release3_BaseBuild)

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
            +:refs/heads/(release_3)
            +:refs/heads/(release_3-*)
        """.trimIndent())
        param("vcs_rules", "+:. => %vcs_dir%")
        param("branch_default", "refs/heads/release_3")
        param("cxx_standard", "20")
        param("keep_images_count", "3")
    }
})
