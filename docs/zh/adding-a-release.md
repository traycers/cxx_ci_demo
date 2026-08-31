[🇬🇧 English](../en/adding-a-release.md) · [🇷🇺 Русский](../ru/adding-a-release.md) · 🇨🇳 中文

_翻译自 `docs/en/adding-a-release.md`。原文变更时请同步更新本文件——参见 [ADR 0006](adr/0006-trilingual-docs-mirror-tree.md)。_

# 添加新的 release

**Release**(见 `CONTEXT.md`)是 `ci-infra` 中的一个 `cxx_ci_demo/<config_name>/` 目录——拥有自己的 TeamCity 子项目、自己的 VCS root、自己的一套 build configuration、自己的 `Dockerfile`，但使用与其他每个 release *相同* 的 GitLab 仓库(从 `project_a` 到 `project_e`)。各 release 的区别在于每个 VCS root 监视哪个分支，以及它自己的 docker 镜像标签前缀(见下文)——GitLab 一侧不会复制或 fork 任何东西。现在实际存在哪些 release,见 `releases.md`。

## 快捷方式：`scripts/new-release.sh`

```
scripts/new-release.sh <new_config_name> [source_config_name]
```

`source_config_name` 默认为 `main`；传入某个已存在 release 的 config name，即可以它为基础分支，而不是以 `main` 为基础。该脚本会机械地完成下文「步骤」一节中的全部内容——复制目录、重命名每一个需要重命名的标识符和字符串，并把结果注册进 `cxx_ci_demo/CxxCiDemo.kt`——然后打印出仍需你手动完成的部分(push、运行 `bootstrap.sh`、创建实际的 git 分支、如果该 release 需要不同的构建环境则检查新的 `Dockerfile`)。它之所以存在，是因为手动重命名确实很容易出错：把这个流程写下来并手动走一遍之后，我们就已经踩中了下文描述的那个「前缀重复」错误，而且只是在推送到真实服务器、读到编译结果之后才发现的。

已完整走通一次真实验证：运行脚本从 `main` 生成了 `release_2_0` release，推送后编译干净通过，得到的 build type(`CxxCiDemo_Release20_BuildCImage` 等)完全正确，镜像也构建成功(`cxxci-build:release_2_0-1`，与 `main` 的标签正确区分)，之后回退撤销——本文档描述的是脚本实际做了什么，而不是一个理论上的流程。

本文档剩余部分解释脚本自动化了什么，供你需要手动完成其中一部分，或者想理解具体改动了什么时参考。

## 分支命名约定

`config_name` 既是 release 的目录名，也是 demo 项目中的基础分支名：

- release 自己的分支：`refs/heads/<config_name>`(例如 `refs/heads/release_2_0`)——`branch_default` 指向的就是它。
- 该 release 的任意派生/feature 分支：`<config_name>-*`(例如 `release_2_0-hotfix-1`、`release_2_0-new-cmake-flag`)——这是 `branch_spec` 中的第二个匹配项。

所以对于名为 `release_2_0` 的 release，两个构建约定参数是这样的：

```kotlin
param("branch_spec", """
    +:refs/heads/(release_2_0)
    +:refs/heads/(release_2_0-*)
""".trimIndent())
param("branch_default", "refs/heads/release_2_0")
```

不匹配任一模式的分支根本不会被该 release 的 VCS root 拾取——这正是防止多个 release 在同一批共享的 demo 项目仓库中互相踩踏对方分支的机制。

## Docker 镜像标签约定

每个 release 都会把自己的根 C++ 镜像构建进*同一个*共享 docker 守护进程(ADR 0002——不使用 registry)。单独的 `%build.number%` 在各 release 之间并不唯一，因此 `BuildCImage` 会把镜像标记为 `cxxci-build:<config_name>-%build.number%`(例如 `cxxci-build:release_2_0-14`)，该 release 中每一个下游 build type 都构造相同的带前缀标签来消费它(`build_image_cxx` 参数)。这也是为什么 `Dockerfile` 位于 release 自己的目录*内部*(`cxx_ci_demo/<config_name>/Dockerfile`)而不是仓库根目录：每个 release 都可以让它产生分歧——不同的基础镜像、不同的工具链——就像它其他一切一样。

