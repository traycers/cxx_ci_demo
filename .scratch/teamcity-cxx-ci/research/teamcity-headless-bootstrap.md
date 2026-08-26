# TeamCity headless bootstrap (Docker + docker-compose) — research findings

Research date: 2026-08-26. "Current stable" = the tags JetBrains was actively publishing to Docker Hub as of this date: **`2026.1.3`** (server & agent), confirmed via the Docker Hub registry API (`hub.docker.com/v2/repositories/jetbrains/teamcity-server/tags` and `.../teamcity-agent/tags`, queried 2026-08-26 — `latest` resolves to `2026.1.3`, with `2025.11.7` as the previous LTS-ish line still receiving patches).

## TL;DR

- **EULA acceptance + first admin/super-user account creation is NOT scriptable via any documented env var or config file in the current images.** There is no `TEAMCITY_SERVER_FIRST_START` variable, no `-Dteamcity.server.firstStart...` flag, and no first-start `answers.properties`-style silent-install mechanism in the current TeamCity docs, the `jetbrains/teamcity-server` Docker Hub README, or the `JetBrains/teamcity-docker-images` startup scripts. This step **requires a human to click through the browser wizard once** (data directory confirmation → DB choice → accept license → create admin user). See §1.
- **The database-selection step of that wizard CAN be pre-empted** by dropping a pre-filled `database.properties` file (or `TEAMCITY_DB_URL` / `TEAMCITY_DB_USER` / `TEAMCITY_DB_PASSWORD` env vars) into the data directory before first boot — but the EULA-accept and admin-creation clicks still remain. See §1.
- **A REST-API-usable credential for automation *does* exist without ever logging into the UI**: TeamCity's built-in Super User mode generates a fresh token on every server start, prints it to `teamcity-server.log` (and stdout/`docker logs`), and that same token can be used as an HTTP Basic-Auth password (empty username) directly against the REST API — including to mint a normal personal access token for a real user once one exists. See §2.
- **Agent registration is fully headless** via `SERVER_URL`/`AGENT_TOKEN`/`AGENT_NAME` env vars on the `jetbrains/teamcity-agent` image. **Agent *authorization* is documented as a manual UI step** ("Agents are manually authorized via the web UI on the Agents page") — **but** that too can be done headlessly via a documented REST call (`PUT /app/rest/agents/<id>/authorizedInfo`) using the Super User token from the point above, so it does not have to stay manual in practice. See §3.
- **Docker socket access for agent-run `docker build`/`docker run`** is supported by mounting `/var/run/docker.sock` into the agent container, but JetBrains' own documented example for that pattern runs the container as **root (`-u 0`)**, sidestepping GID-matching entirely; the image also pre-creates a `docker` group at a fixed GID (999) as a fallback alignment mechanism. See §3.

### What genuinely cannot be automated headless (no documented env-var/API equivalent found)

