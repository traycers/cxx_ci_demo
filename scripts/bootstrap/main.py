"""Entrypoint: run once after `docker compose up -d` has settled, from inside the `cxxci`
network (`docker compose run --rm bootstrap`). See ADR 0008 for why this runs as a container
instead of a host script, and ADR 0007 for the repos/<repo>/<branch>/ seed-content layout.
"""

import sys

import config
import gitlab_ops
import teamcity_ops

log = config.log


def main():
    gitlab_ops.wait_for_gitlab()
    token = gitlab_ops.create_gitlab_token()
    for repo in config.REPOS:
        gitlab_ops.create_gitlab_repo(repo, token)
        gitlab_ops.push_repo_content(repo, token)

    if not teamcity_ops.provision_teamcity(token):
        log("TeamCity provisioning incomplete — see messages above.")
    log("done.")


if __name__ == "__main__":
    try:
        main()
    except Exception as exc:  # noqa: BLE001 - top-level entrypoint, report and exit non-zero
        log(f"ERROR: {exc}")
        sys.exit(1)
