// Composes TeamCity object ids so each nesting level contributes its own word instead of every
// buildType/vcsRoot/template file spelling out the full parent-prefixed id string by hand.
// TeamCity's Kotlin format *requires* every id to carry its parent project's id as a literal
// prefix (confirmed empirically — a bare id is rejected before compilation is even attempted),
// so the composed value still has to be that full string; this only removes the need to
// hand-type/copy it at every leaf. Available everywhere under .teamcity/ (no package
// declaration — same default package as every other file here).
//
// Usage: val MainId = CxxCiDemoId / "Main"   ->  "CxxCiDemo_Main"
//        val ProjectAId = MainId / "ProjectA"  ->  "CxxCiDemo_Main_ProjectA"
class IdPath(private val value: String) {
    operator fun div(word: String) = IdPath("${value}_${word}")
    override fun toString() = value
}
