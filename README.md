# cxx_ci_demo

🇬🇧 English · [🇷🇺 Русский](docs/ru/README.md) · [🇨🇳 中文](docs/zh/README.md)


Docker-compose demo CI stand: GitLab + TeamCity building C++ projects in containers. See `CONTEXT.md` for the glossary and `docs/en/adr/` for the architecture decisions. The full plan lives on the wayfinder map at `.scratch/teamcity-cxx-ci/map.md`. Documentation is maintained in English, Russian, and Chinese (`docs/ru/`, `docs/zh/`) — see [ADR 0006](docs/en/adr/0006-trilingual-docs-mirror-tree.md) for the convention.

## Bringing the stand up

1. `cp .env.example .env` and fill in `GITLAB_ROOT_PASSWORD` (a `.env` with a generated password already exists locally from setup — check before overwriting it).
2. `docker compose up -d`
3. **One unavoidable manual step**: open `http://localhost:${TEAMCITY_HTTP_PORT:-8111}` and click through the TeamCity first-start wizard once (confirm data dir, accept EULA, create the admin account). No headless equivalent exists in the current image — see `.scratch/teamcity-cxx-ci/research/teamcity-headless-bootstrap.md` §1.
4. GitLab is reachable at `http://localhost:${GITLAB_HTTP_PORT:-8929}` with `root` / the password from `.env` — no `/etc/hosts` entry needed, GitLab doesn't reject requests on a Host header mismatch by default, so the published port on `localhost` just works. (Clone URLs shown in GitLab's own UI use the compose service name, `gitlab`, since that's what sibling containers need; only relevant if you're copying a clone URL from the UI rather than using TeamCity's own VCS roots, which already point at `gitlab` directly.)
5. `docker compose run --build --rm bootstrap` — creates the 6 GitLab repos (`ci-infra` and the five `project_*`), pushes `repos/<repo>/<branch>/` seed content into them, and points TeamCity's versioned settings at `ci-infra`. Runs as a one-shot container attached to the `cxxci` network directly (see ADR 0008) rather than a host script, so nothing here depends on host-side `curl`/`git`/`docker` versions. Safe to re-run. **Always pass `--build`**: the `repos/` seed content is baked into the image at build time (`scripts/bootstrap/Dockerfile`), and `docker compose run` without `--build` silently reuses a stale image if one already exists — the container then pushes outdated content, and since `push_repo_content()` skips a branch that already exists on GitLab (ADR 0007), a plain re-run afterwards won't fix it either. If this already happened, force-pushing corrected content over the affected branches (temporarily unprotecting them) and re-running bootstrap to re-inject credentials (DSL reimport clears them from the recreated VCS roots) recovers the stand.

## Troubleshooting

- **`docker compose up` fails mounting `/opt/buildagent/*`** (permission denied): the docker
  daemon needs to be able to create/own `/opt/buildagent` on the host. This path is not
  configurable — it's baked into the `jetbrains/teamcity-agent` image itself, so the host side
  has to be this exact path too (see the comment on `teamcity-agent` in `docker-compose.yml` for
  why). Creating/owning a directory under `/opt` requires root, which a rootless Docker install
  or a host account without root doesn't have. If you hit that, do this once, manually, as a
  human with `sudo` on the host (not as part of `docker compose up`/`bootstrap`):
  `sudo mkdir -p /opt && sudo ln -s /path/you/own /opt/buildagent` — then re-run
  `docker compose up`. From then on the real data lives under the directory you own; Docker and
  the agent still see it at `/opt/buildagent` via the symlink.
- **Docker installed via `snap` is not supported.** Symptom: the `teamcity-agent` container fails
  to write under `/opt/buildagent` even though the host user owns that path (including the symlink
  workaround above) — snap's confinement mounts the filesystem read-only for the `docker` snap in a
  way that breaks this bind mount. Fix: uninstall the snap package and install Docker from the
  official APT/YUM repository instead, then re-run `docker compose up`.
