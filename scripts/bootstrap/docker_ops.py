"""Docker-outside-of-Docker helpers: reach sibling compose containers via the mounted socket.

Same pattern already used by `teamcity-agent` in docker-compose.yml (mounted docker.sock, no GID
juggling — see that service's comment). Containers are found by the standard
`com.docker.compose.service` label rather than a hardcoded name, so this doesn't depend on
`COMPOSE_PROJECT_NAME` or container-naming conventions.
"""

import docker

_client = None


def client():
    global _client
    if _client is None:
        _client = docker.from_env()
    return _client


def find_container(compose_service):
    containers = client().containers.list(
        filters={"label": f"com.docker.compose.service={compose_service}"}
    )
    if not containers:
        raise RuntimeError(
            f"no running container found for compose service '{compose_service}' — "
            f"is the stack up (docker compose up -d)?"
        )
    return containers[0]


def exec_in_container(compose_service, cmd):
    """Run cmd (list of args) inside the named compose service's container.

    Returns (exit_code, stdout_text, stderr_text).
    """
    container = find_container(compose_service)
    result = container.exec_run(cmd, demux=True)
    stdout, stderr = result.output
    return (
        result.exit_code,
        (stdout or b"").decode(errors="replace"),
        (stderr or b"").decode(errors="replace"),
    )


def get_logs(compose_service):
    return find_container(compose_service).logs().decode(errors="replace")
