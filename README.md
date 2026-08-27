# cxx_ci_demo

🇬🇧 English · [🇷🇺 Русский](docs/ru/README.md) · [🇨🇳 中文](docs/zh/README.md)


Docker-compose demo CI stand: GitLab + TeamCity building C++ projects in containers. See `CONTEXT.md` for the glossary and `docs/en/adr/` for the architecture decisions. The full plan lives on the wayfinder map at `.scratch/teamcity-cxx-ci/map.md`. Documentation is maintained in English, Russian, and Chinese (`docs/ru/`, `docs/zh/`) — see [ADR 0006](docs/en/adr/0006-trilingual-docs-mirror-tree.md) for the convention.

## Bringing the stand up

1. `cp .env.example .env` and fill in `GITLAB_ROOT_PASSWORD` (a `.env` with a generated password already exists locally from setup — check before overwriting it).
2. Add `127.0.0.1 gitlab.local` to your host's `/etc/hosts` (or whatever hostname `GITLAB_HOSTNAME` is set to). This is the one piece GitLab's docs don't cover — the compose network gives sibling containers DNS resolution for free, but the host OS needs this entry to resolve the same hostname the way TeamCity's VCS roots and clone links will use it. See `.scratch/teamcity-cxx-ci/research/gitlab-headless-bootstrap.md` §2.
3. `docker compose up -d`
4. **One unavoidable manual step**: open `http://localhost:${TEAMCITY_HTTP_PORT:-8111}` and click through the TeamCity first-start wizard once (confirm data dir, accept EULA, create the admin account). No headless equivalent exists in the current image — see `.scratch/teamcity-cxx-ci/research/teamcity-headless-bootstrap.md` §1.
5. Grab the TeamCity Super User token for scripted access:
   `docker compose logs teamcity-server | grep "Super user authentication token:"`
6. GitLab is reachable at `http://gitlab.local:${GITLAB_HTTP_PORT:-8929}` with `root` / the password from `.env`.

Everything past this point (repo creation, Kotlin DSL, demo projects) is automated by `bootstrap.sh` — see ticket 08 on the map.

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
  can look similar at a glance — `git` did reach `gitlab.local` and got a real response back from
  GitLab, it just didn't like the password. If this hits `demo-project-a`/`demo-project-b`'s own
  VCS roots specifically, it usually means `bootstrap.sh` didn't get to its credential-injection
  step (step 4 of `provision_teamcity`) — which only runs once `CxxCiDemo_Main_DemoProjectA`
  exists, i.e. only after versioned settings successfully imported the DSL tree. Re-run
  `bootstrap.sh`; every REST call it makes now checks its response status and fails loudly with
  the actual HTTP code and response body instead of silently continuing (an earlier version
  didn't, and a fresh-machine run showed exactly this: versioned settings silently failed to
  enable, so the tree — and the credential injection — never happened, and the confusing
  "Access denied" a step later was the real, but delayed, symptom).
- **`gitlab.local` genuinely unreachable from inside a container** (connection refused/timeout,
  not an auth error) is a different problem — check the containers are actually on the `cxxci`
  network (`docker compose ps`) and that nothing on the host is intercepting traffic on the
  published ports (this repo hit a local proxy doing exactly that during development — see
  `bootstrap.sh`'s comments on why its own REST calls go through a throwaway container on the
  `cxxci` network instead of the published host ports).

## Adding a new release

The `cxx_ci_demo` project in TeamCity is split into one directory per release under
`bootstrap/ci-infra/.teamcity/cxx_ci_demo/` (currently just `main/`). See
`docs/en/adding-a-release.md` for the step-by-step procedure and the `<config_name>`/`<config_name>-*`
branch naming convention.
