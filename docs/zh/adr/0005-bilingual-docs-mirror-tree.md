[🇬🇧 English](../../en/adr/0005-bilingual-docs-mirror-tree.md) · [🇷🇺 Русский](../../ru/adr/0005-bilingual-docs-mirror-tree.md) · 🇨🇳 中文

**已被 [ADR 0006](0006-trilingual-docs-mirror-tree.md) 取代**：文档语言从两种扩展为三种(新增中文)；保留本文档用于记录历史。

# 双语文档——`docs/en/` + `docs/ru/`，根目录的 `README.md`/`CONTEXT.md` 保持不动

本仓库的文档同时存在英语和俄语版本。我们最初尝试的是以 `docs/<lang>/` 为根的镜像目录树，无论某篇文档的英文原文实际放在哪里(仓库根目录还是 `docs/`)，都为它保存一份译文——于是 `README.md`/`CONTEXT.md` 直接翻译成了 `docs/ru/README.md`/`docs/ru/CONTEXT.md`。实际使用中，这把英文和俄文文件混在了 `docs/` 目录本身里面(`docs/adr/*.md` 直接和 `docs/ru/` 子文件夹并排放着)——浏览起来令人困惑，也和「`docs/ru/` 本身看起来是一套完整独立的文档集」这一点自相矛盾。

我们改为对 `docs/` 目录内真正存在的一切采用简单的对称划分：`docs/en/` 存放除 `README.md`/`CONTEXT.md` 之外的每一篇英文文档，`docs/ru/` 是它精确的镜像(`docs/en/adr/0001-....md` ↔ `docs/ru/adr/0001-....md`,`docs/en/adding-a-release.md` ↔ `docs/ru/adding-a-release.md`)。`README.md` 和 `CONTEXT.md` 是刻意的例外：它们以英语留在仓库根目录，而不是放进 `docs/en/`，因为那正是 GitHub 渲染仓库主页所需要 `README.md` 所在的位置，也是 `domain-modeling` 技能按约定查找 `CONTEXT.md` 的位置。它们的俄语译文依然放在 `docs/ru/README.md` 和 `docs/ru/CONTEXT.md`，所以 `docs/ru/` 仍然是完整的俄语镜像，即便这两个文件对应的英文一侧是仓库根目录，而不是 `docs/en/`。

`CONTEXT.md` 是这整项工作中唯一一个权威语言发生了变化的文件(此前混合语言版本的历史记录见提交历史)：它现在在根目录完全以英语呈现——是未来 `domain-modeling` 会话的权威版本——此前的俄语内容则作为其译文保留在 `docs/ru/CONTEXT.md` 中。

每一对语言版本的文件顶部都带有语言切换链接，译文一侧还额外带有一条简短说明，指回其来源文件，以便保持同步。

结论：从这一刻起，每一篇新文档(新的 ADR、新的指南)在被视为完成之前，都必须同时以两种语言创建——只添加了英文版本的文档，是一次未完成的添加，而不是一个可以之后再做的后续任务。新的 ADR 同时进入 `docs/en/adr/` + `docs/ru/adr/`；除了 `README.md`/`CONTEXT.md` 本身之外，仓库根目录不会再新增任何东西。这是一项刻意的、持续存在的成本，而不是一次性的回填；只有在从未有内容被单独添加到镜像的一侧时，这份镜像才能保持可信。
