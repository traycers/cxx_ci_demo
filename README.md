# cxx_ci_demo

Demonstrating a multi-release C++ CI strategy. Docker-compose demo CI stand: GitLab + TeamCity building C++ projects in containers. See `CONTEXT.md` for the glossary and `docs/adr/` for the architecture decisions. The full plan lives on the wayfinder map at `.scratch/teamcity-cxx-ci/map.md`.

## Bringing the stand up

1. `cp .env.example .env` and fill in `GITLAB_ROOT_PASSWORD` (a `.env` with a generated password already exists locally from setup — check before overwriting it).
2. Add `127.0.0.1 gitlab.local` to your host's `/etc/hosts` (or whatever hostname `GITLAB_HOSTNAME` is set to). This is the one piece GitLab's docs don't cover — the compose network gives sibling containers DNS resolution for free, but the host OS needs this entry to resolve the same hostname the way TeamCity's VCS roots and clone links will use it. See `.scratch/teamcity-cxx-ci/research/gitlab-headless-bootstrap.md` §2.
3. `docker compose up -d`
4. **One unavoidable manual step**: open `http://localhost:${TEAMCITY_HTTP_PORT:-8111}` and click through the TeamCity first-start wizard once (confirm data dir, accept EULA, create the admin account). No headless equivalent exists in the current image — see `.scratch/teamcity-cxx-ci/research/teamcity-headless-bootstrap.md` §1.
5. Grab the TeamCity Super User token for scripted access:
   `docker compose logs teamcity-server | grep "Super user authentication token:"`
6. GitLab is reachable at `http://gitlab.local:${GITLAB_HTTP_PORT:-8929}` with `root` / the password from `.env`.

Everything past this point (repo creation, Kotlin DSL, demo projects) is automated by `bootstrap.sh` — see ticket 08 on the map.

## Adding a new release

The `cxx_ci_demo` project in TeamCity is split into one directory per release under
`bootstrap/ci-infra/.teamcity/cxx_ci_demo/` (currently just `main/`). See
`docs/adding-a-release.md` for the step-by-step procedure and the `<config_name>`/`<config_name>-*`
branch naming convention.
