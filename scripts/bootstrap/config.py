"""Runtime configuration for the bootstrap container.

This process always runs attached to the compose-managed `cxxci` network (see ADR 0008), so it
reaches every other service by its compose service name (`gitlab`, `teamcity-server`) — never by
a published host port. That's the whole point of running from inside the network: no
hairpin-NAT workaround, no host-side hostname, no proxy bypassing for localhost.
"""

import os

# Strip any inherited proxy env vars: this container only ever talks to gitlab/teamcity-server by
# their internal compose service names, so a proxy is never a legitimate hop for its traffic.
for _var in ("http_proxy", "https_proxy", "HTTP_PROXY", "HTTPS_PROXY"):
    os.environ.pop(_var, None)

GITLAB_HTTP_PORT = os.environ.get("GITLAB_HTTP_PORT", "8929")
GITLAB_HOST = "gitlab"
GITLAB_URL = f"http://{GITLAB_HOST}:{GITLAB_HTTP_PORT}"

TEAMCITY_HOST = "teamcity-server"
TEAMCITY_URL = f"http://{TEAMCITY_HOST}:8111"

# Shared deadline for provision_teamcity()'s wait for the DSL-imported project tree to become
# usable: build types can appear in the REST API within seconds of the DSL import starting, but
# the project can stay "read only, project settings format switched to Kotlin" for well over a
# minute after that — confirmed live, ~10s for import vs. ~82s for the project to become
# writable. Both waits share this one deadline rather than getting 5 minutes each.
TEAMCITY_PROVISION_TIMEOUT_SECONDS = 300

REPOS = [
    "ci-infra",
    "project_a",
    "project_b",
    "project_c",
    "project_d",
    "project_e",
]
REPOS_DIR = "/app/repos"

GIT_AUTHOR_NAME = "ci_cxx bootstrap"
GIT_AUTHOR_EMAIL = "bootstrap@ci-infra.local"


def log(message):
    print(f"[bootstrap] {message}", flush=True)
