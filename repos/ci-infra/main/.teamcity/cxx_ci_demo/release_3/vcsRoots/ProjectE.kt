import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

// Same id as the Release3Track_ProjectE buildType (buildTypes/ProjectE.kt) — allowed, VCS roots and
// build types have separate id namespaces in TeamCity. Matches what was already live for A/B.
object Release3Track_ProjectEVcs : GitVcsRoot({
    id((Release3TrackId / "ProjectE").toString())
    name = "project_e"
    url = "http://gitlab:8929/root/project_e.git"
    branch = "%branch_default%"
    branchSpec = "%branch_spec%"
    checkoutPolicy = GitVcsRoot.AgentCheckoutPolicy.NO_MIRRORS
    authMethod = password {
        userName = "root"
        password = "%gitlab_credentials_password%"
    }
})
