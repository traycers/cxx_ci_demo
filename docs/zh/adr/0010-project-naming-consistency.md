[🇬🇧 English](../../en/adr/0010-project-naming-consistency.md) · [🇷🇺 Русский](../../ru/adr/0010-project-naming-consistency.md) · 🇨🇳 中文

_翻译自 `docs/en/adr/0010-project-naming-consistency.md`。原文变更时请同步更新本文件——参见 [ADR 0006](0006-trilingual-docs-mirror-tree.md)。_

# 将 `demo-project-*` 全面改名为 `project_*`,以统一各工具之间的命名

每个 C++ 项目自己的 `CMakeLists.txt` 一直都把 `PACKAGE_NAME` 设为 `project_a`、`project_b` 等——这正是写进每个导出的 cmake target(`cxx::ci::demo::project_c::vecopscale`)以及每次 `find_package()`/`find_dependency()` 调用里的名字。但 GitLab 仓库和 TeamCity DSL 却把同一个项目叫作 `demo-project-a`,TeamCity 的 Kotlin 标识符又用第三种写法(`Main_DemoProjectA`,id 路径 `"DemoProjectA"`)。三个工具,同一个项目三种不同的名字,彼此之间没有任何命名规则相互对应——新读者根本无法猜到 GitLab 仓库 `demo-project-c`、TeamCity id `CxxCiDemo_Main_DemoProjectC` 和 cmake 包 `project_c` 其实是同一个东西。

我们把所有地方都改了名——GitLab 仓库名/URL、bootstrap 用来推送内容的本地种子目录 `repos/<name>/`,以及 TeamCity 里的每一个 Kotlin 标识符/id/文件名/display name,`main` 和现有的两个 release(`release_1`、`release_2`——它们共用同样的物理 GitLab 仓库,如果不改就会留下指向一个已不存在的仓库名的失效 URL)全都改了——统一改成 cmake 早就在用的那一个名字:`project_a` 到 `project_e`。没有兼容层,也没有过渡期:这是一个没有外部消费者依赖旧名字的演示仓库,没有理由在任何地方同时保留两种写法。

遵循本仓库自己已经确立的惯例(见 ADR 0008 中关于 ADR 0007 的说明),早于这次改名的那些 ADR(0006、0007、0008)都保持原样未动,作为这些决定在当时实际内容的历史记录,而不是被改写成新名字。
