import jetbrains.buildServer.configs.kotlin.*

val CxxCiDemoId = IdPath("CxxCiDemo")

// Index of track/branch-family configurations. Each one is its own directory here
// (currently just main/) — copy that directory, change its ...Id line to derive from a new
// word (e.g. val NextTrackId = CxxCiDemoId / "NextTrack"), rename the Project object it's
// assigned to, and add one subProject(...) line below. Every id nested under it recomposes
// automatically — nothing else in the copy needs to change.
object CxxCiDemo : Project({
    id(CxxCiDemoId.toString())
    name = "cxx_ci_demo"

    subProject(Main)
    subProject(Track1)
    subProject(Track2)
    subProject(Track3)
})
