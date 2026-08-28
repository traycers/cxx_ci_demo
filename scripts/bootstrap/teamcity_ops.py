"""TeamCity side: point versioned settings at ci-infra, let DSL own the project tree.

See ADR 0004 for why this is import-mode Kotlin DSL rather than REST-built config, and ADR 0003
(superseded) for the earlier, incorrect "impossible" conclusion. This module only does what the
DSL itself cannot: bootstrap the one VCS root versioned settings needs to fetch ci-infra in the
first place, point versioned settings at it, and inject the GitLab credential the tree's own VCS
roots need (it can't live in git even as a reference — see ADR 0004).

Every call goes straight to teamcity-server over the cxxci network (this process runs attached to
it, per ADR 0008) — no sibling `curl` container, no published-host-port squid-proxy collision.
"""

import json
import time

import requests

import config
import docker_ops

log = config.log


class TeamCityClient:
    def __init__(self, token):
        self.auth = ("", token)
        self.base = config.TEAMCITY_URL

    def get_status(self, path):
        try:
            resp = requests.get(self.base + path, auth=self.auth, timeout=15)
            return resp.status_code
        except requests.RequestException:
            return None

    def get(self, path):
        return requests.get(
            self.base + path, auth=self.auth, headers={"Accept": "application/json"}, timeout=15
        )

    def post(self, path, json_body):
        resp = requests.post(
            self.base + path,
            auth=self.auth,
            data=json_body,
            headers={"Content-Type": "application/json", "Accept": "application/json"},
            timeout=30,
        )
        return resp.status_code, resp.text

    def put(self, path, data, content_type="application/json"):
        resp = requests.put(
            self.base + path, auth=self.auth, data=data, headers={"Content-Type": content_type}, timeout=30
        )
        return resp.status_code, resp.text


def teamcity_super_user_token():
    logs = docker_ops.get_logs("teamcity-server")
    token = None
    marker = "Super user authentication token:"
    for line in logs.splitlines():
        if marker in line:
            token = line.split(marker, 1)[1].strip().split()[0]
    return token


def _vcs_root_payload(vcs_id, name, repo, gitlab_token):
    # NOTE on "teamcity:branchSpec": a VCS root created via REST with a plain "branchSpec"
    # property is silently ignored for branch matching (confirmed against a UI-created VCS root,
    # which TeamCity itself writes as "teamcity:branchSpec") — this was the real cause behind
    # ticket 09's long invalid_branch_name investigation, not a branchSpec syntax issue.
    return {
        "id": vcs_id,
        "name": name,
        "vcsName": "jetbrains.git",
        "project": {"id": "_Root"},
        "properties": {
            "property": [
                {"name": "url", "value": f"http://{config.GITLAB_HOST}:{config.GITLAB_HTTP_PORT}/root/{repo}.git"},
                {"name": "branch", "value": "refs/heads/main"},
                {"name": "teamcity:branchSpec", "value": "+:refs/heads/*"},
                {"name": "authMethod", "value": "PASSWORD"},
                {"name": "username", "value": "root"},
                {"name": "secure:password", "value": gitlab_token},
            ]
        },
    }