1. **First-start EULA acceptance and initial super-user/admin account creation.** No primary source (current docs, Docker Hub READMEs, or the `teamcity-docker-images` GitHub scripts) documents an environment variable, internal property, or config file that performs these two actions. A human must open the browser wizard once and click "Proceed"/"Accept License Agreement"/"Create Account". Source: absence across [Quick Setup Guide](https://www.jetbrains.com/help/teamcity/quick-setup-guide.html), [Start TeamCity Server](https://www.jetbrains.com/help/teamcity/start-teamcity-server.html), [Server Startup Properties](https://www.jetbrains.com/help/teamcity/server-startup-properties.html), the [`jetbrains/teamcity-server` Docker Hub README](https://github.com/JetBrains/teamcity-docker-images/blob/master/dockerhub/teamcity-server/README.md), and `context/run-server.sh` / `context/run-server-services.sh` in [JetBrains/teamcity-docker-images](https://github.com/JetBrains/teamcity-docker-images) (which only start Tomcat — no first-start flags are read).
2. **Nothing else** turned out to be a hard blocker: agent authorization, despite being documented as a UI action, has a documented REST API escape hatch (see above).

---

## 1. EULA acceptance & first admin account: env vars / files, or manual?

### What the first-start wizard actually requires

The [Quick Setup Guide](https://www.jetbrains.com/help/teamcity/quick-setup-guide.html) documents four mandatory steps the first time you open the server in a browser:

1. Confirm/review the **Data Directory** location.
2. Choose the **database** — "the wizard doesn't permit skipping this choice" even if you keep the default internal database (source: Quick Setup Guide).
3. **Accept the License Agreement** — the guide says you must "accept the License Agreement to proceed with the launch" (source: Quick Setup Guide).
4. **Create the administrator account** — "specify the administrator credentials and click Create Account" (source: Quick Setup Guide).

No JetBrains documentation page describes a way to script steps 2–4. Specifically checked and found nothing on:

- [Start TeamCity Server](https://www.jetbrains.com/help/teamcity/start-teamcity-server.html) — documents `run`/`start`/`stop` script commands and systemd/launchd wiring only; explicitly defers "special properties" to the Server Startup Properties page.
- [Server Startup Properties](https://www.jetbrains.com/help/teamcity/server-startup-properties.html) — documents `internal.properties` (edited at `<TeamCity Data Directory>/config/internal.properties`, or via Administration UI) and JVM-level `TEAMCITY_SERVER_OPTS` / `TEAMCITY_SERVER_MEM_OPTS`. Nothing here relates to license acceptance or account creation; the page even warns "Please do not change internal properties unless asked by the TeamCity support team."
- The image's own boot scripts — `context/run-server.sh` in [JetBrains/teamcity-docker-images](https://raw.githubusercontent.com/JetBrains/teamcity-docker-images/master/context/run-server.sh) does nothing but set `TEAMCITY_SERVER_XML`/`TEAMCITY_CONTEXT`/HTTPS-proxy handling and exec `bin/teamcity-server.sh run`. `context/run-server-services.sh` (the actual `CMD` of the image, see the [Server Dockerfile](https://raw.githubusercontent.com/JetBrains/teamcity-docker-images/master/context/generated/linux/Server/Ubuntu/24.04/Dockerfile)) only runs any scripts dropped in `/services/*.sh` before calling `run-server.sh` — this `/services` extension point is documented for arbitrary custom setup ("place scripts in `/services`, they run before the server starts") but JetBrains does not ship any script there that performs first-start automation; you'd have to reverse-engineer the wizard's own HTTP endpoints yourself, which is unsupported and not documented anywhere primary.
- The [`jetbrains/teamcity-server` Docker Hub README](https://github.com/JetBrains/teamcity-docker-images/blob/master/dockerhub/teamcity-server/README.md) — documents `TEAMCITY_SERVER_MEM_OPTS`, `TEAMCITY_HTTPS_PROXY_ENABLED`, `TEAMCITY_CONTEXT`, data/log volume mounts, and the `maintainDB.sh` backup script. No first-start / EULA / admin-account variable exists.
- No `TEAMCITY_SERVER_FIRST_START` variable and no `-Dteamcity.server.firstStart...` system property could be found in any of the above, nor in web search results turned up from JetBrains-owned domains.

**Conclusion for §1:** EULA acceptance and initial admin-account creation are a real, unavoidable one-time manual browser step in the current stable image line. Document this precisely as a manual checklist item.

### What CAN be pre-seeded (the DB step)

The database-choice sub-step (but not EULA/account creation) can be pre-configured by placing a filled-in `database.properties` file at `<TeamCity Data Directory>/config/database.properties` before the very first boot, using the provided `database.<type>.properties.dist` templates as a base — or by setting **`TEAMCITY_DB_URL`, `TEAMCITY_DB_USER`, `TEAMCITY_DB_PASSWORD`** environment variables (documented specifically as an alternative to putting the DB password in the properties file). Source: [Set up an External Database](https://www.jetbrains.com/help/teamcity/set-up-external-database.html). For this demo stand (Professional/free tier, no external DB required) you can simply leave the default internal (embedded) database — you'll still have to click past that step once in the wizard.

### `internal.properties` / super-user toggles found

The only documented internal property directly touching Super User behavior is **`teamcity.superUser.disable=true`**, which *disables* Super User login — the opposite of what we want, but confirms `internal.properties` at `<TeamCity Data Directory>/config/internal.properties` is the right file for internal-property overrides in general. Source: [Super User Access](https://www.jetbrains.com/help/teamcity/super-user.html). No internal property was found that pre-accepts the EULA or pre-creates an admin user.

---

## 2. Getting a REST API token headlessly (no manual UI login)

### The Super User authentication token

- A **new Super User authentication token is generated every time the server starts** (older docs say daily; current docs say "each time the server starts" — treat as regenerated per restart) and is written to **both the server console/stdout and `teamcity-server.log`**, with the log line containing the string `Super User authentication token`. Source: [Super User Access](https://www.jetbrains.com/help/teamcity/super-user.html).
- In Docker: `docker logs <server-container> 2>&1 | grep -F " Super user authentication token: "` (or read `/opt/teamcity/logs/teamcity-server.log` from the mounted logs volume) reliably extracts it without ever opening a browser.
- **This token works directly as HTTP Basic Auth credentials against the REST API**: TeamCity's own [REST API Quick Start](https://www.jetbrains.com/help/teamcity/rest/quick-start.html) documents "just provide no username and the super user password" as a valid REST authentication method, alongside the recommended personal-access-token Bearer scheme. This confirms Super User auth is a first-class, documented REST credential — **not** something requiring a prior UI session.
- Practical implication: you do not strictly need to mint a "real" personal access token at all — you can drive the whole REST API (create projects, push Kotlin DSL / versioned settings, authorize agents, etc.) using Basic Auth with the Super User token for as long as the demo stand needs it. If you do want a scoped, non-super-user token (e.g., for a service account used by CI automation), you can create one via the REST API's user/token management endpoints once at least one real user exists, again authenticating the request itself as Super User.

### CSRF caveat for POST/PUT/DELETE

Since CSRF protection was tightened, **modifying requests (POST/PUT/DELETE) using session-cookie or Basic-Auth style authentication require a CSRF token** unless you use Bearer-token auth with no session cookies present. To get one: `GET <server>/authenticationTest.html?csrf`, then pass it back as header `X-TC-CSRF-Token` or parameter `tc-csrf-token`. JetBrains explicitly recommends Bearer-token auth for non-browser clients specifically to avoid this dance. Source: [CSRF Protection](https://www.jetbrains.com/help/teamcity/csrf-protection.html). Practical takeaway: for scripted automation, prefer minting a personal access token (Bearer auth) over long-lived Super User Basic Auth wherever request volume/complexity makes the CSRF-token fetch annoying — but Super User Basic Auth remains valid and does not itself require a UI login.

### Important interaction with §1

The Super User token is generated by the running server process; it does **not** bypass the first-start wizard. Until the wizard's DB/EULA/admin steps are completed, the server is still serving the wizard UI rather than a fully initialized REST API, so the practical bootstrap order is: (a) complete the one-time manual wizard click-through (§1), (b) then grab the Super User token from logs and drive everything else via REST API headlessly (§2). No primary source documents REST API availability *before* first-start completion, and the server's own startup scripts show no path that skips the wizard, so treat the wizard as a hard prerequisite gate.

---

## 3. Agent registration, docker.sock, and gotchas

### Headless registration via env vars

`jetbrains/teamcity-agent`'s `context/run-agent.sh` (in [JetBrains/teamcity-docker-images](https://raw.githubusercontent.com/JetBrains/teamcity-docker-images/master/context/run-agent.sh)) reads these and runs `agent.sh configure` with them on first boot (when no existing `buildAgent.properties` is found in the mounted config volume):

- `SERVER_URL` — required; URL of the TeamCity server the agent connects to.
- `AGENT_NAME` — optional; auto-generated if omitted.
- `AGENT_TOKEN` — optional; passed as `--auth-token`.
- `OWN_ADDRESS`, `OWN_PORT` — optional, Linux-only, bind address/port.
- `AGENT_OPTS` — optional, arbitrary extra lines appended verbatim to `buildAgent.properties`.
- `DOCKER_IN_DOCKER=start` (Linux only) — starts an internal Docker daemon inside the agent container (see below); requires the `-linux-sudo` tagged image variant and `--privileged`.

All of the above is also documented in the [`jetbrains/teamcity-agent` Docker Hub README](https://github.com/JetBrains/teamcity-docker-images/blob/master/dockerhub/teamcity-agent/README.md), which matches the script exactly.

### Agent authorization: documented as manual, but has a REST escape hatch

- [Build Agent](https://www.jetbrains.com/help/teamcity/build-agent.html) states plainly: **"Agents are manually authorized via the web UI on the Agents page."** New agents start Unauthorized and can't run builds until authorized; the one documented exception is an agent running on the same host as the server, which auto-authorizes.
- However, this manual step **is scriptable**: [Manage Agents (REST API)](https://www.jetbrains.com/help/teamcity/rest/manage-agents.html) documents `PUT /app/rest/agents/<agentLocator>/authorizedInfo` with body `{"status": true, "comment": {"text": "..."}}` (or the XML equivalent) to flip an agent's authorized flag — usable headlessly with the Super User token / a Bearer access token from §2 immediately after the agent registers. So in an automated compose stack you'd poll `GET /app/rest/agents?locator=authorized:false` after bringing the agent up and PUT `authorizedInfo` for each one, rather than clicking "Authorize" in a browser.
- The `AGENT_TOKEN` docker env var (`--auth-token` on `agent.sh configure`) is a re-registration/identity token used to reconnect an *already-authorized* agent under the same identity after a config-volume wipe or migration, per the Docker Hub README wording ("if unset, the agent should be authorized via TeamCity UI") — it is not itself a pre-authorization mechanism for a brand-new agent; use the REST call above for that.

### docker.sock mounting for `docker build`/`docker run` in build steps

Two documented options, both from the [`jetbrains/teamcity-agent` Docker Hub README](https://github.com/JetBrains/teamcity-docker-images/blob/master/dockerhub/teamcity-agent/README.md):

**Option A — mount the host's docker.sock ("docker-out-of-docker"), the one you asked about:**

```
docker run -e SERVER_URL="<url>" \
    -u 0 \
    -v <agent-config-dir>:/data/teamcity_agent/conf \
    -v /var/run/docker.sock:/var/run/docker.sock \
    -v /opt/buildagent/work:/opt/buildagent/work \
    -v /opt/buildagent/temp:/opt/buildagent/temp \
    -v /opt/buildagent/tools:/opt/buildagent/tools \
    -v /opt/buildagent/plugins:/opt/buildagent/plugins \
    -v /opt/buildagent/system:/opt/buildagent/system \
    jetbrains/teamcity-agent
```

Notes straight from the primary source:
- JetBrains' own example runs the container **as root (`-u 0`)**, which is how they sidestep any docker.sock group/GID mismatch — root can always access the socket regardless of its group ownership on the host.
- The `/opt/buildagent/*` volume mounts are required specifically to make TeamCity's [Docker Wrapper](https://www.jetbrains.com/help/teamcity/docker-wrapper.html) feature (used by Command Line/Maven/Ant/Gradle/.NET/PowerShell runners) work, because that feature bind-mounts those same host paths into any nested "wrapped" container it starts — and the README explicitly warns you **cannot run multiple such agents from one host** without giving each one its own distinct `/opt/buildagentN` path baked into a custom image, because the path used inside the wrapped/nested container is taken from the *host* path, not the container path.
- JetBrains' own security note: **both docker.sock mounting and DinD "require extra trust... as a build may get root access to the host"** (verbatim from the README, linking to the [OWASP Docker Security Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Docker_Security_Cheat_Sheet.html) and TeamCity's own [security notes](https://www.jetbrains.com/help/teamcity/security-notes.html)). For a demo stand this is an acceptable, explicit tradeoff — just don't run untrusted build configs against it.

**Docker CLI / GID pre-alignment already baked into the image**, confirmed by reading the generated Dockerfiles in [JetBrains/teamcity-docker-images](https://github.com/JetBrains/teamcity-docker-images):
- The **full `jetbrains/teamcity-agent`** image (not the minimal one) installs `docker-ce` + `docker-ce-cli` + `containerd.io` via apt and runs `usermod -aG docker buildagent` — Docker CLI is present out of the box. Source: [`context/generated/linux/Agent/Ubuntu/24.04/Dockerfile`](https://raw.githubusercontent.com/JetBrains/teamcity-docker-images/master/context/generated/linux/Agent/Ubuntu/24.04/Dockerfile).
- The **`jetbrains/teamcity-minimal-agent`** base image (which `teamcity-agent` builds on) explicitly pre-creates a `docker` group at a **fixed GID 999**, with the comment "`TW-98512: Ensures alignment of the 'docker' user group across releases`" — i.e., JetBrains anticipated the host/container docker-GID mismatch problem and pins it. Source: [`context/generated/linux/MinimalAgent/Ubuntu/24.04/Dockerfile`](https://raw.githubusercontent.com/JetBrains/teamcity-docker-images/master/context/generated/linux/MinimalAgent/Ubuntu/24.04/Dockerfile).
- **Gotcha:** if your host's `docker` group GID is *not* 999 (very common — Ubuntu hosts often assign the `docker` group a different GID depending on install order), and you run the agent as the non-root `buildagent` user instead of `-u 0`, the mounted socket's group ownership won't match the in-container `docker` group and `docker` CLI calls inside the agent will fail with a permission error. JetBrains' documented example avoids this entirely by using `-u 0` (root) instead of relying on GID alignment. If you need non-root, you'd have to either `chown`/`chmod` the socket on the host or recreate the `docker` group inside a custom image at the host's actual GID — none of that is documented by JetBrains; it's an operational detail you own.

**Option B — Docker-in-Docker (isolated daemon inside the agent container):**

```
docker run -e SERVER_URL="<url>" \
    -u 0 \
    -v <agent-config-dir>:/data/teamcity_agent/conf \
    -v docker_volumes:/var/lib/docker \
    --privileged -e DOCKER_IN_DOCKER=start \
    jetbrains/teamcity-agent:2026.1.3-linux-sudo
```

This requires the `-linux-sudo` tag suffix (a separate image variant with `sudo`/systemd wired for this) and `--privileged`. `context/run-docker.sh` (in `/services/run-docker.sh` inside that image variant, [source](https://raw.githubusercontent.com/JetBrains/teamcity-docker-images/master/context/run-docker.sh)) starts an internal `dockerd` via `service docker start` when `DOCKER_IN_DOCKER=start` is set, including legacy-iptables handling via `DOCKER_IPTABLES_LEGACY=1`. This does **not** touch the host's docker.sock at all — separate daemon, separate image cache, no host build-cache sharing.

For a demo CI stand where you specifically want the agent to `docker build`/`docker run` against the *host's* Docker (shared cache, simplicity), **Option A (mount `/var/run/docker.sock`, run as `-u 0`) is what JetBrains itself documents and is the simpler choice.**

---

## 4. Summary: what's manual vs. automatable

| Step | Automatable headless? | Mechanism | Source |
|---|---|---|---|
| Accept EULA | **No** | none documented | Quick Setup Guide, Server Startup Properties, teamcity-docker-images scripts (absence) |
| Create first admin/super-user account | **No** | none documented | same as above |
| Choose/pre-seed database | Yes (for external DB) / N/A (default internal DB still needs one wizard click) | `database.properties` file or `TEAMCITY_DB_URL`/`TEAMCITY_DB_USER`/`TEAMCITY_DB_PASSWORD` | [Set up an External Database](https://www.jetbrains.com/help/teamcity/set-up-external-database.html) |
| Obtain a REST-usable credential | **Yes** | Super User token from `teamcity-server.log` / `docker logs`, used as Basic Auth (no username) | [Super User Access](https://www.jetbrains.com/help/teamcity/super-user.html), [REST API Quick Start](https://www.jetbrains.com/help/teamcity/rest/quick-start.html) |
| Register agent with server | **Yes** | `SERVER_URL`, `AGENT_NAME`, `AGENT_TOKEN` env vars | [teamcity-agent Docker Hub README](https://github.com/JetBrains/teamcity-docker-images/blob/master/dockerhub/teamcity-agent/README.md), `run-agent.sh` |
| Authorize a new agent | **Yes** (docs call it manual, but a REST call exists) | `PUT /app/rest/agents/<id>/authorizedInfo` | [Manage Agents REST API](https://www.jetbrains.com/help/teamcity/rest/manage-agents.html); manual-by-default claim from [Build Agent](https://www.jetbrains.com/help/teamcity/build-agent.html) |
| Give agent Docker access | **Yes** | mount `/var/run/docker.sock`, run agent as `-u 0` | [teamcity-agent Docker Hub README](https://github.com/JetBrains/teamcity-docker-images/blob/master/dockerhub/teamcity-agent/README.md) |
| Create projects / push Kotlin DSL settings | **Yes** | REST API with Super User token or a minted personal access token | [REST API Quick Start](https://www.jetbrains.com/help/teamcity/rest/quick-start.html), [CSRF Protection](https://www.jetbrains.com/help/teamcity/csrf-protection.html) |

**The only irreducible manual step is the first-start browser wizard** (data-dir confirm → DB choice click → accept EULA → create admin account), a few clicks, once, per fresh data directory. Everything downstream of that is scriptable.

---

## docker-compose shape (minimum recommended)

Based strictly on the volumes/ports/env vars documented in the sources above:

```yaml
services:
  teamcity-server:
    image: jetbrains/teamcity-server:2026.1.3
    # Volumes: data dir (required — settings/build results) and logs dir (recommended —
    # otherwise logs are lost on container removal, incl. the Super User token history).
    # Source: jetbrains/teamcity-server Docker Hub README.
    volumes:
      - teamcity_server_datadir:/data/teamcity_server/datadir
      - teamcity_server_logs:/opt/teamcity/logs
      # optional: override Tomcat conf
      # - ./tomcat-conf:/opt/teamcity/conf
    ports:
      - "8111:8111"   # default HTTP port; EXPOSE 8111 per server Dockerfile
    environment:
      # optional, tune JVM heap; default in image is "-Xmx2g -XX:ReservedCodeCacheSize=640m"
      - TEAMCITY_SERVER_MEM_OPTS=-Xmx2g -XX:ReservedCodeCacheSize=640m
      # only if behind an HTTPS-terminating reverse proxy:
      # - TEAMCITY_HTTPS_PROXY_ENABLED=true
      # only to pre-seed an external DB and skip that wizard step:
      # - TEAMCITY_DB_URL=...
      # - TEAMCITY_DB_USER=...
      # - TEAMCITY_DB_PASSWORD=...

  teamcity-agent:
    image: jetbrains/teamcity-agent:2026.1.3
    privileged: false      # not needed for docker.sock approach (Option A); true only for DinD (Option B)
    user: "0"               # root — required for the mounted docker.sock to be usable without GID juggling
    environment:
      - SERVER_URL=http://teamcity-server:8111
      # - AGENT_NAME=agent1
      # - AGENT_TOKEN=...     # only for re-registering a previously authorized agent identity
    volumes:
      - teamcity_agent_conf:/data/teamcity_agent/conf
      - /var/run/docker.sock:/var/run/docker.sock
      # required specifically if you want TeamCity's "Docker Wrapper" build-step feature to work;
      # must use the SAME host-side paths under /opt/buildagent/ for every agent instance on one host
      # if you rely on Docker Wrapper (see §3 gotcha on multiple agents + docker.sock).
      - /opt/buildagent/work:/opt/buildagent/work
      - /opt/buildagent/temp:/opt/buildagent/temp
      - /opt/buildagent/tools:/opt/buildagent/tools
      - /opt/buildagent/plugins:/opt/buildagent/plugins
      - /opt/buildagent/system:/opt/buildagent/system
    depends_on:
      - teamcity-server

volumes:
  teamcity_server_datadir:
  teamcity_server_logs:
  teamcity_agent_conf:
```

Citations for each element: data/log volumes and port 8111 — [`jetbrains/teamcity-server` README](https://github.com/JetBrains/teamcity-docker-images/blob/master/dockerhub/teamcity-server/README.md); `TEAMCITY_SERVER_MEM_OPTS` default value — [server Dockerfile](https://raw.githubusercontent.com/JetBrains/teamcity-docker-images/master/context/generated/linux/Server/Ubuntu/24.04/Dockerfile) (`ENV TEAMCITY_SERVER_MEM_OPTS="-Xmx2g -XX:ReservedCodeCacheSize=640m"`); `TEAMCITY_HTTPS_PROXY_ENABLED` — same server README; `TEAMCITY_DB_*` vars — [Set up an External Database](https://www.jetbrains.com/help/teamcity/set-up-external-database.html); `SERVER_URL`/`AGENT_NAME`/`AGENT_TOKEN` and docker.sock mount + `/opt/buildagent/*` volumes + `-u 0` — [`jetbrains/teamcity-agent` README](https://github.com/JetBrains/teamcity-docker-images/blob/master/dockerhub/teamcity-agent/README.md); agent config volume path `/data/teamcity_agent/conf` — same README and confirmed in [`run-agent.sh`](https://raw.githubusercontent.com/JetBrains/teamcity-docker-images/master/context/run-agent.sh).

After `docker compose up`, the remaining sequence is:

1. (Manual, one-time) Open `http://localhost:8111`, click through data-dir/DB confirmation, accept the EULA, create the admin account. — Unavoidable per §1.
2. (Scripted) `docker compose logs teamcity-server | grep -F "Super user authentication token:"` to grab the token. — §2.
3. (Scripted) Use that token as REST Basic Auth to: authorize the new agent (`PUT /app/rest/agents/<id>/authorizedInfo`), create projects, push Kotlin DSL settings, mint a longer-lived personal access token for ongoing automation, etc. — §2, §3.

---

## Sources (primary)

- [Quick Setup Guide](https://www.jetbrains.com/help/teamcity/quick-setup-guide.html)
- [Start TeamCity Server](https://www.jetbrains.com/help/teamcity/start-teamcity-server.html)
- [Server Startup Properties](https://www.jetbrains.com/help/teamcity/server-startup-properties.html)
- [Install and Start TeamCity Server](https://www.jetbrains.com/help/teamcity/install-and-start-teamcity-server.html)
- [Super User Access](https://www.jetbrains.com/help/teamcity/super-user.html)
- [REST API Quick Start](https://www.jetbrains.com/help/teamcity/rest/quick-start.html)
- [CSRF Protection](https://www.jetbrains.com/help/teamcity/csrf-protection.html)
- [TeamCity REST API (overview)](https://www.jetbrains.com/help/teamcity/teamcity-rest-api.html)
- [Manage Agents (REST API)](https://www.jetbrains.com/help/teamcity/rest/manage-agents.html)
- [Build Agent](https://www.jetbrains.com/help/teamcity/build-agent.html)
- [Docker Wrapper](https://www.jetbrains.com/help/teamcity/docker-wrapper.html)
- [Set up an External Database](https://www.jetbrains.com/help/teamcity/set-up-external-database.html)
- [Security Notes](https://www.jetbrains.com/help/teamcity/security-notes.html)
- [JetBrains/teamcity-docker-images (repo root, current maintained repo)](https://github.com/JetBrains/teamcity-docker-images)
  - [`README.md`](https://raw.githubusercontent.com/JetBrains/teamcity-docker-images/master/README.md)
  - [`dockerhub/teamcity-server/README.md`](https://raw.githubusercontent.com/JetBrains/teamcity-docker-images/master/dockerhub/teamcity-server/README.md)
  - [`dockerhub/teamcity-agent/README.md`](https://raw.githubusercontent.com/JetBrains/teamcity-docker-images/master/dockerhub/teamcity-agent/README.md)
  - [`context/run-server.sh`](https://raw.githubusercontent.com/JetBrains/teamcity-docker-images/master/context/run-server.sh)
  - [`context/run-server-services.sh`](https://raw.githubusercontent.com/JetBrains/teamcity-docker-images/master/context/run-server-services.sh)
  - [`context/run-agent.sh`](https://raw.githubusercontent.com/JetBrains/teamcity-docker-images/master/context/run-agent.sh)
  - [`context/run-agent-services.sh`](https://raw.githubusercontent.com/JetBrains/teamcity-docker-images/master/context/run-agent-services.sh)
  - [`context/run-docker.sh`](https://raw.githubusercontent.com/JetBrains/teamcity-docker-images/master/context/run-docker.sh)
  - [`context/check-server-volumes.sh`](https://raw.githubusercontent.com/JetBrains/teamcity-docker-images/master/context/check-server-volumes.sh)
  - [`context/generated/linux/Server/Ubuntu/24.04/Dockerfile`](https://raw.githubusercontent.com/JetBrains/teamcity-docker-images/master/context/generated/linux/Server/Ubuntu/24.04/Dockerfile)
  - [`context/generated/linux/Agent/Ubuntu/24.04/Dockerfile`](https://raw.githubusercontent.com/JetBrains/teamcity-docker-images/master/context/generated/linux/Agent/Ubuntu/24.04/Dockerfile)
  - [`context/generated/linux/MinimalAgent/Ubuntu/24.04/Dockerfile`](https://raw.githubusercontent.com/JetBrains/teamcity-docker-images/master/context/generated/linux/MinimalAgent/Ubuntu/24.04/Dockerfile)
- [JetBrains/teamcity-docker-server (deprecated, superseded by teamcity-docker-images)](https://github.com/JetBrains/teamcity-docker-server) — used only to confirm the `/services` extension mechanism's history and the deprecation notice pointing to `teamcity-docker-images`.
- Docker Hub registry API, `jetbrains/teamcity-server` and `jetbrains/teamcity-agent` tag lists (`hub.docker.com/v2/repositories/jetbrains/teamcity-server/tags`, `.../teamcity-agent/tags`), queried 2026-08-26 — used to confirm current stable tag `2026.1.3`.
- [`jetbrains/teamcity-server` Docker Hub page](https://hub.docker.com/r/jetbrains/teamcity-server)
