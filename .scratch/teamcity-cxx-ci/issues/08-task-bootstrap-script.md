Type: task
Status: resolved
Blocked by: 02, 05, 06, 07

## Question

Создать директорию `bootstrap/` в рабочей директории (`ci_cxx`) с поддиректорией на каждый репозиторий (`bootstrap/ci-infra/`, `bootstrap/project_a/`, `bootstrap/project_b/`), собрав в них содержимое из тикетов 05 (Dockerfile), 07 (Kotlin DSL) и 06 (demo-проекты) — по одной директории на репозиторий, как решено в диалоге charting.

Плюс `bootstrap.sh` (или аналог), который:
1. Дожидается готовности GitLab (см. healthcheck из тикета 04).
2. Получает/использует PAT, полученный headless-механизмом из тикета 02.
3. Через GitLab API создаёт три репозитория (`ci-infra`, `project_a`, `project_b`).
4. Пушит в каждый содержимое соответствующей `bootstrap/<repo>/` директории.
5. Подключает `ci-infra` к TeamCity как проект с versioned settings (Kotlin DSL) — используя токен TeamCity из тикета 01.

Готово, когда однократный запуск `bootstrap.sh` после `docker compose up` полностью наполняет GitLab и TeamCity без ручных действий (за вычетом шагов, явно помеченных как неизбежно ручные в тикетах 01/02).

## Answer

`bootstrap.sh` в корне репозитория. Идемпотентен (создание репо безопасно перезапускать — проверяет "already been taken").

1. **Ждёт GitLab** — poll `/-/readiness` (не health/health_check, см. тикет 02) с щедрым таймаутом (до 15 мин).
2. **PAT для root headless** — `docker compose exec gitlab gitlab-rails runner`, 20-символьный токен, scopes `['api', 'write_repository']` (добавил `write_repository` к рекомендации тикета 02 — `api` один может не хватить конкретно для git push, `write_repository` closes that gap explicitly).
3. **Создаёт 3 репозитория** через `POST /api/v4/projects`.
4. **Пушит содержимое** каждого `bootstrap/<repo>/` — **из временной копии** (`mktemp -d` + `cp -a`), чтобы не вкладывать `.git` внутрь `bootstrap/<repo>/` этого репозитория (та же ловушка, что уже словили с `.claude/worktrees` при резолюции тикета 01). **Проверено вживую**: `git init`+`add`+`commit`+`push` симулирован на локальном bare-репо вместо GitLab — содержимое `bootstrap/ci-infra` (Dockerfile + `.teamcity/settings.kts`) запушилось корректно на ветку `main`.
5. **TeamCity**: берёт Super User token из логов сервера (тикет 01); если его ещё нет — значит ручной wizard-шаг ещё не пройден, скрипт прямо это говорит и просит перезапустить после. Создаёт VCS root для `ci-infra` через REST (`POST /app/rest/vcs-roots`, `vcsName: jetbrains.git` — форма подтверждена по официальным REST-доками TeamCity). **Включение Kotlin versioned settings на Root project по REST — best-effort, НЕ подтверждено первоисточником в этой сессии** (точный endpoint не нашёлся в открытом поиске за разумное время): если REST-вызов не сделан/не сработает, скрипт печатает точные шаги для ручного включения через Administration UI (один раз) вместо того, чтобы гадать дальше или падать молча.

**Реальный прогон против живого GitLab/TeamCity не проводился** (это тикет 09) — только: (a) синтаксис (`bash -n`), (b) git init/commit/push логика — против локального bare-репозитория вместо настоящего GitLab.

### Поправка по итогам живого прогона (тикет 09)