def provision_teamcity(gitlab_token):
    token = teamcity_super_user_token()
    if not token:
        log("Could not find a TeamCity Super User token in the logs yet.")
        log("This means the one unavoidable manual step (README step 4: the first-start")
        log("browser wizard) hasn't been completed yet. Complete it, then re-run bootstrap.")
        return False

    tc = TeamCityClient(token)
    log("provisioning TeamCity versioned settings (git/UI owns the project tree from here)...")

    # 1. The one VCS root the DSL itself cannot create: without it, versioned settings has
    #    nothing to fetch ci-infra's .teamcity/settings.kts from in the first place.
    if tc.get_status("/app/rest/vcs-roots/id:CiInfraVersionedSettingsVcs") != 200:
        status, body = tc.post(
            "/app/rest/vcs-roots",
            json.dumps(_vcs_root_payload("CiInfraVersionedSettingsVcs", "ci-infra (versioned settings)", "ci-infra", gitlab_token)),
        )
        if status != 200:
            log(f"ERROR: failed to create VCS root CiInfraVersionedSettingsVcs (HTTP {status}).")
            log(f"Response: {body}")
            log("This can happen if GitLab wasn't fully ready for git operations yet even though")
            log("readiness passed, or a transient network issue. Re-run bootstrap.")
            return False
        log("  created VCS root CiInfraVersionedSettingsVcs")

    # 2. Point _Root's versioned settings at it: format=kotlin, buildSettingsMode=useFromVCS.
    vs_config = {
        "synchronizationMode": "enabled",
        "vcsRootId": "CiInfraVersionedSettingsVcs",
        "format": "kotlin",
        "buildSettingsMode": "useFromVCS",
        "allowUIEditing": True,
        "storeSecureValuesOutsideVcs": True,
    }
    status, _ = tc.put("/app/rest/projects/id:_Root/versionedSettings/config", json.dumps(vs_config))
    if status == 500:
        # A server that already has a *different* tree needs an explicit importDecision.
        status, _ = tc.put(
            "/app/rest/projects/id:_Root/versionedSettings/config",
            json.dumps({**vs_config, "importDecision": "importFromVCS"}),
        )
    if status != 200:
        log(f"ERROR: failed to enable versioned settings (HTTP {status}).")
        log("Check that VCS root CiInfraVersionedSettingsVcs exists and is valid, then re-run bootstrap.")
        return False
    log("  versioned settings pointed at ci-infra (Kotlin, import mode)")

    # 3. Wait for the DSL to actually have applied — poll the tree it's supposed to create,
    #    not the status message text (see the bash version's comment on why).
    log("  waiting for DSL import to apply...")
    for _ in range(30):
        if tc.get_status("/app/rest/buildTypes/id:CxxCiDemo_Main_DemoProjectA") == 200:
            break
        time.sleep(3)
    log(f"  {tc.get('/app/rest/projects/id:_Root/versionedSettings/status').text}")

    # 4. The GitLab credential the demo VCS roots need — not carried by the DSL (see ADR 0004).
    if tc.get_status("/app/rest/buildTypes/id:CxxCiDemo_Main_DemoProjectA") == 200:
        for vcs in ("CxxCiDemo_Main_DemoProjectA", "CxxCiDemo_Main_DemoProjectB"):
            tc.put(f"/app/rest/vcs-roots/id:{vcs}/properties/secure:password", gitlab_token, content_type="text/plain")
        param_payload = json.dumps(
            {
                "name": "gitlab_credentials_password",
                "value": gitlab_token,
                "type": {"rawValue": "password display='normal'"},
            }
        )
        tc.post("/app/rest/projects/id:CxxCiDemo_Main/parameters", param_payload)
        log("  injected GitLab credential into the demo project's VCS roots")
    else:
        log("  demo project tree not present yet (DSL import may still be settling) —")
        log("  re-run bootstrap once CxxCiDemo_Main_DemoProjectA exists to inject credentials.")

    # 5. Agent authorization: documented as a manual UI step, but has a REST escape hatch.
    # PUT, not POST — confirmed live: POST to this endpoint returns 405 Method Not Allowed
    # (this REST call was apparently never actually exercised before; same-host agents do NOT
    # auto-authorize here despite the docs suggesting they might).
    agent_resp = tc.get("/app/rest/agents/id:1?fields=authorized")
    if '"authorized":true' not in agent_resp.text:
        status, body = tc.put("/app/rest/agents/id:1/authorizedInfo", json.dumps({"status": True, "text": "authorized by bootstrap"}))
        if status != 200:
            log(f"ERROR: failed to authorize agent (HTTP {status}): {body}")
        else:
            log("  authorized build agent")

    log("TeamCity provisioned: versioned settings import from ci-infra owns the project tree")
    log("(CxxCiDemo_Main: base_build template + BuildCImage + DemoProjectA/B + Result) — edit")
    log("repos/ci-infra/main/.teamcity/settings.kts or the TeamCity UI, both land in git. See ADR 0004.")
    return True
