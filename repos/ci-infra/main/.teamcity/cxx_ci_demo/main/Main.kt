import jetbrains.buildServer.configs.kotlin.*

val MainId = CxxCiDemoId / "Main"

// The track's word, doubling as: this directory's name (cxx_ci_demo/main/), the docker image
// repository name (buildTypes/BuildCImage.kt — cxxci-main:%build.number%/:latest, and
// buildTypes/BuildDevImage.kt — cxxci-main-dev:latest — so two tracks sharing the one docker
// daemon per ADR 0002 never collide; `release_1`/`release_2`/`release_3` still use the older
// cxxci-build:<track>-* scheme, not yet migrated to this one — see this map's ADR ticket), and
// the base git branch name (branch_default/branch_spec below).
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

    // project_a/c/d/e's actual builds live in the Debug/Release package-variant subprojects
    // below (see debug/MainDebug.kt, release/MainRelease.kt) — duplicated per variant because
    // artifact-dependency buildRule=sameChain() disambiguates by which BuildType object a
    // dependency() call targets, not by a parameter, so a single parameterized build_type
    // couldn't guarantee a debug build never links a release upstream. project_b stays here,
    // unduplicated — it's paused (see the `paused` comment on Main_ProjectB) and outside the
    // a->c->d->e chain this split covers.
    buildType(Main_ProjectB)
    buildType(Main_BuildCImage)
    buildType(Main_BuildDevImage)

    template(Main_BaseBuild)

    subProject(Main_Debug)
    subProject(Main_Release)

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
