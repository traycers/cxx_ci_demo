🇬🇧 English · [🇷🇺 Русский](../ru/track.md) · [🇨🇳 中文](../zh/track.md)

# Track

A **track** is a group of the several projects needed to ship a track.

Organizing a track as its own directory makes it cheap to create a new track (copy and adjust an existing one), delete a track that's gone out of support, and keep tracks independent of one another.

## Contents

A track has a name. That name is consistent across GitLab and TeamCity.

The track's default branch is named `<track_name>`. Every other branch in git follows the pattern `<track_name>-*`. TeamCity's VCS roots use the same pattern.

A commit to a branch triggers a build in TeamCity; per the filter, only the build chain for that specific track runs. To make a change, the developer creates a branch with the same name in whichever repos they need it in. If a repo doesn't have a branch by that name, TeamCity takes artifacts from the default branch instead.

The `result` build is the track's overall result — it carries the trigger that fires builds based on VCS commits.

> Important!
> The track has the same name in GitLab, TeamCity, and git branches.
> Repos in GitLab and build configurations in TeamCity have the same names.

Matching names rule out confusion between the different services and make navigation fast. As a bonus, TeamCity's grouping and GitLab's repo nesting can mirror each other.

## Change diagram

```mermaid
flowchart TD
    A["Track created<br/>(config_name defined in ci-infra)"] --> B["Default branch created in every needed repo:<br/>refs/heads/&lt;track_name&gt;"]
    B --> C{"Change needed?"}
    C -- "No — commit goes straight to the default branch" --> D["Push to refs/heads/&lt;track_name&gt;"]
    C -- "Yes — feature/hotfix" --> E["Create refs/heads/&lt;track_name&gt;-*<br/>only in the repos that need it"]
    E --> F["Push to refs/heads/&lt;track_name&gt;-*"]
    D --> G["VCS trigger fires in TeamCity<br/>filtered to this track only"]
    F --> G
    G --> H{"Does every dependent repo<br/>have a matching branch?"}
    H -- "Yes" --> I["Build chain uses that branch's<br/>artifacts in every repo"]
    H -- "No" --> J["The repo without a matching branch<br/>falls back to its default branch"]
    I --> K["result build aggregates everything<br/>and publishes result.zip"]
    J --> K
    K --> C
    C -- "Track retired" --> L["Directory and branches removed<br/>from ci-infra and the project repos"]
```

## Tracks diagram

```mermaid
gitGraph
   commit id: "init"

   branch feature_1 order: 0
   commit id: "feature_1 work"
   commit id: "feature_1 work 2"
   checkout main
   merge feature_1
   commit id: "main update 1"

   branch track_1 order: 4
   commit id: "track_1 start"
   branch hotfix_1 order: 5
   commit id: "hotfix_1 fix"
   commit id: "hotfix_1 fix 2"
   checkout track_1
   merge hotfix_1
   checkout main
   merge hotfix_1
   merge track_1 id: "track_1 launch" type: HIGHLIGHT
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
   branch track_2 order: 7
   commit id: "track_2 start"
   checkout main
   merge track_2 id: "track_2 launch" type: HIGHLIGHT

   checkout track_2
   branch hotfix_4 order: 8
   commit id: "hotfix_4 fix"
   commit id: "hotfix_4 fix 2"
   checkout track_2
   merge hotfix_4
   checkout main
   merge hotfix_4

   checkout track_1
   branch hotfix_3 order: 6
   commit id: "hotfix_3 fix"
   commit id: "hotfix_3 fix 2"
   checkout track_1
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
