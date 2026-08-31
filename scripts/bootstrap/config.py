"""Runtime configuration for the bootstrap container.

This process always runs attached to the compose-managed `cxxci` network (see ADR 0008), so it
reaches every other service by its compose service name (`gitlab`, `teamcity-server`) — never by
the host-facing `gitlab.local` hostname or a published host port. That's the whole point of
running from inside the network: no hairpin-NAT workaround, no host-side hostname, no proxy
bypassing for localhost.
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

REPOS = [
    "ci-infra",
    "demo-project-a",
    "demo-project-b",
    "demo-project-c",
    "demo-project-d",
    "demo-project-e",
]
REPOS_DIR = "/app/repos"

GIT_AUTHOR_NAME = "ci_cxx bootstrap"
GIT_AUTHOR_EMAIL = "bootstrap@ci-infra.local"


def log(message):
    print(f"[bootstrap] {message}", flush=True)
