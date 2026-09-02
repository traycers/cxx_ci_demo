[🇬🇧 English](../en/track.md) · [🇷🇺 Русский](../ru/track.md) · 🇨🇳 中文

# Track

**Track** 是发布一个版本所需要的若干个项目的集合。

把 track 组织成独立的目录，是为了让创建新 track(复制并调整一个已有的)、删除已经停止维护的 track、以及让各个 track 互不影响都变得简单。

## 内容

Track 有自己的名字，这个名字在 GitLab 和 TeamCity 中保持一致。

Track 的默认分支名为 `<track_name>`。git 中其余的分支都遵循 `<track_name>-*` 这个命名模式，TeamCity 的 VCS root 用的是同一个模式。

向分支提交代码会触发 TeamCity 中的构建；根据过滤规则，只有这个 track 自己的构建链会被触发。要做修改，开发者需要在自己需要改动的那些仓库里，创建一个同名的分支。如果某个仓库里没有这个名字的分支，TeamCity 就会改用默认分支的构建产物。

`result` 构建是这个 track 的总体结果——触发它的，是基于 VCS 提交的触发器。

> 重要！
> track 在 GitLab、TeamCity 和 git 分支里用的是同一个名字。
> GitLab 里的仓库名和 TeamCity 里的 build configuration 名保持一致。

名字保持一致，可以避免在不同服务之间产生混淆，也让人能快速定位。这样做还有一个额外的好处：TeamCity 里的分组方式和 GitLab 里仓库的嵌套结构可以互相对应。

## 变更流程图

```mermaid
flowchart TD
    A["Track 创建<br/>(在 ci-infra 中定义 config_name)"] --> B["在每个需要的仓库中<br/>创建默认分支:refs/heads/&lt;track_name&gt;"]
    B --> C{"需要改动吗?"}
    C -- "不需要——直接提交到默认分支" --> D["Push 到 refs/heads/&lt;track_name&gt;"]
    C -- "需要——feature/hotfix" --> E["只在需要的仓库中<br/>创建 refs/heads/&lt;track_name&gt;-*"]
    E --> F["Push 到 refs/heads/&lt;track_name&gt;-*"]
    D --> G["TeamCity 中触发 VCS 触发器<br/>只过滤出这一个 track"]
    F --> G
    G --> H{"每个依赖的仓库<br/>都有匹配的分支吗?"}
    H -- "是" --> I["构建链在每个仓库中<br/>都使用该分支的构建产物"]
    H -- "否" --> J["没有匹配分支的仓库<br/>回退到自己的默认分支"]
    I --> K["result 构建汇总一切<br/>并发布 result.zip"]
    J --> K
    K --> C
    C -- "track 停止维护" --> L["从 ci-infra 和各 project 仓库中<br/>删除目录和分支"]
```

## Track 关系图

```mermaid
gitGraph
   commit id: "init"

   branch feature_1 order: 0
   commit id: "feature_1 work"
   commit id: "feature_1 work 2"
   checkout main
   merge feature_1
   commit id: "main update 1"

   branch release_1 order: 4
   commit id: "release_1 start"
   branch hotfix_1 order: 5
   commit id: "hotfix_1 fix"
   commit id: "hotfix_1 fix 2"
   checkout release_1
   merge hotfix_1
   checkout main
   merge hotfix_1
   merge release_1 id: "release_1 launch" type: HIGHLIGHT
   commit id: "main update 2"

   branch hotfix_2 order: 1
   commit id: "hotfix_2 fix"
   commit id: "hotfix_2 fix 2"
   checkout main
   merge hotfix_2
   commit id: "main update 3"

   branch feature_2 order: 2
   commit id: "feature_2 work"
   commit id: "feature_2 work 2"
   checkout main
   merge feature_2
   commit id: "main update 4"

   branch track_client_x order: 9
   commit id: "track_client_x start"
   branch special_feature order: 10
   commit id: "special_feature work"
   commit id: "special_feature work 2"
   checkout track_client_x
   merge special_feature
   commit id: "track_client_x continues"

   checkout main
   branch release_2 order: 7
   commit id: "release_2 start"
   checkout main
   merge release_2 id: "release_2 launch" type: HIGHLIGHT

   checkout release_2
   branch hotfix_4 order: 8
   commit id: "hotfix_4 fix"
   commit id: "hotfix_4 fix 2"
   checkout release_2
   merge hotfix_4
   checkout main
   merge hotfix_4

   checkout release_1
   branch hotfix_3 order: 6
   commit id: "hotfix_3 fix"
   commit id: "hotfix_3 fix 2"
   checkout release_1
   merge hotfix_3
   checkout main
   merge hotfix_3
   commit id: "main update 5"

   branch feature_3 order: 3
   commit id: "feature_3 work"
   commit id: "feature_3 work 2"
   checkout main
   merge feature_3
   commit id: "main final"

```
