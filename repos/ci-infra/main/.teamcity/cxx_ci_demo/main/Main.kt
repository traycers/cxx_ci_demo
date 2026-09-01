import jetbrains.buildServer.configs.kotlin.*

val MainId = CxxCiDemoId / "Main"

// The track's word, doubling as: this directory's name (cxx_ci_demo/main/), the docker image
// tag prefix (buildTypes/BuildCImage.kt, ProjectA.kt, ProjectB.kt —
// cxxci-build:main-%build.number%, so two tracks sharing the one docker daemon per ADR 0002
// never collide on a tag), and the base git branch name (branch_default/branch_spec below).
val MainTrackName = "main"

// One "track" / branch-family configuration — see docs/adding-a-track.md.
object Main : Project({
    id(MainId.toString())
    name = MainTrackName
    description = """Build for the "main" branch and its derivatives. Copyable for the new track."""

    vcsRoot(Main_ProjectBVcs)
    vcsRoot(Main_ProjectDVcs)
    vcsRoot(Main_ProjectCVcs)
    vcsRoot(Main_ProjectAVcs)
    vcsRoot(Main_ProjectEVcs)

    buildType(Main_ProjectB)
    buildType(Main_ProjectD)
    buildType(Main_ProjectC)
    buildType(Main_ProjectA)
    buildType(Main_ProjectE)
    buildType(Main_BuildCImage)
    buildType(Main_ResultBuild)

    template(Main_BaseBuild)

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
            +:refs/heads/(main)
            +:refs/heads/(main-*)
        """.trimIndent())
        param("vcs_rules", "+:. => %vcs_dir%")
        param("branch_default", "refs/heads/main")
        param("cxx_standard", "20")
        param("keep_images_count", "3")
    }
})
