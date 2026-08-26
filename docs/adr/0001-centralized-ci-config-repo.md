# Centralized CI configuration repo (ci-infra) instead of per-project .teamcity

TeamCity's idiomatic pattern is per-project versioned settings (a `.teamcity` folder inside each project's own repo). We instead put all Kotlin DSL — the root image build, every C++ project's build configuration, and the snapshot/artifact dependency wiring between them — into one central `ci-infra` repository in GitLab, separate from the C++ project repos.

We chose this because C++ project repos are created independently (often later, by other developers) and shouldn't need to carry CI wiring; the docker image build and the cross-project dependency topology are inherently infrastructure concerns, not per-project concerns; and — deliberately — it stops developers from changing CI configuration from their own project's repo.

Consequence: adding a new C++ project to the build tree always requires a change in `ci-infra`, not just in the new project's own repo. This is intentional, not an oversight.