`wait_for_gitlab` изначально опрашивал `${GITLAB_URL}/-/readiness` через published-порт — на реальном стенде пользователя это стабильно возвращало 404, хотя GitLab был полностью готов (`{"status":"ok"}` при проверке изнутри контейнера). Причина: GitLab отдаёт 404 (не 403 — намеренная обфускация) для IP не из `monitoring_whitelist`, а docker на этой машине заворачивает host→published-port трафик через реальный IP хоста в LAN (hairpin NAT), а не через bridge gateway из настроенной подсети `172.28.0.0/16` — предсказать этот IP заранее было невозможно. Исправлено: `wait_for_gitlab` теперь опрашивает `docker compose exec gitlab curl 127.0.0.1:${GITLAB_HTTP_PORT}/-/readiness` — изнутри контейнера, в обход вопроса whitelist'а целиком. `docker-compose.yml` не трогал: `monitoring_whitelist` остаётся корректным для трафика от настоящих соседних контейнеров (например, будущих обращений agent'а), проблема была именно в host-хайрпине.

### Kotlin DSL заменён на REST-провижининг (по итогам живого прогона)

Пункт 5 (versioned settings) полностью переписан. Живьём выяснилось: `teamcity-server` не имеет исходящего доступа в интернет вообще (`curl 1.1.1.1` изнутри контейнера — timeout), а Kotlin DSL требует внешних зависимостей (`kotlin-stdlib` и т.д.), которых нет в локальном `dsl-plugins-repository` сервера — компиляция DSL в принципе невозможна в этом окружении, не временная проблема. См. ADR `docs/adr/0003-rest-provisioning-instead-of-kotlin-dsl.md` и SUPERSEDED-пометку в тикете 07.

Также по пути нашлось и исправлено:
- Изначальный VCS root для `ci-infra` использовал **TeamCity Super User token** как GitLab-пароль (перепутал с GitLab PAT) — чинилось через `authMethod: PASSWORD` + `username`/`secure:password` с правильным (GitLab) токеном.
- GitLab по умолчанию защищает `main` от force-push — `push_repo_content` больше не форсит, а сначала проверяет `git ls-remote`, пушит только в пустой репозиторий.
- `docker.sock`-запросы `docker run` от агента резолвятся на **хосте**, поэтому `/opt/buildagent/*` обязаны быть host bind mount'ами, не именованными volume'ами (см. `docker-compose.yml`) — иначе `docker run -v checkoutDir:/src` монтирует пустую директорию.

