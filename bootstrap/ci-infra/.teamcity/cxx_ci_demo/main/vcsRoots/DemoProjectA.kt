import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

// Same id as the Main_DemoProjectA buildType (buildTypes/DemoProjectA.kt) — allowed, VCS roots and
// build types have separate id namespaces in TeamCity. Matches what was already live.
//
// url uses the plain docker-compose service name "gitlab", not "gitlab.local" — this traffic is
// TeamCity-server-to-GitLab, entirely inside the cxxci network, so it doesn't need the
// human/browser-facing hostname (GITLAB_HOSTNAME/gitlab.local exists for external_url + the
// host's /etc/hosts entry, not for this). Verified live on a scratch VCS root + build before
// changing this one: git resolves "gitlab" via Compose's automatic per-service DNS entry (no
// docker-compose.yml change needed), and GitLab's git-http backend doesn't reject a Host header
// that doesn't match external_url the way its browser-facing routes might.
object Main_DemoProjectAVcs : GitVcsRoot({
    id((MainId / "DemoProjectA").toString())
    name = "demo-project-a"
    url = "http://gitlab:8929/root/demo-project-a.git"
    branch = "%branch_default%"
    branchSpec = "%branch_spec%"
    checkoutPolicy = GitVcsRoot.AgentCheckoutPolicy.NO_MIRRORS
    authMethod = password {
        userName = "root"
        password = "%gitlab_credentials_password%"
    }
})
