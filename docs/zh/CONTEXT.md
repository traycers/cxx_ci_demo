[🇬🇧 English](../../CONTEXT.md) · [🇷🇺 Русский](../ru/CONTEXT.md) · 🇨🇳 中文

_翻译自 `CONTEXT.md`。原文变更时请同步更新本文件——参见 [ADR 0006](adr/0006-trilingual-docs-mirror-tree.md)。_

# CI CXX

基于 docker-compose 的 CI 演示环境：GitLab(VCS)+ TeamCity(构建服务器，单 agent)在 Docker 容器内构建 C++ 项目，整个构建树以一次根 Docker 镜像构建作为基础依赖。

## 术语

**ci_cxx**:
宿主机一侧的仓库，存放 docker-compose 以及用于搭建该环境(GitLab + TeamCity)的 bootstrap 脚手架。本身不包含任何 C++ 代码，也不是 TeamCity 的 CI 配置。
_避免使用_:project、monorepo

**ci-infra**:
GitLab 内部的中心仓库，存放整个构建树的 TeamCity Kotlin DSL(versioned settings)——包括根镜像构建自身的 build configuration——以及该镜像的 Dockerfile。
_避免使用_:settings repo、teamcity repo

**根镜像构建(Root image build)**:
用于构建 C++ 构建镜像的 TeamCity build configuration(基于 `ci-infra` 中的 Dockerfile)，是整个构建树最根部的依赖；重新构建它会触发依赖它的一切重新构建。
_避免使用_:base build、image job

**镜像标签(Image tag)**:
TeamCity 的配置参数(`%build_image_cxx%`)，保存下游 C++ 项目构建用于运行其构建容器的 Docker 镜像标签。

**Bootstrap**:
一次性的自动化脚本(以及 `bootstrap/` 目录，每个仓库对应一个子目录)，在 `docker compose up` 之后通过 API 在 GitLab 中创建仓库，并用初始内容(DSL、demo 项目)填充它们。

**Demo 项目**:
一个最小化的骨架 C++ 项目(CMake)，为本地图(map)端到端流水线验证而创建。两个 demo 项目中的一个依赖另一个，用于验证基于分支的依赖解析。

**Snapshot 依赖**:
TeamCity 的机制，保证被依赖的构建会从与触发构建相同的分支被触发和获取；如果该分支在依赖项的 VCS root 中不存在，则回退到默认分支。
_避免使用_:build trigger dependency

**Artifact 依赖**:
TeamCity 的机制，将一个 C++ 项目已构建好的二进制文件/头文件传递给另一个项目用于链接，而无需从头重新构建。

**Release**(分支族):
`ci-infra` 中的一个 `cxx_ci_demo/<config_name>/` 子树——拥有自己的 TeamCity 子项目、自己的 VCS root、自己的一套 build configuration，但共享 GitLab 上同样的 demo 项目仓库(`demo-project-a`/`demo-project-b`)。各个 release 之间的区别纯粹在于每个 VCS root 监视哪个分支(`branch_default`/`branch_spec`)。参见 `docs/zh/adding-a-release.md`。
_避免使用_:build configuration(过于含糊——会与某个 release 内部单个项目自己的 build configuration 混淆)

**config_name**:
release 的名称，既作为其目录名(`cxx_ci_demo/<config_name>/`)，也作为 demo 项目中的基础分支名(`refs/heads/<config_name>`)。从该 release 派生出的分支命名为 `<config_name>-*`(例如 release `release_2_0` → 分支 `release_2_0`、`release_2_0-hotfix-1`)。
