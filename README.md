# cxx_ci_demo

🇬🇧 English · [🇷🇺 Русский](docs/ru/README.md) · [🇨🇳 中文](docs/zh/README.md)


Docker-compose demo CI stand: GitLab + TeamCity building C++ projects in containers. See `CONTEXT.md` for the glossary and `docs/en/adr/` for the architecture decisions. The full plan lives on the wayfinder map at `.scratch/teamcity-cxx-ci/map.md`. Documentation is maintained in English, Russian, and Chinese (`docs/ru/`, `docs/zh/`) — see [ADR 0006](docs/en/adr/0006-trilingual-docs-mirror-tree.md) for the convention.

## Bringing the stand up

1. `cp .env.example .env` and fill in `GITLAB_ROOT_PASSWORD` (a `.env` with a generated password already exists locally from setup — check before overwriting it).
2. Add `127.0.0.1 gitlab.local` to your host's `/etc/hosts` (or whatever hostname `GITLAB_HOSTNAME` is set to). This is the one piece GitLab's docs don't cover — the compose network gives sibling containers DNS resolution for free, but the host OS needs this entry to resolve the same hostname the way TeamCity's VCS roots and clone links will use it. See `.scratch/teamcity-cxx-ci/research/gitlab-headless-bootstrap.md` §2.
3. `docker compose up -d`
4. **One unavoidable manual step**: open `http://localhost:${TEAMCITY_HTTP_PORT:-8111}` and click through the TeamCity first-start wizard once (confirm data dir, accept EULA, create the admin account). No headless equivalent exists in the current image — see `.scratch/teamcity-cxx-ci/research/teamcity-headless-bootstrap.md` §1.
5. GitLab is reachable at `http://gitlab.local:${GITLAB_HTTP_PORT:-8929}` with `root` / the password from `.env`.
6. `docker compose run --rm bootstrap` — creates the 3 GitLab repos, pushes `repos/<repo>/<branch>/` seed content into them, and points TeamCity's versioned settings at `ci-infra`. Runs as a one-shot container attached to the `cxxci` network directly (see ADR 0008) rather than a host script, so nothing here depends on host-side `curl`/`git`/`docker` versions. Safe to re-run.

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
  can look similar at a glance. If this hits `demo-project-a`/`demo-project-b`'s own VCS roots
  specifically, it usually means the `bootstrap` container didn't get to its credential-injection
  step (step 4 of `provision_teamcity()` in `scripts/bootstrap/teamcity_ops.py`) — which only runs
  once `CxxCiDemo_Main_DemoProjectA` exists, i.e. only after versioned settings successfully
  imported the DSL tree. Re-run `docker compose run --rm bootstrap`; every REST call it makes
  checks its response status and fails loudly with the actual HTTP code and response body instead
  of silently continuing (an earlier version didn't, and a fresh-machine run showed exactly this:
  versioned settings silently failed to enable, so the tree — and the credential injection — never
  happened, and the confusing "Access denied" a step later was the real, but delayed, symptom).
- **`gitlab` unreachable from the `bootstrap` container** (connection refused/timeout, not an auth
  error) — check the containers are actually on the `cxxci` network (`docker compose ps`). Unlike
  the browser step above, the `bootstrap` container talks to `gitlab`/`teamcity-server` by their
  plain compose service names over the `cxxci` network directly — it never goes through
  `gitlab.local`, a published host port, or a host-side proxy, so host-level network quirks
  (hairpin NAT, a local proxy intercepting `localhost`) that affect the browser/host `git` don't
  apply to it. See ADR 0008.

## Adding a new release

The `cxx_ci_demo` project in TeamCity is split into one directory per release under
`repos/ci-infra/main/.teamcity/cxx_ci_demo/` (currently just a release named `main`, not to
be confused with the outer `repos/ci-infra/main/` — that one is the seeded git branch, see
ADR 0007/0008). See
`docs/en/adding-a-release.md` for the step-by-step procedure and the `<config_name>`/`<config_name>-*`
branch naming convention.
