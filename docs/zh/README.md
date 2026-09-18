# cxx_ci_demo

[🇬🇧 English](../../README.md) · [🇷🇺 Русский](../ru/README.md) · 🇨🇳 中文


基于 docker-compose 的 CI 演示环境：GitLab + TeamCity 在容器中构建 C++ 项目。术语表见 `CONTEXT.md`，架构决策见 `docs/zh/adr/`。完整计划保存在 wayfinder 地图 `.scratch/teamcity-cxx-ci/map.md` 中。文档同时维护英语、俄语和中文版本(`docs/ru/`、`docs/zh/`)——约定说明见 [ADR 0006](adr/0006-trilingual-docs-mirror-tree.md)。

## 启动环境

1. 执行 `cp .env.example .env`，并填写 `GITLAB_ROOT_PASSWORD`(如果之前已经配置过，本地可能已经存在带有生成密码的 `.env`——覆盖前请先检查)。
2. 执行 `docker compose up -d`
3. **唯一无法避免的手动步骤**：打开 `http://localhost:${TEAMCITY_HTTP_PORT:-8111}`，完整走一遍 TeamCity 首次启动向导(确认数据目录、接受 EULA、创建管理员账号)。当前镜像没有无头(headless)等效方案——参见 `.scratch/teamcity-cxx-ci/research/teamcity-headless-bootstrap.md` 第 1 节。
4. GitLab 可通过 `http://localhost:${GITLAB_HTTP_PORT:-8929}` 访问，账号为 `root`，密码见 `.env`——不需要在 `/etc/hosts` 里加任何记录:GitLab 默认不会因为 Host 头不匹配而拒绝请求,所以直接用 `localhost` 上发布的端口就能用。(GitLab 自己 UI 里显示的 clone 链接用的是 compose 服务名 `gitlab`——因为这是相邻容器需要的;只有当你从 UI 里复制 clone 链接、而不是使用 TeamCity 自己那些已经直接指向 `gitlab` 的 VCS root 时,这一点才有意义。)
5. `docker compose run --build --rm bootstrap`——创建 6 个 GitLab 仓库(`ci-infra` 以及五个 `project_*`)，把 `repos/<repo>/<branch>/` 下的种子内容推送进去，并把 TeamCity 的 versioned settings 指向 `ci-infra`。以一次性容器的形式直接连接到 `cxxci` 网络运行(见 ADR 0008)，而不是宿主机脚本，所以这里的一切都不依赖宿主机上 `curl`/`git`/`docker` 的版本。可以安全地重复运行。**务必带上 `--build`**:`repos/` 种子内容是在镜像构建期被打包进去的(`scripts/bootstrap/Dockerfile`),不带 `--build` 的 `docker compose run` 会在镜像已存在时静默复用旧镜像——容器随后会推送过时的内容,而由于 `push_repo_content()` 会跳过 GitLab 上已存在的分支(ADR 0007),之后单纯重新运行也无法修复。如果已经发生这种情况,可以先临时取消受影响分支的保护、force-push 修正后的内容,再重新运行 bootstrap 让它重新注入凭据(DSL 重新导入会清空重建后的 VCS root 上的凭据)来恢复这套环境。

## 故障排查

- **`docker compose up` 在挂载 `/opt/buildagent/*` 时失败**(permission denied)：docker 守护进程需要能够在宿主机上创建/拥有 `/opt/buildagent`。这个路径不可配置——它被写死在 `jetbrains/teamcity-agent` 镜像本身里，所以宿主机一侧也必须是这个确切路径(原因见 `docker-compose.yml` 中 `teamcity-agent` 上的注释)。在 `/opt` 下创建/拥有目录需要 root 权限，这在 rootless Docker 或没有 root 权限的宿主机账号下都不成立。如果遇到这种情况，请以拥有 `sudo` 权限的人的身份，在宿主机上手动执行一次(不作为 `docker compose up`/`bootstrap` 的一部分)：`sudo mkdir -p /opt && sudo ln -s /path/you/own /opt/buildagent`，然后重新运行 `docker compose up`。此后真实数据存放在你自己拥有的目录里，而 Docker 和 agent 仍然通过这个符号链接在 `/opt/buildagent` 看到它。
- **不支持通过 `snap` 安装的 Docker。** 症状：即使宿主机用户已经拥有该路径的所有权(包括上面的符号链接方案)，`teamcity-agent` 容器仍然无法写入 `/opt/buildagent`——snap 版 `docker` 的 confinement 机制会以只读方式挂载文件系统，从而破坏这个 bind mount。解决方法：卸载 snap 包，改为从官方 APT/YUM 仓库安装 Docker，然后重新运行 `docker compose up`。
