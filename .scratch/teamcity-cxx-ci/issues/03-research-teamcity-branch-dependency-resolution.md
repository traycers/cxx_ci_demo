Type: research
Status: resolved
Blocked by: (none)

## Question

Какой именно встроенный механизм TeamCity реализует «зависимость берётся из той же ветки, что и триггерящая сборка, а если такой ветки в VCS root'е зависимости нет — из default branch» — и как это выражается в Kotlin DSL?

Нужно выяснить и задокументировать:
- Точное название настройки (ожидается что-то в духе branch filter / "same branch as triggering build" в snapshot dependency) и с какой версии TeamCity она доступна.
- Применимо ли это только к snapshot-зависимостям, или для artifact-зависимостей нужна отдельная/дополнительная настройка (см. решение "snapshot + artifact вместе" в `map.md`) — если отдельная, задокументировать и её.
- Точный синтаксис Kotlin DSL для этой настройки (класс/метод в `jetbrains.buildServer.configs.kotlin`), пригодный для непосредственной вставки в build config зависимой сборки.
- Поведение на граничных случаях: ветка есть у триггерящей сборки, но полностью отсутствует у зависимости (ни она, ни default branch не сконфигурированы) — что произойдёт (ошибка конфигурации, fallback, skip)?

Результат — точный DSL-фрагмент для использования в тикете 07 (Kotlin DSL ci-infra) и сценарий, который тикет 09 должен воспроизвести для проверки fallback на default branch.

## Answer

Полные находки: `.scratch/teamcity-cxx-ci/research/teamcity-branch-dependency.md`.

- **Нет отдельного "тумблера"** — это два разных механизма:
  - **Snapshot-зависимость**: branch-matching по "логическому имени ветки" — встроенное, неотключаемое поведение build chain (не DSL-свойство). Если у зависимости нет такой же ветки — берётся default branch зависимости. Работает "из коробки".
  - **Artifact-зависимость**: официально называется **"Build branch filter"**; в DSL за это отвечает `ArtifactDependency.buildRule` (по умолчанию `sameChainOrLastFinished()`).
- **Для этого стенда (snapshot + artifact на одну и ту же B) отдельно настраивать artifact-зависимость НЕ нужно**: дефолтный `buildRule` наследует именно ту сборку, которую уже разрешила snapshot-зависимость (branch/default — бесплатно). Явный branch-based `buildRule` нужен только для «одиночной» artifact-зависимости без парной snapshot.
- **Готовый DSL** (Case A, наш случай — см. файл целиком):
  ```kotlin
  dependencies {
      snapshot(BProject.BuildB) {
          onDependencyFailure = FailureAction.FAIL_TO_START
      }
      artifacts(BProject.BuildB) {
          buildRule = sameChain()   // наследует ветку, уже разрешённую snapshot-зависимостью
          artifactRules = "+:*.tar.gz => artifacts/b"
      }
  }
  ```
- **Важная предпосылка** (иначе тихий fallback вместо ожидаемого matching): `branchSpec` должен быть настроен СИММЕТРИЧНО на VCS root'ах ОБОИХ проектов (A и B), чтобы одна и та же физическая ветка давала одинаковое "логическое имя" в обоих. Односторонняя настройка — частая скрытая ловушка.
- **Граничный случай** «ветки нет нигде, включая default» — недостижим штатной конфигурацией: у VCS root default branch всегда есть значение (`refs/heads/master`, если не переопределено). Реалистичный сценарий для тикета 09: «у A ветка есть, у B такой ветки нет → B резолвится в свой default branch» — именно это и задокументировано и должно быть проверено.
