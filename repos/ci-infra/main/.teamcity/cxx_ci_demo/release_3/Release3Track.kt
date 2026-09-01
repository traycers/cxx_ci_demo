import jetbrains.buildServer.configs.kotlin.*

val Track3Id = CxxCiDemoId / "Track3"

// The track's word, doubling as: this directory's name (cxx_ci_demo/track_3/), the docker image
// tag prefix (buildTypes/BuildCImage.kt, ProjectA.kt, ProjectB.kt —
// cxxci-build:track_3-%build.number%, so two tracks sharing the one docker daemon per ADR 0002
// never collide on a tag), and the base git branch name (branch_default/branch_spec below).
val Track3TrackName = "track_3"

// One "track" / branch-family configuration — see docs/adding-a-track.md.
object Track3 : Project({
    id(Track3Id.toString())
    name = Track3TrackName
    description = """Build for the "track_3" branch and its derivatives. Copyable for the new track."""

    vcsRoot(Track3_ProjectBVcs)
    vcsRoot(Track3_ProjectDVcs)
    vcsRoot(Track3_ProjectCVcs)
    vcsRoot(Track3_ProjectAVcs)
    vcsRoot(Track3_ProjectEVcs)

    buildType(Track3_ProjectB)
    buildType(Track3_ProjectD)
    buildType(Track3_ProjectC)
    buildType(Track3_ProjectA)
    buildType(Track3_ProjectE)
    buildType(Track3_BuildCImage)
    buildType(Track3_ResultBuild)

    template(Track3_BaseBuild)

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
            +:refs/heads/(track_3)
            +:refs/heads/(track_3-*)
        """.trimIndent())
        param("vcs_rules", "+:. => %vcs_dir%")
        param("branch_default", "refs/heads/track_3")
        param("cxx_standard", "20")
        param("keep_images_count", "3")
    }
})
