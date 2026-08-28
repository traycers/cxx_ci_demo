[🇬🇧 English](../../en/adr/0008-python-bootstrap-container.md) · [🇷🇺 Русский](../../ru/adr/0008-python-bootstrap-container.md) · 🇨🇳 中文

_翻译自 `docs/en/adr/0008-python-bootstrap-container.md`。原文变更时请同步更新本文件——参见 [ADR 0006](0006-trilingual-docs-mirror-tree.md)。_

# 把 bootstrap 重写为接入 `cxxci` 的 Python 容器——`bootstrap/` 改名为 `repos/`

`bootstrap.sh` 被替换成了 `scripts/bootstrap/`——一个打包成镜像、通过 `docker compose run --rm bootstrap` 运行的 Python provisioning 工具——它是 `docker-compose.yml` 里 `profiles: ["tools"]` 下的一次性服务，因此普通的 `docker compose up` 永远不会把它启动起来。这么做的原因不是宿主机环境的可移植性(那是另一个已经单独修复过的 bug——见 `tc_post` 改成 stdin 的那次重写)，而是为了简化：容器直接接入 `cxxci` 网络，于是每一次对 GitLab/TeamCity 的 REST 调用都变成了按 compose 服务名(`gitlab`/`teamcity-server`)发出的普通请求，取代了旧脚本里每次调用都要起一个「邻居」容器(`docker run --rm --network cxxci curlimages/curl ...`)的做法。顺带也彻底去掉了只在宿主机上有意义的 `gitlab.local` 主机名、绕过环境代理的 `NO_PROXY` 变通，以及和已发布宿主机端口上 squid 代理的冲突——这些东西在网络内部根本就够不着，也就无从谈起。

给 GitLab root 铸造 PAT 仍然需要 exec 进 gitlab 容器内部(`gitlab-rails runner`，不是 HTTP 调用)，读取 TeamCity 的 super-user token 仍然需要读那个容器的日志——这两件事光靠普通网络访问都做不到。为此，`bootstrap` 服务挂载了 `/var/run/docker.sock`——和本仓库里 `teamcity-agent` 已经在用的 Docker-outside-of-Docker 是同一种模式。Docker SDK for Python 通过这个 socket 直接和守护进程对话；镜像里既没有 `docker` CLI，也没有 compose 插件，查找邻居容器靠的是标准的 `com.docker.compose.service` 标签，而不是写死的名字——所以它不依赖 `COMPOSE_PROJECT_NAME`，也不依赖容器命名方式。

这次重写顺带换了语言：用 Python 取代 bash，用 `python-gitlab`、Docker SDK、`GitPython` 和 `requests`，取代直接调用 `subprocess`/`curl`——反正镜像本来就要构建，用真正的客户端库换来的可读性不需要额外成本。固定使用 Python 3.14.7(写这份文档时的当前稳定版)，采用和 `teamcity-server`/`teamcity-agent` 已经在用的同一套精确版本锁定方式，而不是 `:latest`。

种子内容是在构建镜像时通过 `COPY` 复制进去的，而不是 bind mount 进去的——这意味着新增一个待填充分支(ADR 0007)现在需要重新构建镜像，这是权衡之后的有意选择，为了让容器更简单、自成一体，考虑到这个工具真正的用途(把系统跑起来、演示 C++ 构建，而不是频繁迭代的开发循环)。

幂等性和 bash 版本保持 1:1 一致：每一次 REST 调用在动手之前仍然会先检查状态(VCS root 是否已存在？分支是否已经推送过？agent 是否已经被授权？)——「可以安全地重复运行」这个特性完全一样，只是用 `requests`/`GitPython` 取代了通过 subprocess 调用 `curl`/`git`。旧的 `bootstrap.sh` 被彻底删除，而不是作为后备方案留在旁边；如果需要对照，git 历史里还留着它。

与此同时，根目录下的 `bootstrap/`(种子内容，见 ADR 0007)被改名为 `repos/`——纯粹的改名，ADR 0007 确立的按分支划分结构、orphan 提交策略、推送顺序都没有变化。所有硬编码引用旧路径的地方(`scripts/new-release.sh`、`README.md`、`CONTEXT.md`)都已更新；ADR 0007 本身作为该决定的历史记录被保留，只加了一条简短的指向说明，而不是就地重写(和 ADR 0005 → 0006 用的是同一套惯例)。

结论：现在启动这套系统的最后一步是 `docker compose run --rm bootstrap`，而不是 `./bootstrap.sh`；任何原来写作 `bootstrap/ci-infra/...` 的路径，现在都写作 `repos/ci-infra/...`。
