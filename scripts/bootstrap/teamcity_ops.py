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


def _poll_until(check, deadline, interval, waiting_message=None):
    """Retry `check` (a no-arg callable returning bool) until it's true or `deadline` passes."""
    while True:
        if check():
            return True
        if time.time() >= deadline:
            return False
        if waiting_message:
            log(waiting_message)
        time.sleep(interval)


def provision_teamcity(gitlab_token):
    token = teamcity_super_user_token()
    if not token:
        log("Could not find a TeamCity Super User token in the logs yet.")
        log("This means the one unavoidable manual step (README step 3: the first-start")
        log("browser wizard) hasn't been completed yet. Complete it, then re-run bootstrap.")
        return False

    tc = TeamCityClient(token)
    log("provisioning TeamCity versioned settings (git/UI owns the project tree from here)...")
    deadline = time.time() + config.TEAMCITY_PROVISION_TIMEOUT_SECONDS

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
    if not _poll_until(
        lambda: tc.get_status("/app/rest/buildTypes/id:CxxCiDemo_Main_ProjectA") == 200,
        deadline,
        interval=3,
    ):
        log(f"ERROR: DSL import did not apply within {config.TEAMCITY_PROVISION_TIMEOUT_SECONDS}s "
            "(CxxCiDemo_Main_ProjectA never appeared in the REST API).")
        log("Check repos/ci-infra/main/.teamcity/settings.kts for errors, then re-run bootstrap.")
        return False
    log(f"  {tc.get('/app/rest/projects/id:_Root/versionedSettings/status').text}")

    # 4. The GitLab credential the demo VCS roots need — not carried by the DSL (see ADR 0004).
    #    Discovered dynamically (every VCS root outside _Root, i.e. every track's demo VCS
    #    roots) rather than a hardcoded per-track list: a hardcoded list goes stale the moment
    #    a new track is added via scripts/new-track.sh, exactly as adding-a-track.md warns
    #    ("extend that loop... when this stops being a one-track demo") — confirmed live: with
    #    the old Main-only list, track_1/track_2's VCS roots kept an empty
    #    gitlab_credentials_password forever, no matter how many times bootstrap re-ran.
    #
    #    A build type existing (step 3) does NOT mean the project accepts writes yet: right after
    #    DSL import, the project can stay "read only, project settings format switched to Kotlin,
    #    waiting for initial commit from VCS to be applied" for well over a minute — confirmed
    #    live, every PUT/POST below got HTTP 500 with exactly that reason immediately after step 3
    #    succeeded, ~82s before the project actually became writable. So this batch of writes gets
    #    retried as a whole (not each call individually — read-only is a project-wide state, not a
    #    per-VCS-root one) against the shared deadline, instead of firing once and trusting it.
    vcs_resp = tc.get("/app/rest/vcs-roots?fields=vcs-root(id,project(id))")
    demo_vcs_roots = [
        v for v in vcs_resp.json().get("vcs-root", [])
        if v["project"]["id"] != "_Root"
    ]
    param_payload = json.dumps(
        {
            "name": "gitlab_credentials_password",
            "value": gitlab_token,
            "type": {"rawValue": "password display='normal'"},
        }
    )
    track_project_ids = {v["project"]["id"] for v in demo_vcs_roots}

    def _inject_credentials():
        statuses = [
            tc.put(
                f"/app/rest/vcs-roots/id:{v['id']}/properties/secure:password",
                gitlab_token,
                content_type="text/plain",
            )[0]
            for v in demo_vcs_roots
        ]
        statuses += [
            tc.post(f"/app/rest/projects/id:{project_id}/parameters", param_payload)[0]
            for project_id in track_project_ids
        ]
        return all(200 <= status < 300 for status in statuses)

    log("  injecting GitLab credential into demo VCS roots...")
    if not _poll_until(
        _inject_credentials,
        deadline,
        interval=5,
        waiting_message="  project still read-only (settings format switching) — retrying...",
    ):
        log(f"ERROR: could not inject GitLab credentials within {config.TEAMCITY_PROVISION_TIMEOUT_SECONDS}s "
            "— the project tree stayed read-only the whole time.")
        log("Re-run bootstrap.")
        return False
    log(f"  injected GitLab credential into {len(demo_vcs_roots)} demo VCS root(s) across "
        f"{len(track_project_ids)} track project(s): {', '.join(sorted(track_project_ids))}")

    # 5. Agent authorization: documented as a manual UI step, but has a REST escape hatch.
    # PUT, not POST — confirmed live: POST to this endpoint returns 405 Method Not Allowed
    # (this REST call was apparently never actually exercised before; same-host agents do NOT
    # auto-authorize here despite the docs suggesting they might).
    #
    # Found by connection state, not a hardcoded id: TeamCity assigns a fresh id (and a
    # disambiguated name, e.g. "...-1") to every new agent registration, while an old,
    # disconnected registration from a previous stand keeps its own id — and stays authorized.
    # Confirmed live after a container-only restart with the TeamCity data volume kept: the old
    # id stayed authorized-but-disconnected, the reconnecting agent came up under a new,
    # unauthorized id, and hardcoding id:1 authorized the wrong one, leaving the real agent stuck
    # in the queue with no visible error.
    def _connected_agents():
        resp = tc.get("/app/rest/agents?locator=connected:true,authorized:any&fields=agent(id,name,authorized)")
        return resp.json().get("agent", [])

    if not _poll_until(
        lambda: bool(_connected_agents()),
        deadline,
        interval=5,
        waiting_message="  waiting for a build agent to connect...",
    ):
        log(f"ERROR: no build agent connected within {config.TEAMCITY_PROVISION_TIMEOUT_SECONDS}s.")
        return False

    for agent in _connected_agents():
        if agent["authorized"]:
            continue
        status, body = tc.put(
            f"/app/rest/agents/id:{agent['id']}/authorizedInfo",
            json.dumps({"status": True, "text": "authorized by bootstrap"}),
        )
        if status != 200:
            log(f"ERROR: failed to authorize agent {agent['id']} (HTTP {status}): {body}")
            return False
        log(f"  authorized build agent (id {agent['id']}, {agent['name']})")

    log("TeamCity provisioned: versioned settings import from ci-infra owns the project tree")
    log("(CxxCiDemo_Main: base_build template + BuildCImage + ProjectA/B/C/D/E + Result) — edit")
    log("repos/ci-infra/main/.teamcity/settings.kts or the TeamCity UI, both land in git. See ADR 0004.")
    return True
