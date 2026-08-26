# Research: Running `gitlab/gitlab-ce` headlessly in docker-compose for a CI demo stand

Scope: primary sources only — `docs.gitlab.com`, the official `gitlab/gitlab-ce` Docker Hub
page, and GitLab's own omnibus/Docker installation docs. Every claim below is followed by
the exact URL it was verified against. Where the official docs are silent or ambiguous, that
is stated explicitly rather than filled in with a guess.

---

## 1. Headless initial root password

**Documented mechanism.** GitLab documents a top-level `GITLAB_ROOT_PASSWORD` environment
variable ("Sets the password for the administrator on installation"), alongside
`GITLAB_ROOT_EMAIL` ("Sets the email address for the administrator on installation") and
`GITLAB_ROOT_USERNAME` ("Sets the username for the administrator on installation").
[Environment variables — docs.gitlab.com](https://docs.gitlab.com/administration/environment_variables/)

GitLab's own single-node install tutorial demonstrates this pattern directly on the command
line for a Linux-package (omnibus) install:

> `sudo GITLAB_ROOT_PASSWORD="strong password" EXTERNAL_URL="https://gitlab.example.com" apt install gitlab-ee`

[Tutorial: Install and secure a single node GitLab instance — docs.gitlab.com](https://docs.gitlab.com/tutorials/install_gitlab_single_node/)

The same tutorial also warns the value can fail to be picked up ("If the password you set
wasn't picked up, read more about resetting the root account password"), pointing to the
password-reset docs as the fallback — which is the closest primary-source signal that this
env var is not unconditionally reliable and is scoped to the very first bootstrap of the
instance. [Reset a user's password — docs.gitlab.com](https://docs.gitlab.com/security/reset_user_password/)
(that page itself documents the Rake task / Rails-console reset path, but does not itself
discuss `GITLAB_ROOT_PASSWORD` or persistence semantics).

**Docker-specific pattern.** The official Docker installation guide does not show bare
`GITLAB_ROOT_PASSWORD` in a `docker run`/compose example. Instead, its documented pattern for
presetting the password in a container is via `GITLAB_OMNIBUS_CONFIG` setting
`gitlab_rails['initial_root_password']` inside `gitlab.rb` syntax — shown in the page's Docker
Swarm/secrets example:

```
external_url 'https://my.domain.com/'
gitlab_rails['initial_root_password'] = File.read('/run/secrets/gitlab_root_password').gsub("\n", "")
```

[Install GitLab in a Docker container — docs.gitlab.com](https://docs.gitlab.com/install/docker/installation/)

The same page documents the default (no password set) behavior: GitLab generates a random
password for `root`, written to `/etc/gitlab/initial_root_password` inside the container,
retrievable with `sudo docker exec -it gitlab grep 'Password:' /etc/gitlab/initial_root_password`,
and that file "is automatically deleted in the first container restart after 24 hours."
[Install GitLab in a Docker container — docs.gitlab.com](https://docs.gitlab.com/install/docker/installation/)

**Caveats (per primary docs, plus explicit gaps):**
- **Administrator account only.** All three of `GITLAB_ROOT_PASSWORD`/`GITLAB_ROOT_EMAIL`/`GITLAB_ROOT_USERNAME`
  are scoped to "the administrator" account created at install time (i.e., `root`, or a
  renamed root account if `GITLAB_ROOT_USERNAME` is also set) — not an arbitrary user.
  [Environment variables — docs.gitlab.com](https://docs.gitlab.com/administration/environment_variables/)
- **Minimum password length:** GitLab's site-wide password policy setting
  `minimum_password_length` "Cannot be less than 8 characters or more than 128 characters."
  This is documented as a general new-user/reset password-policy control, not explicitly
  stated by the docs to gate the initial root password value itself, but it is the closest
  documented lower bound and a safe assumption for a script generating a random password.
  [Custom password length limits — docs.gitlab.com](https://docs.gitlab.com/security/password_length_limits/)
- **First-bootstrap-only, not explicitly spelled out for Docker volumes.** The docs describe
  the env var as applying "on installation" / "at the time of installation" and describe the
  random-password fallback file as tied to first startup, but **no primary-source page found
  in this research explicitly states the precise persistence rule** (e.g., "has no effect if
  the `/etc/gitlab` volume/database already exists from a prior run"). Treat this as an
  inferred, not directly documented, behavior: set it before the container's data volume is
  ever created, and don't rely on it to change the password on a later `docker compose up` of
  an already-initialized volume.

---

## 2. `external_url` / hostname for dual reachability (host browser + other containers)

**What `external_url` actually controls.** GitLab's own docs are explicit that this single
setting is what drives the clone links and web links shown to users:

> "To display the correct repository clone links to your users, you must provide GitLab with
> the URL your users use to reach the repository."

[Configuration options for Linux package installations — docs.gitlab.com](https://docs.gitlab.com/omnibus/settings/configuration/)

**The official Docker docs' pattern.** The documented docker-compose example sets both the
container `hostname:` and the `external_url` (via `GITLAB_OMNIBUS_CONFIG`) to the *same*
single fixed name, then publishes matching ports to the host:

```yaml
services:
  gitlab:
    image: gitlab/gitlab-ee:<version>-ee.0
    hostname: 'gitlab.example.com'
    environment:
      GITLAB_OMNIBUS_CONFIG: |
        external_url 'https://gitlab.example.com'
    ports:
      - '80:80'
      - '443:443'
      - '22:22'
```

A second example in the same doc shows customizing the published HTTP/SSH ports while keeping
the same one-hostname pattern: `external_url 'http://gitlab.example.com:8929'` with
`gitlab_rails['gitlab_shell_ssh_port'] = 2424`, published as `8929:8929`, `2424:22`.
[Install GitLab in a Docker container — docs.gitlab.com](https://docs.gitlab.com/install/docker/installation/)

The companion configuration page reiterates the same idea outside Compose: pass
`--hostname gitlab.example.com` to `docker run` and "Set the `external_url` field to a valid
URL for your GitLab instance" — again, one hostname used both as the container's own hostname
and as `external_url`. [Configure GitLab running in a Docker container — docs.gitlab.com](https://docs.gitlab.com/install/docker/configuration/)

**`GITLAB_HOST`/`GITLAB_PORT`/`GITLAB_HTTPS` — explicitly checked, mixed/negative result.**
The official environment-variables reference documents `GITLAB_HOST` ("The full URL of the
GitLab server (including `http://` or `https://`)") and a separate `EXTERNAL_URL` variable
("Specify the external URL at the time of installation"). **`GITLAB_PORT` and `GITLAB_HTTPS`
do NOT appear anywhere on this page and were not found on any other `docs.gitlab.com` page
searched in this research** — do not assume they exist as supported Docker/omnibus
pre-configuration variables; the documented, current mechanism is `external_url` (via
`GITLAB_OMNIBUS_CONFIG` for Docker, or the bare `EXTERNAL_URL`/`GITLAB_HOST` vars for a
source/package install), not a host+port+scheme triad.
[Environment variables — docs.gitlab.com](https://docs.gitlab.com/administration/environment_variables/)

**Multiple/alternate hostnames — `allowed_hosts`.** GitLab documents an Omnibus setting to
allow (or, if unset, implicitly not restrict) which HTTP `Host:` headers the app will accept:

```ruby
gitlab_rails['allowed_hosts'] = ['gitlab.example.com', '127.0.0.1', 'localhost']
```

Crucially, the docs state: **"There are no known security issues in GitLab caused by not
configuring `allowed_hosts`, but it's recommended for defense in depth."** — i.e., by
default (unconfigured) GitLab does **not** reject requests based on Host header/hostname
mismatch. [Configuration options for Linux package installations — docs.gitlab.com](https://docs.gitlab.com/omnibus/settings/configuration/)

**The NGINX listen port is itself derived from `external_url`.** GitLab's NGINX settings docs
state directly: **"By default, NGINX listens on the port specified in `external_url` or uses
the standard port (80 for HTTP, 443 for HTTPS)."**
[NGINX settings — docs.gitlab.com](https://docs.gitlab.com/omnibus/settings/nginx/)
This is the fact that makes GitLab's own custom-port docker-compose example map the port
**identically on both sides** — `'8929:8929'` (host:container), not `8929:80` — because with
`external_url 'http://gitlab.example.com:8929'`, NGINX actually binds `8929` *inside* the
container, not `80`. [Install GitLab in a Docker container — docs.gitlab.com](https://docs.gitlab.com/install/docker/installation/)
A request to the container on port 80 would not be served if `external_url` specifies a
different port.

**What this means for the dual-reachability requirement (synthesis of the above primary
facts — the docs do not address the "compose network + host browser" scenario as a named
use case, and this paragraph is inference, flagged as such):**
- The primary-docs-supported pattern for dual reachability is: pick **one** fixed hostname
  (e.g. a compose service name/network alias, or a fixed local domain like `gitlab.local`),
  put that same hostname *and port* into `external_url` (e.g. `http://gitlab.local:8929`), set
  it as the container `hostname:`, and map that exact port 1:1 in `ports:` (`8929:8929`). With
  that single `host:port` string, both the host browser and a sibling container on the compose
  network can reach GitLab on the same NGINX listener — for the sibling container via the
  compose network's DNS (the service name resolves to the container's IP, and the container
  listens on `8929` because that's what `external_url` set), and for the host browser via the
  published port on `localhost`/`127.0.0.1` (or a host `/etc/hosts` entry mapping the same
  hostname to `127.0.0.1`, so the *same* URL string works from both places).
- The one piece the docs still leave undocumented is **client-side DNS resolution on the
  host** — making the *hostname itself* (as opposed to the port) resolve identically from the
  host browser and from sibling containers. `extra_hosts`, compose network aliasing, and a
  `/etc/hosts` entry for a fixed local domain like `gitlab.local` are standard Docker
  mechanisms for this, but **are not discussed anywhere on `docs.gitlab.com`** in the pages
  checked for this research — GitLab's docs only show the *server-side* half (`hostname:` +
  `external_url` + matching `ports:`). This is a real, confirmed gap in the primary docs, not
  an oversight in this research.
- Separately: `external_url`/`hostname:` mismatches do not, by themselves (absent
  `allowed_hosts`), cause GitLab to *reject* a request addressed with a different Host header
  — the default `allowed_hosts` behavior is unrestricted per the quote above. This means a
  request that reaches the right IP:port combination will be served regardless of what
  hostname string it was addressed with; it is *port* mismatch (per the NGINX finding above),
  not *hostname* mismatch, that is the actual failure mode to avoid.

---

## 3. Headless Personal Access Token (PAT) generation

GitLab **does** document a fully headless, no-UI way to create a PAT, under "Create a personal
access token programmatically" on the Personal access tokens page:

```
sudo gitlab-rails runner "token = User.find_by_username('automation-bot').personal_access_tokens.create(scopes: ['read_user', 'read_repository'], name: 'Automation token', expires_at: 365.days.from_now); token.set_token('token-string-here123'); token.save!"
```

[Personal access tokens — docs.gitlab.com](https://docs.gitlab.com/user/profile/personal_access_tokens/)

**Note for this use case:** the doc's example scopes (`read_user`, `read_repository`) are
read-only and would **not** be sufficient to call `POST /api/v4/projects` in item 4 below
(project creation requires write access to the API). The docs themselves say "The scopes must
be valid" (i.e. any valid scope name works) — for a bootstrap script that both authenticates
and creates a repo, substitute the full `api` scope, e.g.
`scopes: ['api']`, which is a documented valid PAT scope
([Personal access tokens — docs.gitlab.com](https://docs.gitlab.com/user/profile/personal_access_tokens/))
even though it is not the literal example GitLab shows on this page.

Documented caveats/requirements from that same page:
- **Token length:** "The token must be 20 characters long" — the predetermined
  `token-string-here123` string (or your own) must be exactly 20 characters.
- **Applicability:** the method "works for any user account on GitLab Self-Managed or GitLab
  Dedicated instances" — this explicitly includes `root`, since `root` is just another
  username to `User.find_by_username`.
- **Access requirement:** you need "sufficient access to run a Rails console session" — for
  Docker this means `docker exec` into the `gitlab` container and running
  `gitlab-rails runner "..."`, which is fully scriptable/headless (no browser, no interactive
  prompt beyond shell access to the container).

This is a **fully headless, officially documented** option — no gap to flag here. It is the
right mechanism for a bootstrap script to seed a token for `root` (or a dedicated
`automation-bot`/service user) without any manual "generate token" click in the UI.

---

## 4. REST API repository/project creation

Endpoint: `POST /projects` (i.e., `POST /api/v4/projects` against the instance base URL).

Minimal required fields: `name` is required **if** `path` is not provided, and vice versa —
i.e. at least one of `name` or `path` must be supplied; every other attribute is optional.

Auth header format: `PRIVATE-TOKEN: <your_access_token>` (the PAT from item 3, or any token
with API scope).

Example (from the docs):

```bash
curl --request POST --header "PRIVATE-TOKEN: <your-token>" \
     --header "Content-Type: application/json" --data '{
        "name": "new_project", "description": "New Project", "path": "new_project",
        "namespace_id": "42", "initialize_with_readme": "true"}' \
     --url "https://gitlab.example.com/api/v4/projects/"
```

A simpler form is also documented: `curl --request POST --header "PRIVATE-TOKEN: <your_access_token>" --url "https://gitlab.example.com/api/v4/projects?name=foo"`.

If `namespace_id` is omitted, the project defaults to the current (authenticated) user's
personal namespace.

Source: [Projects API — docs.gitlab.com](https://docs.gitlab.com/api/projects/) (create-project
section; content cross-verified identical against the archived version of the same page,
[Projects API (17.10 archive) — docs.gitlab.com](https://docs.gitlab.com/17.10/api/projects.html),
since the live page is large enough that a single fetch of it did not always surface the
"Create project" subsection).

---

## 5. Resource requirements, startup time, and readiness check

### Resource requirements

Two different documented tiers apply, and the gap between them matters for a lightweight demo
stand:

- **Production reference sizing (smallest tier GitLab documents):** the "Up to 20 RPS or 1,000
  users" single-node reference architecture calls for **8 vCPU, 16 GB memory**, and states
  "you should not go lower than the general requirements."
  [Reference architecture: Up to 20 RPS or 1,000 users — docs.gitlab.com](https://docs.gitlab.com/administration/reference_architectures/1k_users/)
  General installation requirements (linked from that page) list, for a single-node
  installation: **8 vCPU baseline** ("burstable instance types are not recommended"),
  **16 GB memory baseline**, **40 GB storage** for the application node (package install
  ~2.5 GB plus OS/logs/temp files), **5–12 GB** for the PostgreSQL database, plus enough
  repository storage to hold all repositories, with SSD recommended especially for Gitaly.
  [System requirements — docs.gitlab.com](https://docs.gitlab.com/install/requirements/)
- **Documented minimum/memory-constrained floor (more realistic for a demo stand):** GitLab's
  own "Running GitLab in a memory-constrained environment" page states the minimum expected
  specs are **4 CPU cores of ARM7/ARM64 or 1 CPU core of AMD64**, with **at least 2 GB RAM +
  1 GB swap** (2.5 GB RAM + 1 GB swap "optimally"), while also warning "you may experience
  unexpected degradation of both product functionality and performance" at that floor. The
  same page notes that a standard Omnibus install on a 64-bit machine "requires about ~3 GB of
  memory to run" without any `gitlab.rb` tuning.
  [Running GitLab in a memory-constrained environment — docs.gitlab.com](https://docs.gitlab.com/omnibus/settings/memory_constrained_envs/)

**Gap:** no primary-source page found in this research states an expected container
**startup time** (cold-start duration for `gitlab-ctl reconfigure` + services to come up) — this
was explicitly searched for and not found on the requirements, reference-architecture, or
Docker installation pages. Do not assume a number; budget generously (GitLab's own community
guidance elsewhere commonly cites several minutes, but that is not sourced to an official page
found here, so it is omitted as a claim).

### Readiness check for a bootstrap script

GitLab documents four distinct monitoring endpoints, with different semantics:

| Endpoint | Purpose | Response |
|---|---|---|
| `GET /-/health` | Basic app-server-is-running check, implemented as early middleware (bypasses Rails Controllers); does **not** verify DB/Redis | `GitLab OK` text, 200 |
| `GET /health_check` | Deep check of DB, Redis, and other backend services | `success` text, 200; explicit failure text otherwise |
| `GET /-/readiness` | Checks whether the instance is ready to accept traffic through Rails Controllers; optional `?all=1` also validates Database, Redis, Gitaly | JSON, 200 on success / 503 on failure |
| `GET /-/liveness` | Checks Rails Controllers aren't deadlocked | JSON `{"status": "ok"}`, 200 / 503 |

[Health Check — docs.gitlab.com](https://docs.gitlab.com/administration/monitoring/health_check)

The docs explicitly warn: **"Do not use `/health_check` for load balancing or autoscaling"**
(it can flap healthy nodes when backend services are merely slow) — so for a bootstrap
script's "is GitLab ready for API calls yet" poll, **`GET /-/readiness` (optionally with
`?all=1` to also confirm DB/Redis/Gitaly) is the endpoint the docs' own semantics point to**,
since it is specifically the one routed through Rails Controllers (the same code path the REST
API in item 4 uses) rather than the shallow middleware-only `/-/health` check.
[Health Check — docs.gitlab.com](https://docs.gitlab.com/administration/monitoring/health_check)

**Important caveat for a docker-compose bootstrap script — IP allowlist.** By default, only
local IPs may reach these monitoring endpoints: "by default only local IPs are allowed to
access monitoring resources," with the default allowlist being `127.0.0.0/8`. To poll
`/-/readiness` from a **different container** on the compose network (e.g., a bootstrap
container, or from TeamCity itself), you must add that network's subnet to the allowlist:

```ruby
gitlab_rails['monitoring_whitelist'] = ['127.0.0.0/8', '192.168.0.1']
```

(set via `GITLAB_OMNIBUS_CONFIG` and applied on `gitlab-ctl reconfigure`).
[IP allowlist — docs.gitlab.com](https://docs.gitlab.com/administration/monitoring/ip_allowlist/)

Without this, a bootstrap script running as a *separate* container polling GitLab's
`/-/readiness` over the docker-compose network will get blocked/rejected by the default
allowlist — either add the compose network's subnet to `monitoring_whitelist`, or poll from
inside the `gitlab` container itself (e.g. `docker exec gitlab curl -sf localhost/-/readiness`).

---

## Summary of what CANNOT be done fully headlessly / is not documented (explicit call-outs)

1. **Exact persistence semantics of `GITLAB_ROOT_PASSWORD` / `initial_root_password` across
   container restarts of an already-initialized data volume** are not spelled out by any
   primary-source page found (§1) — treat as "first bootstrap only" based on indirect
   evidence, not a directly quoted guarantee.
2. **Client-side DNS resolution for dual reachability** (making one `external_url` hostname
   resolve correctly both from the host browser and from sibling containers) — `extra_hosts`,
   compose network aliasing, and a fixed local domain like `gitlab.local` are **not addressed
   anywhere in the official GitLab docs** checked (§2). The docs only cover the server-side
   half (`hostname:` + `external_url` + published ports); the docker-compose networking
   pattern to achieve dual resolution is standard Docker knowledge, not GitLab-documented
   knowledge, and was not fabricated into this document.
3. **`GITLAB_PORT` and `GITLAB_HTTPS` env vars** do not appear to exist in current GitLab docs
   (§2) — only `GITLAB_HOST`/`EXTERNAL_URL`/`external_url` (via `GITLAB_OMNIBUS_CONFIG`) are
   documented.
4. **Startup time** for the `gitlab/gitlab-ce` container is not documented anywhere found in
   this research (§5).

Everything else requested is documented headlessly by GitLab's own primary sources, as cited
above: the initial root password specifically via `GITLAB_OMNIBUS_CONFIG` setting
`gitlab_rails['initial_root_password']` for Docker (bare `GITLAB_ROOT_PASSWORD` is only
docs-verified for an `apt install` package install, not confirmed for Docker — see §1), PAT
generation via `gitlab-rails runner`, REST API project creation, and a readiness endpoint to
poll.
