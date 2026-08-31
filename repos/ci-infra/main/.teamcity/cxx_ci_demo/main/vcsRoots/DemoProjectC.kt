import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

// Same id as the Main_DemoProjectC buildType (buildTypes/DemoProjectC.kt) — allowed, VCS roots and
// build types have separate id namespaces in TeamCity. Matches what was already live for A/B.
object Main_DemoProjectCVcs : GitVcsRoot({
    id((MainId / "DemoProjectC").toString())
    name = "demo-project-c"
    url = "http://gitlab:8929/root/demo-project-c.git"
    branch = "%branch_default%"
    branchSpec = "%branch_spec%"
    checkoutPolicy = GitVcsRoot.AgentCheckoutPolicy.NO_MIRRORS
    authMethod = password {
        userName = "root"
        password = "%gitlab_credentials_password%"
    }
})
