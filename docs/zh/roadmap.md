[🇬🇧 English](../en/roadmap.md) · [🇷🇺 Русский](../ru/roadmap.md) · 🇨🇳 中文

_翻译自 `docs/en/roadmap.md`。原文变更时请同步更新本文件——参见 [ADR 0006](adr/0006-trilingual-docs-mirror-tree.md)。_

# Roadmap

本页描述的是 CI 未来可能的发展方向,尚不是已确定的架构。这里的任何内容都不应被视为已敲定;一旦其中某一点真正做出带有实际取舍的决定,就会为它单独写一份 ADR,本页也会更新指向那份 ADR。

## C++ 包管理器

今天的依赖配置依赖构建镜像内的系统包管理器(`apt`——见 [`tradeoff.md`](tradeoff.md) 缺点 2),该缺点里提到的环境分歧也正是由此而来:CI 通过 `apt` 安装某个库,而开发者本地可能通过 Conan 安装同一个库——两边的环境在这一步就已经分道扬镳。

目前有三个候选者可以替代或补充这一点:**Conan**、**vcpkg** 和 **Nix**。三者都还没有被选定。Nix 目前是个人偏好,但这只是一种倾向,不是决定——真正的评估之后画面还可能改变。只有在完成这样的 research 之后,这才会成为一份 ADR。

## Package variant——`release` 与 `debug`

今天 CI 已经用 `CMAKE_BUILD_TYPE=RelWithDebInfo` 构建每一个 demo project(见每个 track 模板里的 `BaseBuild.kt`)——但只是为了跑测试,并不是作为可复用的产物。计划是把它变成真正可复用的 **package variant**,并在旁边加上第二个:

- **`release`**——即今天的 `RelWithDebInfo`,保留下来是为了与当前的构建/测试流程保持向后兼容。保留最近 **5** 次构建。
- **`debug`**——新的 `CMAKE_BUILD_TYPE=Debug` 配置,专门为供开发者消费而构建。只保留**最近一次**构建——需要更早 debug 构建的人得自己重新构建,而不是让 CI 保存一份除了最新消费者之外没人需要的深层历史。

叫 `release` 而不是 `optimized`,是因为分支族的术语现在是 `Track` 而不是 `Release`(见 `CONTEXT.md`)。某个具体的 track 仍然可以命名为 `release_1`/`release_2` 等(见 [ADR 0012](adr/0012-release-instance-names-restored.md)),并不会与这个 package variant 冲突:两者从不占据路径中同一个位置(`track_name/repo_name/variant`),区分它们的是位置,而不是词本身。

**已为 track `main` 实现**——见 [ADR 0013](adr/0013-debug-release-subprojects-and-track-scoped-image-naming.md):每个 variant 都是一个完整的子 TeamCity subproject(`Main_Debug`/`Main_Release`),而不是单个 build type 上的参数。`main` 的 `debug` `Result` 会把它的 artifact 依赖所解压出的内容原样发布为可下载压缩包——见下面的 Phase 1。`release_1`/`release_2`/`release_3` 尚未推行(特意推迟到另一张独立的未来地图——不急,因为各个 track 相互独立)。

## Dev container image

一个 Docker 镜像,在 TeamCity 上以根镜像构建的镜像为 `FROM` 基础构建(见 `CONTEXT.md`——这样对 track 的根镜像所做的改动就不会丢失,而不需要从头重新推导),推送到 Docker registry,并被每个 demo project 的 `devcontainer.json` 直接引用。同事无需自己构建镜像就能拿到可用的 dev container——只要在 `devcontainer.json` 里指向它即可。

具体用哪个 registry(GitLab 内置的 Container Registry,因为 GitLab 本来就是该环境的一部分,还是来自 Docker Hub 的普通 `registry:2` 镜像)暂时留白——这是实现层面的决定,不是愿景层面的决定。

