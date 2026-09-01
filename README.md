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

- **`docker compose up` fails mounting `/opt/buildagent/*`** (permission denied): that path
  requires the docker daemon to be able to create/own directories under `/opt` — true for a
  normal rootful Docker install, not for rootless Docker or a host account without root. Set
  `BUILDAGENT_DATA_DIR` in `.env` to a directory you actually own (e.g.
  `BUILDAGENT_DATA_DIR=${HOME}/.local/share/cxxci-buildagent`) and re-run. These specifically
  have to be host bind mounts, not named volumes — see the comment on `teamcity-agent` in
  `docker-compose.yml` for why.
- **A VCS root "test connection"/build fails with `HTTP Basic: Access denied` or
  `Authentication failed`**: this is a credential problem, not a network/DNS one, even though it
  can look similar at a glance. If this hits one of the `project_*` VCS roots specifically, it's
  a known race in `provision_teamcity()` (`scripts/bootstrap/teamcity_ops.py`): a build type
  appearing in the REST API doesn't mean the imported project accepts writes yet — right after
  DSL import, the project can stay "read only, project settings format switched to Kotlin" for
  well over a minute, and credential injection needs a write. Bootstrap now retries the whole
  credential-injection batch against a shared 5-minute deadline instead of firing it once and
  trusting the result, so this should self-heal without any manual step. If it still fails, every
  REST call bootstrap makes checks its response status and fails loudly with the actual HTTP code
  (and, for this specific race, `provision_teamcity()` now returns `False` and the container exits
  non-zero instead of misreporting `"done."`) — re-run `docker compose run --rm bootstrap`; if it
  keeps failing past the 5-minute deadline, something else is wrong (e.g. a broken
  `settings.kts`), not just this race.
- **`gitlab` unreachable from the `bootstrap` container** (connection refused/timeout, not an auth
  error) — check the containers are actually on the `cxxci` network (`docker compose ps`). Unlike
  the browser step above, the `bootstrap` container talks to `gitlab`/`teamcity-server` by their
  plain compose service names over the `cxxci` network directly — it never goes through a
  published host port or a host-side proxy, so host-level network quirks (hairpin NAT, a local
  proxy intercepting `localhost`) that affect the browser/host `git` don't apply to it. See ADR
  0008.
