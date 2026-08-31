import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

// Same id as the Main_ProjectD buildType (buildTypes/ProjectD.kt) — allowed, VCS roots and
// build types have separate id namespaces in TeamCity. Matches what was already live for A/B.
object Main_ProjectDVcs : GitVcsRoot({
    id((MainId / "ProjectD").toString())
    name = "project_d"
    url = "http://gitlab:8929/root/project_d.git"
    branch = "%branch_default%"
    branchSpec = "%branch_spec%"
    checkoutPolicy = GitVcsRoot.AgentCheckoutPolicy.NO_MIRRORS
    authMethod = password {
        userName = "root"
        password = "%gitlab_credentials_password%"
    }
})
