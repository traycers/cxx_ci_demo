[🇬🇧 English](../../en/adr/0011-track-term-replaces-release-for-branch-family.md) · [🇷🇺 Русский](../../ru/adr/0011-track-term-replaces-release-for-branch-family.md) · 🇨🇳 中文

# 分支族术语从 `Release` 改名为 `Track`,把 `release` 让给 package variant

`CONTEXT.md` 把 **Release**(分支族)定义为 `ci-infra` 中的一个 `cxx_ci_demo/<config_name>/` 子树——拥有自己的 TeamCity 子项目和 VCS root;而另一边,`docs/en/roadmap.md` 计划中的 **package variant** 叫 `optimized`(即今天的 `CMAKE_BUILD_TYPE=RelWithDebInfo`),之所以刻意不叫 `release`,是为了避免 `project_a/release` 在这两者之间产生歧义。这确实避开了冲突,但也让 package variant 永远无法使用那个真正能描述它自身的名字,同时分支族这边用的词其实也一直不太贴切:这些并不一定是面向公众的产品发布,而是相互独立、可能长期存在的配置分支(`release_1` = 原始的项目结构,`release_2` = 改进后的结构,`release_3` = 依赖树的变化),它们可以无限期共存。

一开始考虑过用 `stage` 作为替代词,但被否决了:在 CI/CD 语境里,"stage" 早已被占用——要么指流水线的一个步骤,要么指 staging(预生产)环境,而这个仓库既没有带步骤的流水线,也没有环境层级,只有并行、独立的分支。于是改选 `track`(如浏览器的 release track):它准确描述了这个概念的实际形态——一条独立、长期存在的配置线——而不会带入上述任何一种错误联想。

分支族这个概念所涉及的一切都已改名:`CONTEXT.md` 的术语条目,以及 `docs/{en,ru,zh}/*.md` 里的每一处散文提及;TeamCity Kotlin DSL(`release_1`/`release_2`/`release_3` 目录和文件 → `track_1`/`track_2`/`track_3`,每一个 `ReleaseN`/`ReleaseNId`/`ReleaseN_*` 标识符 → `TrackN`/`TrackNId`/`TrackN_*`,config-name 字符串字面量,`branch_default`/`branch_spec`,docker 标签前缀);`scripts/new-release.sh` → `new-track.sh`;demo 项目的分支种子目录(`repos/project_{a..e}/release_N` → `track_N`);以及文档本身(`adding-a-release.md`/`releases.md`/`release.md` → `adding-a-track.md`/`tracks.md`/`track.md`)。改名过程中发现,Kotlin 里的 `ConfigName` 后缀(`Release1ConfigName` 等)其实是文档层面造出来的词,并非代码里的字面字符串,于是也一并改成了 `TrackName`——如果只改后缀,会产生像 `Release1TrackName` 这样不一致的标识符。`optimized` 在尚未实现的 package variant 计划里改名为 `release`,因为它原本要避免的那个冲突已经不存在了。

未改动的部分:`CONTEXT.md` 里 `config_name` 这个术语条目本身保留了原名(只修正了其中已经过时的示例,以保持一致)——这是改名过程中刻意收窄的一个决定,没有并入 `TrackName` 的改动;正在运行的 GitLab/TeamCity 环境,那里已经真实存在 `release_1`/`release_2` 分支以及由它们构建出来的 TeamCity 子项目——这次改名只涉及仓库文件,如果以后确实需要,迁移线上环境将是另一个独立的动作;此外,遵循本仓库自己在 ADR 0010 中确立的先例,早于这次改名的那些 ADR(0002、0005、0007、0008、0010)都保持原样未动,作为这些决定在当时实际内容的历史记录,而不是被改写成新名字。
