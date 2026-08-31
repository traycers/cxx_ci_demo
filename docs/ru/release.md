[🇬🇧 English](../en/release.md) · 🇷🇺 Русский · [🇨🇳 中文](../zh/release.md)

_Перевод `docs/en/release.md`. При изменении оригинала обновите и эту версию — см. [ADR 0006](adr/0006-trilingual-docs-mirror-tree.md)._

# Релиз

Релиз - группа нескольких проектов, необходимые для выпуска релиза.

Организация релиза в отдельную директорию необходима для простого создания нового релиза (путём копирования и изменения другого), удаления релиза, вышедшего из поддержки, и независимости одних релизов от других.

## Содержимое

Релиз имеет имя. Это имя согласуется с Gitlab и Teamcity.

Так ветка по умолчанию для релиза имеет имя `%release name%`. Имена, остальных веток в git, формируются по следующему шаблону `%release name%-*`. Тот же паттерн используется в VCS в Teamcity.

Коммит в ветку запускает сборку в Teamcity, согласно фильтру, запускается цепочка сборок только нужного релиза.
Для внесения изменений, создаётся ветка во всех, необходимых разработчику, репозиториях с одинаковым именем. При отсутствии такого имени в репозитории, Teamcity берёт артефакты из ветки по умолчанию.

Сборка `result` является результатом релиза, в ней находится триггер запуска сборок на основе коммитов в VCS.

> Важно!
> одинаковые названия у релиза в gitlab, teamcity и git branches.
> одинаковые названия у репозиториев в gitlab и конфигурации сборки в Teamcity.

Одинаковые имена исключают путаницу между разными сервисами и обеспечивают быструю навигацию. Дополнительно, можно отражать группами в Teamcity, вложенность репозиториев в Gitlab.


## Схема изменений

```mermaid
flowchart TD
    A["Релиз создан<br/>(config_name задан в ci-infra)"] --> B["В каждом нужном репозитории создана<br/>ветка по умолчанию: refs/heads/&lt;release_name&gt;"]
    B --> C{"Нужно изменение?"}
    C -- "Нет — коммит прямо в ветку по умолчанию" --> D["Push в refs/heads/&lt;release_name&gt;"]
    C -- "Да — feature/hotfix" --> E["Создать refs/heads/&lt;release_name&gt;-*<br/>только в тех репозиториях, где нужно"]
    E --> F["Push в refs/heads/&lt;release_name&gt;-*"]
    D --> G["Срабатывает VCS-триггер в TeamCity,<br/>отфильтрованный только на этот релиз"]
    F --> G
    G --> H{"У каждого зависимого репозитория<br/>есть подходящая ветка?"}
    H -- "Да" --> I["Цепочка сборок берёт артефакты<br/>из этой ветки в каждом репозитории"]
    H -- "Нет" --> J["Репозиторий без подходящей ветки<br/>откатывается на свою ветку по умолчанию"]
    I --> K["Сборка result агрегирует всё<br/>и публикует result.zip"]
    J --> K
    K --> C
    C -- "Релиз выведен из поддержки" --> L["Директория и ветки удалены<br/>из ci-infra и репозиториев проектов"]
```

## Схема релизов

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
