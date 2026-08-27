[🇬🇧 English](../../en/adr/0004-kotlin-dsl-versioned-settings-import-mode.md) · [🇷🇺 Русский](../../ru/adr/0004-kotlin-dsl-versioned-settings-import-mode.md) · 🇨🇳 中文

_翻译自 `docs/en/adr/0004-kotlin-dsl-versioned-settings-import-mode.md`。原文变更时请同步更新本文件——参见 [ADR 0006](0006-trilingual-docs-mirror-tree.md)。_

# Kotlin DSL versioned settings,import 模式——git/UI 是权威来源

ADR 0003 曾认为 Kotlin DSL 在这里永久不可行，原因是 `teamcity-server` 没有出站互联网访问。这个结论是错的，或者至少是不完整的：针对当前的 `CxxCiDemo_Main` 树重新测试后发现，DSL 编译可以完全离线成功。TeamCity 生成的 `.teamcity/pom.xml` 声明了两个 Maven 仓库——`https://download.jetbrains.com/teamcity-repository`(和之前一样不可达)以及 `http://localhost:8111/app/dsl-plugins-repository`，也就是服务器自己的本地镜像。第二个仓库是从已安装的插件生成的，完全不需要接触网络(已确认：服务器上的 `system/caches/pluginsDslCache/.m2`，约 376 个 jar 文件，包括 `configs-dsl-kotlin-bundled` 和 Kotlin 编译器本身)，而且它恰好包含了编译器需要的一切——Maven 会从它这里解析，完全不需要用到第一个仓库。最初那次尝试真正的阻塞原因并没有被确认(那次会话的错误日志没有被重新诊断)，但在这个过程中确认了一个具体的前提条件：Kotlin 格式要求每个 object 的 id(包括 VCS root)都必须以其父项目的 id 作为前缀——裸 id 在编译尝试之前就会被直接拒绝。

我们把 `_Root` 的 versioned settings 从 XML/`alwaysUseCurrent`(服务器是权威来源，git 是单向的审计镜像——ADR 0003 的安排)切换到了 Kotlin/`useFromVCS`(git 是权威来源；`allowUIEditing: true` 意味着通过 UI/REST 的修改仍然像以前一样可以正常工作，只是现在它们会自动提交回 `ci-infra`，而不再只是本地状态)。已经在真实环境中双向验证过：一次手动编辑并推送到 `ci-infra` 的提交，改变了一个真实的 TeamCity 参数；一次通过 REST 完成的、类似 UI 操作的修改，产生了一个真实的 `ci-infra` 提交。

这本身并不能让 git 成为一条完全独立的复现路径——这正是 ADR 0003 后续工作指出的那个缺口。VCS root 的密码仍然不能作为字面量密钥出现在 git 中。我们用一个项目参数(`gitlab_credentials_password`,password 类型)来解决这个问题，DSL 中的 VCS root 通过符号引用(`%gitlab_credentials_password%`)指向它，用于文档记录，但参数的真实值从不会被提交——`storeSecureValuesOutsideVcs: true` 让它始终只保留在服务器本地。这里有个细节：TeamCity 会在 DSL 应用时，把这个 `%param%` 引用解析成 VCS root 上一个*静态的*、特定安装专属的 `credentialsJSON:<uuid>`；后续同步不会重新解析这个引用。所以 `bootstrap.sh` 在每次运行时都会通过 REST 直接把真实的 GitLab token 设置到每一个需要它的 VCS root 上——而不是依赖这个参数引用自行传播。这是一个正常的、幂等的步骤，而不是权宜之计；TeamCity 自己会把这些 DSL 之外的密钥修改表示为自动提交到 `ci-infra` 的 `.teamcity/patches/` 文件(仅作参考，从不会被自动应用回主脚本)，这正是特定安装专属密钥应有的形态，因此被保持原样，而不是被「修正」到主 `settings.kts` 里。

结论：`bootstrap.sh` 在 TeamCity 中的职责，收缩到了 git/DSL 确实做不到的那部分——创建 versioned settings 拉取 `ci-infra` 所需要的那一个 VCS root、把 `_Root` 以 import 模式指向它，并注入那一个凭据。其余一切(项目树、`base_build` 模板、build type、依赖、触发器、`Result` build type)现在都归属于 `bootstrap/ci-infra/.teamcity/settings.kts` 和 TeamCity 的 UI——两条路径现在都落进同一份 git 历史里。`docs/build.sh` 作为独立文件已被删除；构建步骤脚本的内容现在只存在于 `settings.kts` 中。
