import jetbrains.buildServer.configs.kotlin.*

val Main_ReleaseId = MainId / "Release"

// Package-variant subproject: today's `release` build_type (CMAKE_BUILD_TYPE=RelWithDebInfo) — see CONTEXT.md's "Package variant" entry.
// Sibling of Main_Debug (MainDebug.kt); both share Main's VCS roots, BuildCImage, track-wide params (branch_spec/branch_default/cxx_standard/etc.), and now the same Main_BaseBuild template (templates/BaseBuild.kt) too — only install_dir/deps_dir and the cxx_build_type param overridden below differ per variant.
object Main_Release : Project({
    id(Main_ReleaseId.toString())
    name = "release"

    buildType(Main_Release_ProjectD)
    buildType(Main_Release_ProjectC)
    buildType(Main_Release_ProjectA)
    buildType(Main_Release_ProjectE)
    buildType(Main_Release_Result)

    params {
        param("cxx_build_type", "RelWithDebInfo")
    }
})