这是本环境中第一个计划经由 registry 分发的产物。[ADR 0002](adr/0002-no-registry-shared-docker-daemon.md) 刻意为根镜像构建跳过了 registry,因为所有构建都共用一个 agent 上的同一个 Docker daemon。这个理由在这里不成立:dev container image 需要送达开发者的机器,而不只是同一个 agent 上的兄弟构建,所以共享 daemon 无法像对根镜像构建那样替代 registry。这并不推翻 ADR 0002——根镜像构建仍然不经过 registry——这只是它的推理从一开始就没有覆盖到的第一个场景。

**已为 track `main` 实现,且不需要 registry**——`Main_BuildDevImage` 以根镜像为 `FROM` 基础构建 `cxx_ci_demo/main/Dockerfile.dev`,并将其打上 `cxxci-main-dev:latest` 标签;`project_a`/`project_c`/`project_d`/`project_e` 的 `devcontainer.json` 直接引用宿主机共享 Docker daemon 上的这个标签。对这个 demo 环境来说,开发者和 TeamCity agent 共用同一个 daemon 就已经够用了——上面"需要送达开发者机器"的推理,假设的是开发者的机器和 agent 用的是*不同*的 Docker daemon,而对这个单主机的 demo 环境来说并非如此。真正的 registry 仍然是上面记录在案的可选项,留给这个假设不再成立的那一天;这并不是 `main` 已实现部分里的缺口。`release_1`/`release_2`/`release_3` 尚未推行。

## 使用 dev container 的两个阶段

### Phase 1——不使用包管理器

现在就可以实现,与上面包管理器的决定无关。开发者创建一个任务目录(见 [`developer-flow.md`](developer-flow.md)),脚本构建整条仓库链,把每一个仓库 `cmake install` 到任务目录根部的一个目录里——这实际上就是今天 TeamCity 自身构建方式的本地镜像。

为了加速这一过程,TeamCity 会把 `debug` 变体发布为可下载的压缩包——思路和今天的 **Artifact 依赖** 机制(见 `CONTEXT.md`)一样,只是携带的是 `debug` 二进制文件,而不是作为项目自身构建的副产物。开发者下载并解压到任务目录里,然后只构建自己真正需要改动的那个仓库。

关键在于,这保留了日常开发中最重要的灵活性:开发者仍然可以走进链上的任何其他仓库,构建它并在本地 `install`——从而拿到某个依赖上尚未完成的改动,而不仅仅是自己一开始动手的那个仓库。

对 track `main` 来说,上面提到的 dev container 和可下载的 `debug` 压缩包现在都已经是真实存在的了——每个仓库 `.devcontainer/devcontainer.json` 里的 `CMAKE_PREFIX_PATH` 已经指向 checkout 上一级的一个目录,随时准备接收解压后的压缩包。任务目录的克隆/切换脚本和构建脚本本身仍未实现(见 `developer-flow.md`、`tradeoff.md` 缺点 4)——目前下载和解压压缩包还是手动步骤。

### Phase 2——使用包管理器

在上面的包管理器 research 落地为 ADR 之后才会到来。开发者只把目标仓库克隆到任务目录里。在 dev container 内,只构建这一个仓库;它的 `debug` 依赖来自包管理器,而不是被克隆并在本地构建。

这会失去 Phase 1 拥有的东西:当一次改动同时涉及两个仓库时,目前还没有明显的办法用本地构建的版本去覆盖包管理器提供的依赖。这是一个尚未解决的开放问题,而不是已经敲定的细节——必须先在开发者的机器上把它解决,Phase 2 才能真正取代 Phase 1 的灵活性。

Phase 2 到底值不值得做,本身也是一个开放问题,只有等 Phase 1 上线并投入使用之后才值得重新审视——迁移到包管理器并不是板上钉钉的事。

## 另请参阅

- [`tradeoff.md`](tradeoff.md) 缺点 2——本 roadmap 要解决的环境分歧问题。
- [`developer-flow.md`](developer-flow.md)——本 roadmap 中构建脚本要扩展的"一个任务一个目录"流程。
