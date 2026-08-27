[🇬🇧 English](../../en/adr/0007-per-branch-bootstrap-content.md) · [🇷🇺 Русский](../../ru/adr/0007-per-branch-bootstrap-content.md) · 🇨🇳 中文

_翻译自 `docs/en/adr/0007-per-branch-bootstrap-content.md`。原文变更时请同步更新本文件——参见 [ADR 0006](0006-trilingual-docs-mirror-tree.md)。_

# 按分支划分的 bootstrap 内容——`bootstrap/<repo>/<branch>/`

此前 `bootstrap/<repo>/` 是扁平的——每个仓库一个文件目录，由 `push_repo_content()` 作为唯一的 `main` 分支推送。我们把它改造成了 `bootstrap/<repo>/<branch>/`——每个分支对应一个子目录，这样一个仓库就可以从一开始就用多个预先准备好、内容刻意不同的分支来填充，而不只是 `main`。三个已有仓库(`ci-infra`、`demo-project-a`、`demo-project-b`)在同一次改动中就迁移到了这个结构(它们的内容被移进了 `.../main/`)，而不是继续留在旧的扁平结构上——这样 `bootstrap.sh` 只需要支持一种目录形态。

`bootstrap.sh` 通过扫描 `bootstrap/<repo>/` 的子目录来发现分支——要新增一个待填充的分支，只需新建一个目录，不需要改动脚本。每个分支都作为一个独立的 orphan 提交推送(全新的 `git init`，单个提交)，而不是构建在 `main` 的历史之上：这些目录本来就是刻意不同的内容，而不是同一份代码的分叉，所以共享的根提交没有任何意义。只要存在 `main`，它总是被最先推送——因为 GitLab 会把第一个推送到空仓库的分支设为默认分支，先推送 `main` 就能让这个默认分支不受目录扫描顺序影响。重新运行 `bootstrap.sh` 会对每个分支独立检查并推送(对每个分支各自执行一次 `git ls-remote`，而不是对整个仓库执行一次)，所以在已经完成 bootstrap 的仓库里新增一个分支目录后重新运行脚本，只会推送这个新分支，而不会因为仓库里已经存在别的分支就整体跳过。

这有意地与 `adding-a-release.md` 中创建分支的步骤无关：那个步骤是在一个内容早已完整的 demo-project 仓库里新建一个普通的 git 分支，纯粹是为了让 TeamCity release 的 VCS root 有东西可构建(没有独立的内容，和 `main` 是同一份代码)——而这里的机制针对的是那些从一开始就需要每个分支内容真正不同的仓库。把新仓库接入 TeamCity 本身(Kotlin DSL 中的 VCS root、build configuration)不在本 ADR 范围内——这里只描述 `bootstrap.sh` 如何把内容送到 GitLab 的各个分支上，不描述 TeamCity 如何被告知去构建它们。

结论：对三个已有仓库而言，`bootstrap/<repo>/<branch>/.../*` 路径都下移了一层——例如 `bootstrap/ci-infra/.teamcity/` 现在是 `bootstrap/ci-infra/main/.teamcity/`。任何硬编码了旧的扁平路径的地方(`scripts/new-release.sh`、`bootstrap.sh` 自己的日志信息、本仓库的 `README.md`)都已更新；ADR 0003/0004 早于这次改动，描述的是另一个不相关的决定，作为历史记录被保留，没有为了这次路径变化而回过头去修改。
