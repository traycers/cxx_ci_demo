[🇬🇧 English](../../en/adr/0001-centralized-ci-config-repo.md) · [🇷🇺 Русский](../../ru/adr/0001-centralized-ci-config-repo.md) · 🇨🇳 中文

_翻译自 `docs/en/adr/0001-centralized-ci-config-repo.md`。原文变更时请同步更新本文件——参见 [ADR 0006](0006-trilingual-docs-mirror-tree.md)。_

# 集中式 CI 配置仓库(ci-infra)，而非每个项目各自的 .teamcity

TeamCity 的惯用模式是「每个项目自己的 versioned settings」(项目自己仓库内的 `.teamcity` 文件夹)。我们没有这样做，而是把全部 Kotlin DSL——根镜像构建、每个 C++ 项目的 build configuration，以及它们之间 snapshot/artifact 依赖的连线——都放进 GitLab 中一个集中的 `ci-infra` 仓库，与各个 C++ 项目仓库分开。

我们这样选择，是因为 C++ 项目仓库是各自独立创建的(通常还更晚，由其他开发者创建)，不应该背负 CI 相关的配置；docker 镜像构建和跨项目依赖拓扑本质上是基础设施层面的关注点，而不是单个项目的关注点；而且——这是有意为之——这样可以阻止开发者从自己项目的仓库里修改 CI 配置。

结论：向构建树中添加一个新的 C++ 项目，总是需要在 `ci-infra` 中做改动，而不仅仅是在新项目自己的仓库里。这是有意的设计，不是疏漏。
