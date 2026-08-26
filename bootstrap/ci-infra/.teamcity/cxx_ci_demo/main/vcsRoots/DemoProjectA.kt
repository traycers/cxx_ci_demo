import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

// Same id as the DemoProjectA buildType (buildTypes/DemoProjectA.kt) — allowed, VCS roots and
// build types have separate id namespaces in TeamCity. Matches what was already live.
object DemoProjectAVcs : GitVcsRoot({
    id((MainId / "DemoProjectA").toString())
    name = "demo-project-a"
    url = "http://gitlab.local:8929/root/demo-project-a.git"
    branch = "%branch_default%"
    branchSpec = "%branch_spec%"
    checkoutPolicy = GitVcsRoot.AgentCheckoutPolicy.NO_MIRRORS
    authMethod = password {
        userName = "root"
        password = "%gitlab_credentials_password%"
    }
})