没有 registry 也就意味着没有自动垃圾回收——`BuildCImage` 的「清理旧镜像」步骤会为自己 release 的前缀(`cxxci-build:<config_name>-*`)保留 `keep_images_count` 项目参数指定数量的最新标签，并在每次成功构建镜像后删除其余的。它只作用于自己 release 的前缀，永远不会碰到另一个 release 的镜像；这个参数会随着 `main/` 的 `params { }` 块一起自动带过来，不需要在那里做任何重命名。

## 脚本做了什么(手动流程)

1. **复制目录。** `cxx_ci_demo/main/` → `cxx_ci_demo/<config_name>/`(例如 `cxx_ci_demo/release_2_0/`)，包含其中的一切——这样也就顺带带上了 `Dockerfile`，不需要单独处理。

2. **把项目文件重命名为一个唯一的基础文件名。** 项目文件(`main/` 中的 `Main.kt`)包含一个顶层的 `val ...Id = ...`，而不只是一个 `object`。Kotlin 会把顶层的 val/函数包装进一个以**文件名**(而不是目录名)命名的合成类——不同目录下两个都叫 `Main.kt` 的文件会编译失败，报 `Duplicate JVM class name`。把它重命名为该 release 单词的 PascalCase 形式，例如 `release_2_0` 对应 `Release20.kt`(snake_case → PascalCase：把每个用 `_` 分隔的片段首字母大写，再无分隔符地拼接起来——这正是 `scripts/new-release.sh` 里 `to_pascal_case` 所做的；手动重命名时请保持与它一致)。只包含 `object` 声明的文件(树中其他所有文件)没有这个问题——它们编译后的类名取自 object 的名字，本身已经唯一——所以它们可以在每个 release 目录中保留通用的基础文件名(`ProjectA.kt` 等)。

3. **重命名副本中的每一个 object**，给每个都加上 release 单词作为前缀——这套 DSL 中的 Kotlin object 全部共享同一个默认包(`.teamcity/` 下任何地方都没有 `package` 声明，这是有意为之——见 `IdPath.kt`)，所以 `main` 中的 `Main_ProjectA`、`Main_BuildCImage`、`Main_ResultBuild`、`Main_BaseBuild`、`Main_ProjectAVcs`、`Main_ProjectBVcs` 都已经被占用了。这和 CMake 的 `add_library`/`add_executable` target 名称面临的约束是一样的——一个扁平的全局命名空间，所有名字都必须唯一。`main` 自己的 object 同样被加了前缀(而不是保持裸露)，专门是为了让从*任意* release 复制这个操作方式都一致，`main` 本身也不例外——不存在特殊情况。对 `release_2_0` 而言：

   | main/(object 名) | release_2_0/(object 名) |
   |--------------------------|-------------------------------------|
   | `Main`                   | `Release20`                        |
   | `MainId`                 | `Release20Id`                      |
   | `MainConfigName`         | `Release20ConfigName`              |
   | `Main_ProjectA`      | `Release20_ProjectA`           |
   | `Main_ProjectB`      | `Release20_ProjectB`           |
   | `Main_ProjectAVcs`   | `Release20_ProjectAVcs`        |
   | `Main_ProjectBVcs`   | `Release20_ProjectBVcs`        |
   | `Main_BuildCImage`       | `Release20_BuildCImage`            |
   | `Main_ResultBuild`       | `Release20_ResultBuild`            |
   | `Main_BaseBuild`         | `Release20_BaseBuild`              |

   除了把 `MainId` 换成 `Release20Id` 之外，**不要**改动传给 `id(...)` 调用的裸字符串字面量(例如 `id((MainId / "ProjectA").toString())`)——正是这个字符串被 `IdPath` 用来拼出真正的 TeamCity id(`CxxCiDemo_Release20_ProjectA`)。如果连它也加上前缀，这个单词就会在 id 中重复出现(这是在真实验证这个流程时实际踩中并两次被抓到的错误：第一次是 `CxxCiDemo_Main_Main_ProjectA`，第二次被 `scripts/new-release.sh` 自身的合理性检查再次抓到)。如果手动重命名，对整个单词做一次简单的 `sed`(`s/\bMain\b/Release20/g`)也会误伤这些字符串字面量，因为 sed 无法分辨 Kotlin 标识符和字符串——这正是脚本要把 `*Id`/`*ConfigName`/`<单词>_` 前缀替换拆成几个更窄的独立步骤、而不是一次性整体替换单词的原因。

   同样**不要**改动 `buildTypes/BuildCImage.kt` 中 `docker build -f .teamcity/cxx_ci_demo/${...ConfigName}/Dockerfile .teamcity/cxx_ci_demo/${...ConfigName}` 里字面的 `.teamcity/cxx_ci_demo/` 路径片段——这一行里只需要把 `${MainConfigName}` 这个引用改成 `${Release20ConfigName}`，与你重命名的 val 保持一致。`.teamcity/cxx_ci_demo/` 部分是固定的(这是每个 release 目录相对于 TeamCity 实际检出的仓库根目录的真实位置——`DslContext.settingsRoot` 不支持 agent 侧检出的自定义 checkout rules，这一点已在真实环境中确认过，所以检出的永远是整个 `ci-infra` 仓库，里面的路径必须显式写出)。

