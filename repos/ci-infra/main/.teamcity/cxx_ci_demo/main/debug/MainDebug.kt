import jetbrains.buildServer.configs.kotlin.*

val Main_DebugId = MainId / "Debug"

// Package-variant subproject: the new `debug` build_type (CMAKE_BUILD_TYPE=Debug) — see
// CONTEXT.md's "Package variant" entry. Sibling of Main_Release (../release/MainRelease.kt);
// both share Main's VCS roots, BuildCImage and track-wide params — only install_dir/deps_dir and
// the build_type baked into base_build differ per variant.
//
// Note: install_dir/deps_dir stay the standard Main-inherited pair ("_install"/"_deps") for
// ProjectA/C/D/E here, same as Main_Release — each project's own sdk.zip must still only contain
// that project's own files, never a dependency's (ADR 0009's flat-not-transitive design; see
// ProjectA.kt's comment). Only Main_Debug_Result overrides install_dir to alias deps_dir, in its
// own params block (buildTypes/Result.kt) — not here at the Project level, which would leak the
// alias into every build type in this subproject and break that separation.
object Main_Debug : Project({
    id(Main_DebugId.toString())
    name = "debug"

    buildType(Main_Debug_ProjectD)
    buildType(Main_Debug_ProjectC)
    buildType(Main_Debug_ProjectA)
    buildType(Main_Debug_ProjectE)
    buildType(Main_Debug_Result)

    template(Main_Debug_BaseBuild)
})
