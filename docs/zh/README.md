# cxx_ci_demo

[🇬🇧 English](../../README.md) · [🇷🇺 Русский](../ru/README.md) · 🇨🇳 中文

_翻译自 `README.md`。原文变更时请同步更新本文件——参见 [ADR 0006](adr/0006-trilingual-docs-mirror-tree.md)。_


基于 docker-compose 的 CI 演示环境：GitLab + TeamCity 在容器中构建 C++ 项目。术语表见 `CONTEXT.md`，架构决策见 `docs/zh/adr/`。完整计划保存在 wayfinder 地图 `.scratch/teamcity-cxx-ci/map.md` 中。文档同时维护英语、俄语和中文版本(`docs/ru/`、`docs/zh/`)——约定说明见 [ADR 0006](adr/0006-trilingual-docs-mirror-tree.md)。

## 启动环境

1. 执行 `cp .env.example .env`，并填写 `GITLAB_ROOT_PASSWORD`(如果之前已经配置过，本地可能已经存在带有生成密码的 `.env`——覆盖前请先检查)。
2. 在宿主机的 `/etc/hosts` 中添加 `127.0.0.1 gitlab.local`(或 `GITLAB_HOSTNAME` 中设置的任何主机名)。这是 GitLab 官方文档没有覆盖的唯一一处：compose 网络免费为相邻容器提供 DNS 解析，但宿主机操作系统需要这条记录，才能以 TeamCity 的 VCS root 和 clone 链接将来会用到的方式解析同一个主机名。参见 `.scratch/teamcity-cxx-ci/research/gitlab-headless-bootstrap.md` 第 2 节。
3. 执行 `docker compose up -d`
4. **唯一无法避免的手动步骤**：打开 `http://localhost:${TEAMCITY_HTTP_PORT:-8111}`，完整走一遍 TeamCity 首次启动向导(确认数据目录、接受 EULA、创建管理员账号)。当前镜像没有无头(headless)等效方案——参见 `.scratch/teamcity-cxx-ci/research/teamcity-headless-bootstrap.md` 第 1 节。
5. 获取 TeamCity 的 Super User token，用于脚本化访问：
   `docker compose logs teamcity-server | grep "Super user authentication token:"`
6. GitLab 可通过 `http://gitlab.local:${GITLAB_HTTP_PORT:-8929}` 访问，账号为 `root`，密码见 `.env`。

此后的一切(创建仓库、Kotlin DSL、demo 项目)均由 `bootstrap.sh` 自动完成——参见地图上的 08 号任务。

## 故障排查

- **`docker compose up` 在挂载 `/opt/buildagent/*`时失败**(permission denied)：该路径要求 docker 守护进程能够在 `/opt` 下创建/拥有目录——对普通的 rootful Docker 安装成立，但对 rootless Docker 或没有 root 权限的宿主机账号不成立。请在 `.env` 中将 `BUILDAGENT_DATA_DIR` 设置为你实际拥有权限的目录(例如 `BUILDAGENT_DATA_DIR=${HOME}/.local/share/cxxci-buildagent`)，然后重新运行。这些路径必须是宿主机 bind mount，而不能是命名卷(named volume)——原因见 `docker-compose.yml` 中 `teamcity-agent` 上的注释。
- **VCS root 的 "test connection"/构建失败，报 `HTTP Basic: Access denied` 或 `Authentication failed`**：这是凭据问题，不是网络/DNS 问题，尽管乍看很像——`git` 确实连上了 `gitlab.local` 并从 GitLab 得到了真实响应，只是不接受这个密码。如果具体命中的是 `demo-project-a`/`demo-project-b` 自己的 VCS root，通常说明 `bootstrap.sh` 没有执行到凭据注入那一步(`provision_teamcity` 的第 4 步)——该步骤只有在 `CxxCiDemo_Main_DemoProjectA` 已存在，也就是 versioned settings 成功导入了 DSL 树之后才会运行。请重新运行 `bootstrap.sh`；它发出的每个 REST 调用现在都会检查响应状态，失败时会带着真实的 HTTP 状态码和响应体明确报错，而不是悄悄继续下去(早期版本没有这样做，在一台全新机器上的运行就恰好展示了这一点：versioned settings 悄悄未能启用，于是那棵树——以及凭据注入——从未发生过，而随后出现的令人困惑的 "Access denied" 其实是那个延迟出现的真实症状)。
- **`gitlab.local` 在容器内部确实无法访问**(connection refused/timeout，而不是认证错误)是另一个问题：检查容器是否真的都在 `cxxci` 网络里(`docker compose ps`)，以及宿主机上是否有东西在拦截已发布端口上的流量(本仓库在开发过程中就遇到过本地代理这样做——参见 `bootstrap.sh` 中关于为什么它自己的 REST 调用要经过 `cxxci` 网络里的一个一次性容器，而不是走已发布的宿主机端口的注释)。

## 添加新的 release

TeamCity 中的 `cxx_ci_demo` 项目按 release 拆分为 `bootstrap/ci-infra/main/.teamcity/cxx_ci_demo/` 下的多个目录(目前只有一个名为 `main` 的 release——不要与外层的 `bootstrap/ci-infra/main/` 混淆，后者是预先准备好的 git 分支，见 ADR 0007)。分步操作流程以及 `<config_name>`/`<config_name>-*` 分支命名约定见 `docs/zh/adding-a-release.md`。
