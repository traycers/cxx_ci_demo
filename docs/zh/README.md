# cxx_ci_demo

[🇬🇧 English](../../README.md) · [🇷🇺 Русский](../ru/README.md) · 🇨🇳 中文

_翻译自 `README.md`。原文变更时请同步更新本文件——参见 [ADR 0006](adr/0006-trilingual-docs-mirror-tree.md)。_


基于 docker-compose 的 CI 演示环境：GitLab + TeamCity 在容器中构建 C++ 项目。术语表见 `CONTEXT.md`，架构决策见 `docs/zh/adr/`。完整计划保存在 wayfinder 地图 `.scratch/teamcity-cxx-ci/map.md` 中。文档同时维护英语、俄语和中文版本(`docs/ru/`、`docs/zh/`)——约定说明见 [ADR 0006](adr/0006-trilingual-docs-mirror-tree.md)。

## 启动环境

1. 执行 `cp .env.example .env`，并填写 `GITLAB_ROOT_PASSWORD`(如果之前已经配置过，本地可能已经存在带有生成密码的 `.env`——覆盖前请先检查)。
2. 执行 `docker compose up -d`
3. **唯一无法避免的手动步骤**：打开 `http://localhost:${TEAMCITY_HTTP_PORT:-8111}`，完整走一遍 TeamCity 首次启动向导(确认数据目录、接受 EULA、创建管理员账号)。当前镜像没有无头(headless)等效方案——参见 `.scratch/teamcity-cxx-ci/research/teamcity-headless-bootstrap.md` 第 1 节。
4. GitLab 可通过 `http://localhost:${GITLAB_HTTP_PORT:-8929}` 访问，账号为 `root`，密码见 `.env`——不需要在 `/etc/hosts` 里加任何记录:GitLab 默认不会因为 Host 头不匹配而拒绝请求,所以直接用 `localhost` 上发布的端口就能用。(GitLab 自己 UI 里显示的 clone 链接用的是 compose 服务名 `gitlab`——因为这是相邻容器需要的;只有当你从 UI 里复制 clone 链接、而不是使用 TeamCity 自己那些已经直接指向 `gitlab` 的 VCS root 时,这一点才有意义。)
5. `docker compose run --rm bootstrap`——创建 6 个 GitLab 仓库(`ci-infra` 以及五个 `project_*`)，把 `repos/<repo>/<branch>/` 下的种子内容推送进去，并把 TeamCity 的 versioned settings 指向 `ci-infra`。以一次性容器的形式直接连接到 `cxxci` 网络运行(见 ADR 0008)，而不是宿主机脚本，所以这里的一切都不依赖宿主机上 `curl`/`git`/`docker` 的版本。可以安全地重复运行。

## 故障排查

- **`docker compose up` 在挂载 `/opt/buildagent/*`时失败**(permission denied)：该路径要求 docker 守护进程能够在 `/opt` 下创建/拥有目录——对普通的 rootful Docker 安装成立，但对 rootless Docker 或没有 root 权限的宿主机账号不成立。请在 `.env` 中将 `BUILDAGENT_DATA_DIR` 设置为你实际拥有权限的目录(例如 `BUILDAGENT_DATA_DIR=${HOME}/.local/share/cxxci-buildagent`)，然后重新运行。这些路径必须是宿主机 bind mount，而不能是命名卷(named volume)——原因见 `docker-compose.yml` 中 `teamcity-agent` 上的注释。
- **VCS root 的 "test connection"/构建失败，报 `HTTP Basic: Access denied` 或 `Authentication failed`**：这是凭据问题，不是网络/DNS 问题，尽管乍看很像。如果具体命中的是某个 `project_*` 自己的 VCS root，这是 `provision_teamcity()`(`scripts/bootstrap/teamcity_ops.py`)里一个已知的竞态：build type 出现在 REST API 里并不代表已导入的项目已经能接受写入——DSL 导入完成后，项目可能会在相当长的一段时间(超过一分钟)里保持 "read only, project settings format switched to Kotlin" 状态，而注入凭据恰好是一次写入。现在 bootstrap 会把整批凭据注入请求作为一个整体，针对共享的 5 分钟总期限反复重试，而不是只发一次就相信结果——这样通常无需任何人工步骤即可自愈。如果仍然失败——bootstrap 发出的每个 REST 调用都会检查响应状态，并带着真实的 HTTP 状态码明确报错(针对这个具体的竞态，`provision_teamcity()` 现在会返回 `False`，容器以非零状态码退出，而不是谎报 `"done."`)——请重新运行 `docker compose run --rm bootstrap`；如果超过 5 分钟总期限仍然失败，说明问题已经不是这个竞态本身，而是别的原因(例如 `settings.kts` 本身有错误)。
- **`bootstrap` 容器无法访问 `gitlab`**(connection refused/timeout，而不是认证错误)——检查容器是否真的都在 `cxxci` 网络里(`docker compose ps`)。与上面浏览器那一步不同，`bootstrap` 容器是直接通过 compose 服务名(`gitlab`/`teamcity-server`)在 `cxxci` 网络内部访问的——它完全不经过已发布的宿主机端口或宿主机代理，所以影响浏览器/宿主机 `git` 的那些宿主机网络问题(hairpin NAT、拦截 `localhost` 的本地代理)对它都不适用。见 ADR 0008。
