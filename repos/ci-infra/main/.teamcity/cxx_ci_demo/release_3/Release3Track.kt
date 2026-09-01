import jetbrains.buildServer.configs.kotlin.*

val Release3TrackId = CxxCiDemoId / "Release3Track"

// The track's word, doubling as: this directory's name (cxx_ci_demo/release_3/), the docker image
// tag prefix (buildTypes/BuildCImage.kt, ProjectA.kt, ProjectB.kt —
// cxxci-build:release_3-%build.number%, so two tracks sharing the one docker daemon per ADR 0002
// never collide on a tag), and the base git branch name (branch_default/branch_spec below).
val Release3TrackName = "release_3"

// One "track" / branch-family configuration — see docs/adding-a-track.md.
object Release3Track : Project({
    id(Release3TrackId.toString())
    name = Release3TrackName
    description = """Build for the "release_3" branch and its derivatives. Copyable for the new track."""

    vcsRoot(Release3Track_ProjectBVcs)
    vcsRoot(Release3Track_ProjectDVcs)
    vcsRoot(Release3Track_ProjectCVcs)
    vcsRoot(Release3Track_ProjectAVcs)
    vcsRoot(Release3Track_ProjectEVcs)

    buildType(Release3Track_ProjectB)
    buildType(Release3Track_ProjectD)
    buildType(Release3Track_ProjectC)
    buildType(Release3Track_ProjectA)
    buildType(Release3Track_ProjectE)
    buildType(Release3Track_BuildCImage)
    buildType(Release3Track_ResultBuild)

    template(Release3Track_BaseBuild)

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
