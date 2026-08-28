#!/usr/bin/env bash
# One-shot bootstrap: run once after `docker compose up -d` has settled.
# Creates the 3 GitLab repos, pushes bootstrap/<repo>/<branch>/ content into them — one branch per
# subdirectory, see push_repo_content() below — (including bootstrap/ci-infra/main/.teamcity/settings.kts
# — the actual TeamCity project tree definition), then
# points TeamCity's Kotlin DSL versioned settings at ci-infra in import mode (git/UI is the
# source of truth — see provision_teamcity() below) and injects the one thing the DSL itself
# cannot carry: the GitLab credential used by the tree's own VCS roots.
#
# See:
#   - .scratch/teamcity-cxx-ci/research/gitlab-headless-bootstrap.md   (GitLab side)
#   - .scratch/teamcity-cxx-ci/research/teamcity-headless-bootstrap.md (TeamCity side)
#   - .scratch/teamcity-cxx-ci/issues/08-task-bootstrap-script.md      (this ticket's Answer)
#   - docs/adr/0004-kotlin-dsl-versioned-settings-import-mode.md       (current mechanism)
#   - docs/adr/0003-rest-provisioning-instead-of-kotlin-dsl.md         (superseded — kept for history)
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")"

# shellcheck disable=SC1091
[ -f .env ] && source .env
GITLAB_HOSTNAME="${GITLAB_HOSTNAME:-gitlab.local}"
GITLAB_HTTP_PORT="${GITLAB_HTTP_PORT:-8929}"
TEAMCITY_HTTP_PORT="${TEAMCITY_HTTP_PORT:-8111}"
GITLAB_URL="http://${GITLAB_HOSTNAME}:${GITLAB_HTTP_PORT}"
TEAMCITY_URL="http://localhost:${TEAMCITY_HTTP_PORT}"
# GITLAB_HOSTNAME (gitlab.local) is for anything resolved on the HOST — this script's own git/curl
# calls below run directly on the host shell, and the host only has gitlab.local in /etc/hosts
# (README step 2), not the plain compose service name. GITLAB_CONTAINER_HOST is for URLs that a
# container on the cxxci network resolves instead — Compose already registers the plain service
# name automatically, no docker-compose.yml change needed. Used for CiInfraVersionedSettingsVcs's
# url below, since that's fetched by teamcity-server, not this script. Verified live before
# adopting it: a scratch VCS root + real TeamCity build against http://gitlab:8929/... succeeded.
GITLAB_CONTAINER_HOST="gitlab"

# These are local docker-published services, not something to route through an ambient
# http_proxy/https_proxy — but curl honors those env vars even for localhost unless told
# otherwise. Force-bypass so a corporate/system proxy can't intercept these calls.
if [ -n "${NO_PROXY:-}" ]; then
    export NO_PROXY="${NO_PROXY},localhost,127.0.0.1,${GITLAB_HOSTNAME}"
else
    export NO_PROXY="localhost,127.0.0.1,${GITLAB_HOSTNAME}"
fi
export no_proxy="${NO_PROXY}"

REPOS=(ci-infra demo-project-a demo-project-b)

log() { echo "[bootstrap] $*" >&2; }
die() { echo "[bootstrap] ERROR: $*" >&2; exit 1; }

require_cmd() { command -v "$1" >/dev/null 2>&1 || die "'$1' is required but not found on PATH"; }
require_cmd curl
require_cmd docker
require_cmd git
require_cmd python3

# --- 1. Wait for GitLab readiness ------------------------------------------------------------
# Per research: /-/readiness (not /-/health or /health_check) is the endpoint the docs point at
# for automation. Polled via `docker compose exec` (i.e. from 127.0.0.1 inside the container),
# NOT via the published host port: GitLab's monitoring endpoints 404 (not 403 — this is
# deliberate obfuscation) for any source IP not in monitoring_whitelist, and empirically the
# source IP docker presents for host-published-port traffic on this kind of setup does not
# reliably match the compose network's configured subnet (hairpin NAT can present the host's own
# LAN-facing IP instead of the bridge gateway) — so a whitelist entry chosen in advance can't be
# trusted. Checking from inside the container sidesteps the whitelist question entirely.
wait_for_gitlab() {
    log "waiting for GitLab readiness (polled from inside the container; this can take several minutes on first boot)..."
    local i=0
    until docker compose exec -T gitlab curl -sf "http://127.0.0.1:${GITLAB_HTTP_PORT}/-/readiness" >/dev/null 2>&1; do
        i=$((i + 1))
        [ "$i" -gt 90 ] && die "GitLab did not become ready in time (waited $((i * 10))s). Check: docker compose logs gitlab"
        sleep 10
    done
    log "GitLab is ready."
}

