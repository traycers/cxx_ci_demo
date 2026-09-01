import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

// Same id as the Release2Track_ProjectB buildType (buildTypes/ProjectB.kt) — allowed, VCS roots and
// build types have separate id namespaces in TeamCity. Matches what was already live.
object Release2Track_ProjectBVcs : GitVcsRoot({
    id((Release2TrackId / "ProjectB").toString())
    name = "project_b"
    url = "http://gitlab:8929/root/project_b.git"
    branch = "%branch_default%"
    branchSpec = "%branch_spec%"
    checkoutPolicy = GitVcsRoot.AgentCheckoutPolicy.NO_MIRRORS
    authMethod = password {
        userName = "root"
        password = "%gitlab_credentials_password%"
    }
})
