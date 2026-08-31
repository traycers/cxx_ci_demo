import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

// Same id as the Release2_ProjectA buildType (buildTypes/ProjectA.kt) — allowed, VCS roots and
// build types have separate id namespaces in TeamCity. Matches what was already live.
//
// url is hardcoded to the plain docker-compose service name "gitlab", not derived from
// GitLab's own external_url — this traffic is TeamCity-server-to-GitLab, entirely inside the
// cxxci network, so it doesn't need whatever hostname GitLab's external_url happens to be
// configured with (that setting only matters for links GitLab itself generates, e.g. clone URLs
// shown in its UI). Verified live on a scratch VCS root + build before changing this one: git
// resolves "gitlab" via Compose's automatic per-service DNS entry (no docker-compose.yml change
// needed), and GitLab's git-http backend doesn't reject a Host header that doesn't match
// external_url the way its browser-facing routes might.
object Release2_ProjectAVcs : GitVcsRoot({
    id((Release2Id / "ProjectA").toString())
    name = "project_a"
    url = "http://gitlab:8929/root/project_a.git"
    branch = "%branch_default%"
    branchSpec = "%branch_spec%"
    checkoutPolicy = GitVcsRoot.AgentCheckoutPolicy.NO_MIRRORS
    authMethod = password {
        userName = "root"
        password = "%gitlab_credentials_password%"
    }
})