# --- 2. Headless PAT for root, scoped for API + git push -------------------------------------
# Per research: gitlab-rails runner is the documented headless path; token must be exactly 20
# chars. 'api' scope covers project creation; 'write_repository' explicitly covers git push.
create_gitlab_token() {
    local token
    token="$(tr -dc 'A-Za-z0-9' </dev/urandom | head -c 20)"
    log "minting a GitLab PAT for root via gitlab-rails runner..."
    docker compose exec -T gitlab gitlab-rails runner "
        token = User.find_by_username('root').personal_access_tokens.create(
          scopes: ['api', 'write_repository'],
          name: 'ci_cxx bootstrap',
          expires_at: 365.days.from_now
        )
        token.set_token('${token}')
        token.save!
    " || die "failed to create GitLab PAT — see gitlab-rails output above"
    echo "$token"
}

# --- 3. Create the 3 GitLab projects via REST -------------------------------------------------
create_gitlab_repo() {
    local name="$1" token="$2"
    log "creating GitLab project '${name}'..."
    local status
    status="$(curl -s --noproxy '*' -o /tmp/gitlab-create-${name}.json -w '%{http_code}' \
        --request POST \
        --header "PRIVATE-TOKEN: ${token}" \
        --data-urlencode "name=${name}" \
        --data-urlencode "visibility=private" \
        "${GITLAB_URL}/api/v4/projects")"
    if [ "$status" = "201" ]; then
        log "  created."
    elif [ "$status" = "400" ] && grep -q 'has already been taken' "/tmp/gitlab-create-${name}.json" 2>/dev/null; then
        log "  already exists, skipping (bootstrap.sh is safe to re-run)."
    else
        die "unexpected GitLab API response ($status) creating '${name}': $(cat /tmp/gitlab-create-${name}.json 2>/dev/null)"
    fi
}

