[🇬🇧 English](../../en/adr/0003-rest-provisioning-instead-of-kotlin-dsl.md) · [🇷🇺 Русский](../../ru/adr/0003-rest-provisioning-instead-of-kotlin-dsl.md) · 🇨🇳 中文

_翻译自 `docs/en/adr/0003-rest-provisioning-instead-of-kotlin-dsl.md`。原文变更时请同步更新本文件——参见 [ADR 0006](0006-trilingual-docs-mirror-tree.md)。_

**已被 [ADR 0004](0004-kotlin-dsl-versioned-settings-import-mode.md) 取代**：下文「永久不可行」的结论是错误的——后来针对真实的树重新测试时发现，Kotlin DSL 可以通过服务器自己本地的 `dsl-plugins-repository` 完全离线编译通过。保留本文档用于记录历史；本 ADR 描述的机制已不再是 `bootstrap.sh` 实际采用的方式。

# TeamCity 项目树通过 REST 而非 Kotlin DSL versioned settings 来部署

在制图(charting)阶段，Q7 决定 TeamCity 配置采用 Kotlin DSL versioned settings，理由是 config-as-code 能带来可复现性。但实践中，`teamcity-server` 在 demo 主机上没有出站互联网访问——已通过实测确认(`docker compose exec teamcity-server curl https://1.1.1.1` 超时；Maven 本身也因为同样的原因无法自动下载依赖)。Kotlin DSL 编译至少需要 `kotlin-stdlib` 这样的外部构件，而服务器自己本地的 `dsl-plugins-repository` 并不提供这些构件，所以无论宿主机的网络限制如何，DSL 编译在这个环境里永远都不可能成功——这不是一个可以重试几次就过去的临时故障。

我们放弃了 Kotlin DSL，改为让 `bootstrap.sh` 直接通过 TeamCity 的 REST API 创建整棵项目树(VCS root、build type、步骤、参数、snapshot/artifact 依赖、触发器)。这保留了这项工作真正在意的那个特性——CI 配置是集中脚本化、可从 `docker compose up` + `bootstrap.sh` 复现的，而不是靠手工点击拼出来的(ADR 0001 原样保留、不受影响)——同时放弃了「配置以 `.kt` 文件形式存在于 git 中」这个具体机制，而这个机制在这个网络限制下本来就永远无法实现。

结论：项目树的*复现机制*是 `bootstrap.sh` 的 REST 调用，而不是 versioned settings——`docker compose down -v` 之后接着 `docker compose up` + `bootstrap.sh`，就能从零完整复现一个可用的环境，不依赖任何存储在 git 中的配置。

后续已完成的工作：在 Root 项目上以 *export* 模式(`ALWAYS_USE_CURRENT`)启用了 XML 格式的 versioned settings(不需要 Maven/Kotlin 编译)，得到的 `.teamcity/` XML 树被镜像进 `bootstrap/ci-infra/`，以便为通过 REST 部署的配置保留一份可读的 git 历史。这是单向的(UI/REST → git)，并且**不**携带任何密钥——VCS root 的密码是以 `credentialsJSON:<uuid>` 这样的引用形式导出的，由服务器在内部解析，单靠 git 本身无法迁移使用——所以这还不能让 git 导入成为一条独立的复现路径；`bootstrap.sh` 的 REST 部署仍然是一台全新机器真正依赖的机制。要切换到 *import* 模式、让全新的服务器可以直接从这份 XML 自行启动，还需要额外解决导入时的凭据提供问题——本次没有尝试这一点。
