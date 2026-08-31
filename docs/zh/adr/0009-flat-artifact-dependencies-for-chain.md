[🇬🇧 English](../../en/adr/0009-flat-artifact-dependencies-for-chain.md) · [🇷🇺 Русский](../../ru/adr/0009-flat-artifact-dependencies-for-chain.md) · 🇨🇳 中文

_翻译自 `docs/en/adr/0009-flat-artifact-dependencies-for-chain.md`。原文变更时请同步更新本文件——参见 [ADR 0006](0006-trilingual-docs-mirror-tree.md)。_

# a→c→d 整条链上的扁平 artifact 依赖

`project_a` 现在经由 `project_c`(`vecopscale`)链到 `project_d`(`vecutils`)——`a` 中的 `app_a_core` 以 PUBLIC 方式链接 `c`,而 `c` 又以 PUBLIC 方式链接 `d`,因此 `d` 的 `Vector2` 类型直接出现在 `c` 的公共头文件里。尽管存在这条传递性的 C++ 依赖,`a` 在 TeamCity 中的 build type 却**同时**对 `c` 和 `d` 声明了显式的 artifact 依赖,而不仅仅是对 `c`——与 `c` 自身对 `d` 的依赖形状完全一致。乍一看这显得多余("`a` 已经依赖 `c`,`c` 又依赖 `d`,为什么 `a` 还要直接依赖 `d`?"),所以有必要记录原因。

每个 demo 项目的 `CMakeLists.txt` 都在用的 cmake 机制——`install_component`/`install_package_config`——只会把项目**自己**的 target 和头文件打进自己的安装前缀,绝不会把某个依赖项已经安装好的文件一并复制进来。所以 `c` 的 sdk.zip 只包含 `vecopscale` 自己的头文件、库和 package config,不包含 `d` 的任何东西。任何需要 `find_package(project_c)` 完全解析成功的消费者(`project_c` 导出的 config 自己会去调用 `find_dependency(project_d)`),以及任何需要把 `libvecutils.a` 真正链接进最终二进制文件的消费者,都需要 `d` 的真实文件同样出现在自己的 `%deps_dir%` 里——而不只是 `c` 的文件。

我们选择让链上每个 build type 都对它传递需要的每一个包各自声明一条 artifact 依赖(扁平模型),而不是让链中某个项目的安装步骤去把其依赖的文件重新打包进自己的产物里(嵌套模型)。这与 `install_component` 本身的行为完全一致,让每个项目的 sdk.zip 始终只意味着"只有我自己的文件,不多不少"(因此单独重新构建一个项目时,产物内容永远一致),并且让 `a→c→d` 这条链成为 `install_package_config` 真实解析机制的一次现场演示——这正是这个演示仓库存在的全部意义——而不是把这套机制藏在一个打包捷径背后。
