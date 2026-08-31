import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

// Same id as the Main_DemoProjectE buildType (buildTypes/DemoProjectE.kt) — allowed, VCS roots and
// build types have separate id namespaces in TeamCity. Matches what was already live for A/B.
object Main_DemoProjectEVcs : GitVcsRoot({
    id((MainId / "DemoProjectE").toString())
    name = "demo-project-e"
    url = "http://gitlab:8929/root/demo-project-e.git"
    branch = "%branch_default%"
    branchSpec = "%branch_spec%"
    checkoutPolicy = GitVcsRoot.AgentCheckoutPolicy.NO_MIRRORS
    authMethod = password {
        userName = "root"
        password = "%gitlab_credentials_password%"
    }
})
