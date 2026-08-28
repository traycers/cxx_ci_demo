🇬🇧 English · [🇷🇺 Русский](../ru/release.md) · [🇨🇳 中文](../zh/release.md)

# Release

A **release** is a group of the several projects needed to ship a release.

Organizing a release as its own directory makes it cheap to create a new release (copy and adjust an existing one), delete a release that's gone out of support, and keep releases independent of one another.

## Contents

A release has a name. That name is consistent across GitLab and TeamCity.

The release's default branch is named `<release_name>`. Every other branch in git follows the pattern `<release_name>-*`. TeamCity's VCS roots use the same pattern.

A commit to a branch triggers a build in TeamCity; per the filter, only the build chain for that specific release runs. To make a change, the developer creates a branch with the same name in whichever repos they need it in. If a repo doesn't have a branch by that name, TeamCity takes artifacts from the default branch instead.

The `result` build is the release's overall result — it carries the trigger that fires builds based on VCS commits.

> Important!
> The release has the same name in GitLab, TeamCity, and git branches.
> Repos in GitLab and build configurations in TeamCity have the same names.

Matching names rule out confusion between the different services and make navigation fast. As a bonus, TeamCity's grouping and GitLab's repo nesting can mirror each other.

## Change diagram

```mermaid
flowchart TD
    A["Release created<br/>(config_name defined in ci-infra)"] --> B["Default branch created in every needed repo:<br/>refs/heads/&lt;release_name&gt;"]
    B --> C{"Change needed?"}
    C -- "No — commit goes straight to the default branch" --> D["Push to refs/heads/&lt;release_name&gt;"]
    C -- "Yes — feature/hotfix" --> E["Create refs/heads/&lt;release_name&gt;-*<br/>only in the repos that need it"]
    E --> F["Push to refs/heads/&lt;release_name&gt;-*"]
    D --> G["VCS trigger fires in TeamCity<br/>filtered to this release only"]
    F --> G
    G --> H{"Does every dependent repo<br/>have a matching branch?"}
    H -- "Yes" --> I["Build chain uses that branch's<br/>artifacts in every repo"]
    H -- "No" --> J["The repo without a matching branch<br/>falls back to its default branch"]
    I --> K["result build aggregates everything<br/>and publishes result.zip"]
    J --> K
    K --> C
    C -- "Release retired" --> L["Directory and branches removed<br/>from ci-infra and the demo-project repos"]
```

## Releases diagram

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

   branch release_client_x order: 9
   commit id: "release_client_x start"
   branch special_feature order: 10
   commit id: "special_feature work"
   commit id: "special_feature work 2"
   checkout release_client_x
   merge special_feature
   commit id: "release_client_x continues"

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