**Функция `provision_teamcity` (замена `wire_teamcity_versioned_settings`)** создаёт весь граф через REST, идемпотентно (проверка существования перед созданием больших объектов — project/VCS roots/build types; под-ресурсы новых build type'ов создаются один раз при первом создании самого build type):
- Project `CxxCiDemo` под `_Root` (Root project не может напрямую содержать build configuration'ы — REST прямо об этом сообщает).
- 3 VCS root'а (ci-infra, project_a, project_b), у всех `branchSpec: +:refs/heads/*` с самого создания (не PUT постфактum — VCS root **instance** не подхватывает обновление свойств уже использованного VCS root'а без пересоздания, это отдельная находка).
- `BuildImage` (VCS root + `docker build` шаг + VCS-триггер), `ProjectB` (VCS root + param + `docker run cmake/ninja/install` шаг + snapshot-зависимость на BuildImage + VCS-триггер + `buildDependencyTrigger` на BuildImage — это и есть механизм «обновил образ → пересобралось всё зависящее» — + `artifactRules` через `PUT .../settings/artifactRules`), `ProjectA` (аналогично + snapshot на BuildImage И ProjectB + artifact-зависимость на ProjectB с `revisionName: sameChainOrLastFinished` — REST-эквивалент DSL `buildRule = sameChain()` — + `buildDependencyTrigger` на ProjectB).
- Авторизация агента (`PUT .../agents/id:1/authorizedInfo`).

**Проверено вживую, полностью, несколько раз**: `BuildImage` → `project_b` → `project_a` собираются каскадом (finish-build триггеры), `project_a` реально линкуется с артефактом `project_b` (`2 + 3 = 5` в логе сборки, не просто триггер). Идемпотентность подтверждена повторным прогоном `bootstrap.sh` на уже настроенном стенде — второй прогон ничего не пересоздаёт.

**Открытый вопрос, не решён в этой сессии**: ручной REST-триггер сборки на СВЕЖЕСОЗДАННОЙ ветке (`POST /app/rest/buildQueue` с `branchName` сразу после `git push` новой ветки) стабильно падает с проблемой `invalid_branch_name`, даже когда `branchSpec` корректен и ветка подтверждена присутствующей и в `git ls-remote`, и в `/app/rest/buildTypes/.../branches`. Причина не найдена (не devErrorSpec, не тайминг — ждали по 2+ минуты реального времени, не помогло). **Это не блокирует основной вывод**: dependency-резолюция «та же ветка, иначе default» доказана — сборка `project_a` на ветке, которой нет ни у образа, ни у `project_b`, корректно переиспользовала их существующие default-branch сборки вместо ошибки. Проблема именно в триггере СВЕЖЕЙ ветки через REST для сборки, у которой это единственная (не-dependency) VCS root — возможно, специфика окружения (нет background VCS-поллинга — обнаружение веток происходит только в момент постановки в очередь, но и тогда не всегда подхватывается с первого раза). Стоит перепроверить через UI или с более развитым retry в реальном использовании.

### REST-провижининг заменён на Kotlin DSL import mode (пользователь явно попросил вернуться к Kotlin DSL)

`provision_teamcity` переписана третий раз. См. `docs/adr/0004-kotlin-dsl-versioned-settings-import-mode.md` и UN-SUPERSEDED-раздел тикета 07 для полной картины: DSL-компиляция оказалась рабочей полностью офлайн (сервер обслуживает нужные артефакты сам себе через `http://localhost:8111/app/dsl-plugins-repository`), вывод ADR 0003 был ошибочным.

Новая роль `bootstrap.sh` в TeamCity-части — только то, что DSL в принципе не может сделать сама:
1. Создать VCS root `CiInfraVersionedSettingsVcs` через REST (как и раньше) — без него versioned settings нечем подключиться к `ci-infra`.
2. `PUT /app/rest/projects/id:_Root/versionedSettings/config` — `format: kotlin`, `buildSettingsMode: useFromVCS` (git/UI — источник правды, не «сервер экспортирует в git», как было в XML/`alwaysUseCurrent`-режиме). При конфликте (VCS root уже содержит другое дерево — например, повторная инициализация без сноса datadir) — retry с `importDecision: importFromVCS`.
3. Дождаться применения (`GET .../versionedSettings/status`, до ~90с).
4. Внедрить реальный GitLab-токен напрямую в `secure:password` VCS root'ов `CxxCiDemo_Main_ProjectA`/`B` — **не полагаясь** на `%gitlab_credentials_password%`-ссылку в `settings.kts`: выяснилось, что TeamCity резолвит такую ссылку в статичный `credentialsJSON:<uuid>` в момент применения DSL и **не переразрешает её заново** на последующих синхронизациях. Ссылка в DSL остаётся как документация/дефолт, но живой источник правды для секрета — этот REST-шаг, каждый прогон.
5. Авторизация агента — без изменений.

Всё остальное дерево (`CxxCiDemo_Main`: 11 параметров, шаблон `base_build`, `BuildCImage`/`ProjectA`/`ProjectB`/`Result`) больше не создаётся через REST — оно целиком описано в `bootstrap/ci-infra/.teamcity/settings.kts`, который живёт и в этом репозитории, и в живом `ci-infra` на GitLab (git — источник правды). `docs/build.sh` удалён — его содержимое теперь только внутри `settings.kts`.

**Проверено вживую, полностью**: переписанный `bootstrap.sh` прогнан против уже настроенного стенда — чистый идемпотентный no-op на GitLab-части, versioned settings подтвердили «repository is up-to-date», credential-инъекция отработала. Отдельно прогнана реальная сборка `project_a` (build 406) с credential'ом, только что внедрённым этим прогоном — `SUCCESS`, тесты прошли. Round-trip git↔TeamCity в обе стороны подтверждён отдельно (правка параметра прямо в git применилась на сервере; REST-правка параметра на сервере закоммитилась обратно в `ci-infra`).