4. **在项目文件中**(`Release20.kt`)：设置 `val Release20Id = CxxCiDemoId / "Release20"`、`val Release20ConfigName = "release_2_0"`、`name = Release20ConfigName`，以及把 `branch_default`/`branch_spec` 参数改成新的 `config_name`(见上文)。`gitlab_credentials_password` 保持 `password("gitlab_credentials_password", "")`——空的默认值，和 `main` 完全一样。不要手动写入 `credentialsJSON:...` 这样的值；它是特定服务器专属的，由 TeamCity 自身在应用时写入，而不是在各 release 之间复制的东西。

5. **注册它**：在 `cxx_ci_demo/CxxCiDemo.kt` 中添加 `subProject(Release20)`。

6. **推送到 `ci-infra`，等待应用生效**，然后运行一次 `bootstrap.sh`，让它把 GitLab 凭据也注入新 release 的 VCS root(它目前是对 `CxxCiDemo_Main_ProjectA`/`B`/`C`/`D`/`E` 做循环——当这不再是单 release 的 demo 时，需要扩展这个循环，或者添加新 release 的 VCS root id)。

7. **在从 `project_a` 到 `project_e` 的 GitLab 仓库中创建实际分支**：至少需要 `refs/heads/<config_name>`，这样该 release 的 VCS root 才有东西可构建。

## 验证是否成功

`GET /app/rest/projects/id:_Root/versionedSettings/status` 对你提交的 revision/message 应当以 `"Changes from VCS are applied to project settings"` 结束，而不是 `Kotlin DSL compilation errors` 警告。编译失败**不会**影响已经应用的那棵树——之前的 release 配置会继续正常构建——所以可以安全地直接在真实的 `ci-infra` 仓库上迭代，而不需要单独的测试项目，而且值得在信任一个已写入但尚未推送的改动之前先这样验证一遍。

如果你要删除某个 release(本文档自己的测试就这么做过两次)，也要一并删除所有引用它的 `.teamcity/patches/vcsRoots/<Prefix>_*.kts` / `.teamcity/patches/projects/<Prefix>*.kts` 文件——当 secret 被直接注入到某个 VCS root 时，TeamCity 会自动提交这些文件(见 ADR 0004)，而一个指向已删除 VCS root 的过期文件会让*下一次*同步失败，报错 `Expected VCS root with id '...' not found`，并阻塞所有改动，即便是完全无关的改动，直到清理干净为止。