# --- 4. Push bootstrap/<repo>/<branch>/ content into each repo, one branch per subdirectory ----
# Each subdirectory of bootstrap/<repo>/ is an independent, pre-made branch: adding a branch to
# seed is just adding a directory, no script changes needed (see ADR 0007). Every branch is
# pushed as its own orphan commit (fresh `git init`, single commit, done from a throwaway copy so
# we never run `git init` inside bootstrap/<repo>/<branch>/ itself — that would nest a second
# .git inside this repo's own working tree) — there's no shared history between branches, since
# the directories hold deliberately different content, not a fork of one codebase.
#
# `main` is always pushed first when present: GitLab makes the first branch ever pushed to an
# empty repo its default branch, so pushing `main` first keeps that default what everyone expects
# regardless of directory-listing order.
#
# Idempotency: GitLab protects the default branch from force-push out of the box, so re-running
# with --force (the original approach) gets rejected by GitLab's own pre-receive hook. Instead,
# each branch is checked and pushed independently — only push a branch that genuinely doesn't
# exist on the remote yet (`git ls-remote` against a missing branch returns nothing / non-zero) —
# a plain, non-force push of a new branch needs no special permission and respects branch
# protection as-is. Checking per branch (not per repo) also means re-running bootstrap.sh after
# adding a new branch directory to an already-bootstrapped repo pushes just the new branch,
# instead of skipping the whole repo because *some* branch already exists.
push_repo_content() {
    local name="$1" token="$2"
    local repo_dir="bootstrap/${name}"
    [ -d "$repo_dir" ] || die "bootstrap/${name} does not exist — did tickets 05/06/07 run first?"

    local url="http://root:${token}@${GITLAB_HOSTNAME}:${GITLAB_HTTP_PORT}/root/${name}.git"

    local branches=()
    [ -d "${repo_dir}/main" ] && branches+=("main")
    local d branch
    for d in "${repo_dir}"/*/; do
        branch="$(basename "$d")"
        [ "$branch" = "main" ] && continue
        branches+=("$branch")
    done

    for branch in "${branches[@]}"; do
        push_repo_branch "$name" "$branch" "$url"
    done
}

push_repo_branch() {
    local name="$1" branch="$2" url="$3"
    local src="bootstrap/${name}/${branch}"

    if git -c http.proxy= ls-remote --exit-code "$url" "$branch" >/dev/null 2>&1; then
        log "  '${name}' already has a '${branch}' branch on GitLab, skipping push (bootstrap.sh is safe to re-run)."
        return 0
    fi

    local tmp
    tmp="$(mktemp -d)"
    cp -a "${src}/." "${tmp}/"

    (
        cd "$tmp"
        git init -q
        git checkout -q -b "$branch"
        git add -A
        git -c user.email="bootstrap@ci-infra.local" -c user.name="ci_cxx bootstrap" \
            commit -q -m "initial content from ci_cxx bootstrap.sh"
        git remote add origin "$url"
        log "pushing '${name}' branch '${branch}' to GitLab..."
        git -c http.proxy= push -q -u origin "$branch"
    )
    rm -rf "$tmp"
}

# --- 5. TeamCity: point versioned settings at ci-infra, let DSL own the project tree -----------
# Kotlin DSL versioned settings was tried first and abandoned early in this project's life —
# teamcity-server has no outbound internet access, and the .teamcity/pom.xml the DSL compiler
# generates declares download.jetbrains.com as its first Maven repository. That repo is
# unreachable, BUT the second declared repository is http://localhost:8111/app/dsl-plugins-repository
# — the server's own local mirror, generated from installed plugins without ever touching the
# network — and it turns out to carry everything the compiler needs. DSL compilation was
# re-verified fully offline (scratch project + the real tree); what actually blocked the original
# attempt was never re-diagnosed, but a concrete prerequisite surfaced along the way (Kotlin format
# requires ids prefixed by their project id) and had to be fixed before the scratch project would
# even accept the config change. See ADR 0004 for the full story and ADR 0003 (superseded) for the
# original, incorrect "impossible" conclusion.
#
# So: this script no longer builds the project tree via REST. It only does what the DSL itself
# cannot — bootstrap the one VCS root versioned settings needs to fetch ci-infra in the first
# place, point versioned settings at it in import mode (git/UI is the source of truth from here),
# and inject the GitLab credential the tree's own VCS roots need. That credential can't live in
# git even as a reference: VCS root `password = "%gitlab_credentials_password%"` in settings.kts
# gets resolved and baked into a fresh server-local credentialsJSON at DSL-apply time, and does
# NOT keep re-resolving the parameter on later syncs — so bootstrap.sh sets the real secret
# directly on every VCS root that needs it via REST, every run. Idempotent and cheap either way.
#
# NOT via ${TEAMCITY_URL} (the published host port): confirmed empirically that something (a
# local squid instance) answers on 127.0.0.1:8111 ahead of/instead of docker's published port,
# with a real "Server: squid/6.13" response — not a proxy curl can be told to bypass, the port
# itself is occupied on the host. A throwaway container on the cxxci network reaches
# teamcity-server directly over the compose network's own DNS instead.
teamcity_super_user_token() {
    docker compose logs teamcity-server 2>/dev/null \
        | grep -o 'Super user authentication token: [^ ]*' \
        | tail -1 \
        | awk '{print $NF}'
}

# tc_post <path> <json-body-string> -> prints "<http_status>\n<response_body>"
# Body goes in over stdin (`--data @-`, `docker run -i`), not a bind-mounted temp file: an earlier
# version wrote the payload to a host temp dir and bind-mounted it in, which broke on at least one
# machine with "curl: option --data: error encountered when reading a file" — curlimages/curl runs
# as a non-root UID (100) inside the container, and depending on the host's mount/SELinux/Docker
# setup that UID isn't guaranteed to be able to read a bind-mounted host path even at 644/755.
# Piping over stdin never touches the host filesystem, so no host-side permission state can break it.
tc_post() {
    local path="$1" body="$2"
    local out
    out="$(printf '%s' "$body" | docker run --rm -i --network cxxci curlimages/curl:latest \
        curl -s -w '\n%{http_code}' -u ":${TC_SU_TOKEN}" \
        -H 'Content-Type: application/json' -H 'Accept: application/json' \
        --request POST "http://teamcity-server:8111${path}" --data @-)"
    printf '%s\n%s' "$(echo "$out" | tail -1)" "$(echo "$out" | sed '$d')"
}

# tc_get_status <path> -> http status only (used for idempotency existence checks)
tc_get_status() {
    docker run --rm --network cxxci curlimages/curl:latest \
        curl -s -o /dev/null -w '%{http_code}' -u ":${TC_SU_TOKEN}" \
        "http://teamcity-server:8111$1"
}

# tc_put_text <path> <text-body> -> http status (used for single-value settings: idempotent by nature)
tc_put_text() {
    docker run --rm --network cxxci curlimages/curl:latest \
        curl -s -o /dev/null -w '%{http_code}' -u ":${TC_SU_TOKEN}" -H 'Content-Type: text/plain' \
        --request PUT "http://teamcity-server:8111$1" --data-binary "$2"
}

# NOTE on the "teamcity:" prefix on branchSpec below: a VCS root created via REST with a plain
# "branchSpec" property is silently ignored for branch matching (confirmed by comparing against
# a UI-created VCS root, which TeamCity itself writes as "teamcity:branchSpec") — this was the
# real cause behind ticket 09's long invalid_branch_name investigation, not a branchSpec syntax
# issue as originally suspected.
vcs_root_json() {
    local id="$1" name="$2" repo="$3" gitlab_token="$4"
    cat <<JSON
{
  "id": "${id}", "name": "${name}", "vcsName": "jetbrains.git",
  "project": {"id": "_Root"},
  "properties": {"property": [
    {"name": "url", "value": "http://${GITLAB_CONTAINER_HOST}:${GITLAB_HTTP_PORT}/root/${repo}.git"},
    {"name": "branch", "value": "refs/heads/main"},
    {"name": "teamcity:branchSpec", "value": "+:refs/heads/*"},
    {"name": "authMethod", "value": "PASSWORD"},
    {"name": "username", "value": "root"},
    {"name": "secure:password", "value": "${gitlab_token}"}
  ]}
}
JSON
}

provision_teamcity() {
    local gitlab_token="$1"
    TC_SU_TOKEN="$(teamcity_super_user_token)"
    if [ -z "$TC_SU_TOKEN" ]; then
        log "Could not find a TeamCity Super User token in the logs yet."
        log "This means the one unavoidable manual step (README.md step 4: the first-start"
        log "browser wizard) hasn't been completed yet. Complete it, then re-run this script."
        return 1
    fi

    log "provisioning TeamCity versioned settings (git/UI owns the project tree from here)..."

    # 1. The one VCS root the DSL itself cannot create: without it, versioned settings has
    #    nothing to fetch ci-infra's .teamcity/settings.kts from in the first place.
    #
    # NOTE: every REST call in this function checks its status explicitly and fails loudly
    # (rather than silently continuing) — a real fresh-machine run surfaced the alternative:
    # this VCS root creation returned a non-200 (transient GitLab/network hiccup right after
    # first boot), the old code didn't check, and the script happily reported "provisioned"
    # while versioned settings was never actually wired up — confusing to debug after the fact.
    if [ "$(tc_get_status /app/rest/vcs-roots/id:CiInfraVersionedSettingsVcs)" != "200" ]; then
        local vcs_create_out vcs_create_status
        vcs_create_out="$(tc_post /app/rest/vcs-roots "$(vcs_root_json CiInfraVersionedSettingsVcs "ci-infra (versioned settings)" ci-infra "$gitlab_token")")"
        vcs_create_status="$(echo "$vcs_create_out" | head -1)"
        if [ "$vcs_create_status" != "200" ]; then
            log "ERROR: failed to create VCS root CiInfraVersionedSettingsVcs (HTTP ${vcs_create_status})."
            log "Response: $(echo "$vcs_create_out" | tail -n +2)"
            log "This can happen if GitLab wasn't fully ready for git operations yet even though"
            log "readiness passed, or a transient network issue. Re-run bootstrap.sh."
            return 1
        fi
        log "  created VCS root CiInfraVersionedSettingsVcs"
    fi

    # 2. Point _Root's versioned settings at it: format=kotlin, buildSettingsMode=useFromVCS
    #    (git/UI wins on conflict — UI edits still work, they auto-commit back into ci-infra).
    #    On a genuinely fresh server there's nothing to reconcile (the pushed ci-infra content
    #    already carries .teamcity/settings.kts, and the server has no competing project tree
    #    yet), so the plain PUT below should just succeed. The importDecision fallback handles
    #    a re-run against a server that already has a *different* tree (e.g. this same demo
    #    stand reprovisioned without wiping the datadir) — prefer git, since that's now this
    #    project's whole point.
    local vs_config
    vs_config='{
        "synchronizationMode": "enabled",
        "vcsRootId": "CiInfraVersionedSettingsVcs",
        "format": "kotlin",
        "buildSettingsMode": "useFromVCS",
        "allowUIEditing": true,
        "storeSecureValuesOutsideVcs": true
    }'
    local vs_status
    vs_status="$(docker run --rm --network cxxci curlimages/curl:latest \
        curl -s -o /dev/null -w '%{http_code}' -u ":${TC_SU_TOKEN}" -H 'Content-Type: application/json' -H 'Accept: application/json' \
        --request PUT "http://teamcity-server:8111/app/rest/projects/id:_Root/versionedSettings/config" --data "$vs_config")"
    if [ "$vs_status" = "500" ]; then
        vs_status="$(docker run --rm --network cxxci curlimages/curl:latest \
            curl -s -o /dev/null -w '%{http_code}' -u ":${TC_SU_TOKEN}" -H 'Content-Type: application/json' -H 'Accept: application/json' \
            --request PUT "http://teamcity-server:8111/app/rest/projects/id:_Root/versionedSettings/config" \
            --data "$(echo "$vs_config" | sed 's/}$/,"importDecision":"importFromVCS"}/')")"
    fi
    if [ "$vs_status" != "200" ]; then
        log "ERROR: failed to enable versioned settings (HTTP ${vs_status})."
        log "Check that VCS root CiInfraVersionedSettingsVcs exists and is valid, then re-run bootstrap.sh."
        return 1
    fi
    log "  versioned settings pointed at ci-infra (Kotlin, import mode)"

    # 3. Wait for the DSL to actually have applied — NOT by matching the status message text
    #    ("repository is up-to-date" can legitimately be the FIRST message on a fresh server,
    #    before the DSL has run even once, which would break out of a text-matching loop too
    #    early). Poll the thing we actually need instead: does the tree the DSL is supposed to
    #    create exist yet. Same ~90s budget as everywhere else in this script.
    log "  waiting for DSL import to apply..."
    local i=0
    while [ "$i" -lt 30 ]; do
        [ "$(tc_get_status /app/rest/buildTypes/id:CxxCiDemo_Main_DemoProjectA)" = "200" ] && break
        i=$((i + 1))
        sleep 3
    done
    log "  $(docker run --rm --network cxxci curlimages/curl:latest \
        curl -s -u ":${TC_SU_TOKEN}" -H 'Accept: application/json' \
        "http://teamcity-server:8111/app/rest/projects/id:_Root/versionedSettings/status")"

    # 4. The GitLab credential the demo VCS roots need. NOT carried by the DSL: settings.kts
    #    references it as "%gitlab_credentials_password%" for documentation, but TeamCity bakes
    #    that reference into a static per-installation credentialsJSON at apply time rather than
    #    re-resolving it live — so bootstrap.sh sets the real value directly on every VCS root
    #    that needs it, every run. Idempotent (just overwrites with the same value on a re-run).
    if [ "$(tc_get_status /app/rest/buildTypes/id:CxxCiDemo_Main_DemoProjectA)" = "200" ]; then
        for vcs in CxxCiDemo_Main_DemoProjectA CxxCiDemo_Main_DemoProjectB; do
            docker run --rm --network cxxci curlimages/curl:latest \
                curl -s -o /dev/null -u ":${TC_SU_TOKEN}" -H 'Content-Type: text/plain' \
                --request PUT "http://teamcity-server:8111/app/rest/vcs-roots/id:${vcs}/properties/secure:password" \
                --data-binary "${gitlab_token}"
        done
        local param_payload
        param_payload="$(printf '{"name":"gitlab_credentials_password","value":"%s","type":{"rawValue":"password display=%s"}}' \
            "$gitlab_token" "'normal'")"
        tc_post /app/rest/projects/id:CxxCiDemo_Main/parameters "$param_payload" >/dev/null
        log "  injected GitLab credential into the demo project's VCS roots"
    else
        log "  demo project tree not present yet (DSL import may still be settling) —"
        log "  re-run bootstrap.sh once CxxCiDemo_Main_DemoProjectA exists to inject credentials."
    fi

    # 5. Agent authorization: documented as a manual UI step, but has a REST escape hatch (ticket
    #    01's research). Same-host agents auto-authorize per the docs, but do this defensively.
    local agent_authorized
    agent_authorized="$(docker run --rm --network cxxci curlimages/curl:latest \
        curl -s -u ":${TC_SU_TOKEN}" -H 'Accept: application/json' "http://teamcity-server:8111/app/rest/agents/id:1?fields=authorized")"
    if ! echo "$agent_authorized" | grep -q '"authorized":true'; then
        tc_post /app/rest/agents/id:1/authorizedInfo '{"status":true,"text":"authorized by bootstrap"}' >/dev/null
        log "  authorized build agent"
    fi

    log "TeamCity provisioned: versioned settings import from ci-infra owns the project tree"
    log "(CxxCiDemo_Main: base_build template + BuildCImage + DemoProjectA/B + Result) — edit"
    log "bootstrap/ci-infra/main/.teamcity/settings.kts or the TeamCity UI, both land in git. See ADR 0004."
}

main() {
    wait_for_gitlab
    local token
    token="$(create_gitlab_token)"
    for repo in "${REPOS[@]}"; do
        create_gitlab_repo "$repo" "$token"
        push_repo_content "$repo" "$token"
    done
    provision_teamcity "$token" || log "TeamCity provisioning incomplete — see messages above."
    log "done."
}

main "$@"
