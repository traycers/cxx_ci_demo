Type: task
Status: resolved (superseded, then un-superseded — see below)
Blocked by: 03, 06

## Question

Написать Kotlin DSL (попадёт в `bootstrap/ci-infra/`) для всего дерева TeamCity build configuration:

- Root TeamCity Project.
- VCS root + build configuration для корневой сборки образа (Dockerfile из тикета 05, `docker build`, публикует тег в конфигурационную переменную `%build_image_cxx%`).
- VCS root + build configuration для `demo-project-b` (собирается внутри контейнера из `%build_image_cxx%`, публикует артефакт — см. тикет 06 для пути).
- VCS root + build configuration для `demo-project-a` (аналогично, плюс artifact-зависимость на `demo-project-b` и snapshot-зависимость с branch-match/default-fallback по результату тикета 03).
- Snapshot-зависимость обеих сборок проектов на корневую сборку образа (пересборка при обновлении образа — см. `map.md`).

Готово, когда DSL синтаксически валиден и описывает полный граф зависимостей (образ → demo-project-b → demo-project-a) с нужной branch-резолюцией.

## Answer

`bootstrap/ci-infra/.teamcity/settings.kts` — Root Project, 3 VCS root'а (ci-infra, demo-project-a, demo-project-b, у всех симметричный `branchSpec = "+:refs/heads/*"` — предпосылка из ответа тикета 03), 3 build configuration:

- `BuildImage` — VCS root ci-infra, `docker build -t cxxci-build:%build.number% .`.
- `DemoProjectB` — snapshot-зависимость на `BuildImage`, `%build_image_cxx% = cxxci-build:%dep.BuildImage.build.number%`, сборка через `docker run` с volume-mount checkout dir (модель исполнения из charting, Q5), публикует `dist/lib/libmathutils.a => lib` и `dist/include/mathutils.h => include` (раскладка из ответа тикета 06).
- `DemoProjectA` — snapshot на `BuildImage` И на `DemoProjectB`, artifact-зависимость на `DemoProjectB` с `buildRule = sameChain()` (Case A из тикета 03 — не задавать отдельный branch-based `buildRule`, иначе разъедется с snapshot-зависимостью), кладёт артефакты в `deps/mathutils/{lib,include}` как и ждёт `CMakeLists.txt` проекта A.

**Важная находка по скоупу, меняющая план**: pom.xml/Maven-обвязку для DSL руками писать нельзя корректно — TeamCity резолвит саму DSL-библиотеку (`org.jetbrains.teamcity:configs-dsl-kotlin`) из репозитория **самого работающего сервера** (`{server url}/app/dsl-plugins-repository`), а не из публичного Maven Central (проверено: официальные доки TeamCity + реальный пример `pom.xml` из публичного репозитория `hhariri/teamcity-dsl` на GitHub, оба подтверждают этот паттерн). Значит, pom.xml/scaffold **не может** быть корректно захардкожен заранее — его генерирует сам TeamCity при включении Versioned Settings (Kotlin) на живом проекте. Это смещает шаг «включить versioned settings на ci-infra и свести сгенерированный scaffold с этим `settings.kts`» в тикет 08/09, против реально поднятого сервера — что и так входило в scope тикета 08 (п.5 его тела), отдельно ничего доводить не нужно.

**Честно про уверенность**: API вокруг зависимостей и веток (`SnapshotDependency`, `ArtifactDependency`, `sameChain()`, `branchSpec`, `GitVcsRoot.branch`) сверены с находками тикета 03 (официальные generated DSL docs). Остальной каркас (`Project`/`BuildType`/`GitVcsRoot`/`script`-шаг/`vcs`-триггер/блок `password { }` для авторизации) — стандартные, давно стабильные паттерны Kotlin DSL TeamCity, но отдельно **не** перепроверялись первоисточниками в этой сессии (компилятора Kotlin/JDK/Maven в этом окружении нет, а сам DSL нельзя скомпилировать без живого сервера — см. выше). Реальная проверка синтаксиса — на первом применении в тикете 09.

## SUPERSEDED (по итогам живого прогона в тикете 09)

Живьём выяснилось: `teamcity-server` не имеет исходящего доступа в интернет вообще (не только к Maven), а компиляция Kotlin DSL требует внешних артефактов (`kotlin-stdlib` и т.д.), которых нет в локальном `dsl-plugins-repository` сервера. Это не временная неполадка — DSL в принципе не может скомпилироваться в этом окружении. См. ADR `docs/adr/0003-rest-provisioning-instead-of-kotlin-dsl.md`.

`settings.kts` из этого тикета удалён из `bootstrap/ci-infra/` (мёртвый код — не используется). Дерево TeamCity-конфигурации теперь создаётся напрямую через REST API в `bootstrap.sh` (см. обновлённый ответ тикета 08) — тот же граф зависимостей (образ → B → A, branch/default-резолюция), но без DSL-компиляции. Ответ этого тикета (API-вызовы `SnapshotDependency`/`ArtifactDependency`/`branchSpec`) остаётся ценным как справочник по семантике, но сам файл больше не часть стенда.

## UN-SUPERSEDED (пользователь явно попросил вернуться к Kotlin DSL)

Вывод «SUPERSEDED» выше оказался неверным. Перепроверено вживую на актуальном дереве (`CxxCiDemo_Main`, гораздо сложнее исходного): компиляция Kotlin DSL **работает полностью офлайн**. `.teamcity/pom.xml`, который генерирует сам сервер, объявляет два Maven-репозитория — недостижимый `download.jetbrains.com` и **локальный `http://localhost:8111/app/dsl-plugins-repository`**, который сервер обслуживает сам себе из `system/caches/pluginsDslCache/.m2` (~376 jar, сгенерированы локально из установленных плагинов, включая сам компилятор Kotlin и `configs-dsl-kotlin-bundled`) — и этого достаточно, Maven ни разу не обращается к первому репозиторию. Найдена и устранена реальная преграда для первого включения: Kotlin-формат требует, чтобы id каждого объекта (включая VCS root) был префиксован id родительского проекта — иначе явная ошибка ещё до попытки компиляции.

TeamCity теперь работает в режиме `useFromVCS` (git/UI — источник правды, см. `docs/adr/0004-kotlin-dsl-versioned-settings-import-mode.md`), `bootstrap.sh` переписан под это (см. обновлённый ответ тикета 08). Актуальный `settings.kts` живёт в `bootstrap/ci-infra/.teamcity/` и в живом GitLab-репозитории `ci-infra` — он теперь единственный источник правды для дерева TeamCity-конфигурации, REST-провижининг полностью выведен из употребления для этой части.
