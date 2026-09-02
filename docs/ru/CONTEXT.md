[🇬🇧 English](../../CONTEXT.md) · 🇷🇺 Русский · [🇨🇳 中文](../zh/CONTEXT.md)

# CI CXX

Демо-стенд CI на docker-compose: GitLab (VCS) + TeamCity (build server, один агент) собирают C++ проекты внутри Docker-контейнеров, с корневой сборкой Docker-образа как базовой зависимостью для всего дерева сборок.

## Термины

**ci_cxx**:
Репозиторий на хосте, где живут docker-compose и bootstrap-обвязка для подъёма стенда (GitLab + TeamCity). Сам по себе не содержит C++ кода и не является CI-конфигурацией TeamCity.
_Избегать_: проект, монорепо

**ci-infra**:
Центральный репозиторий внутри GitLab, хранящий Kotlin DSL (versioned settings) TeamCity для всего дерева сборок — включая build configuration корневой сборки образа — и Dockerfile этого образа.
_Избегать_: settings repo, teamcity repo

**Корневая сборка образа**:
Build configuration в TeamCity, которая собирает Docker-образ для сборки C++ (по Dockerfile из ci-infra) и является самой корневой зависимостью всего дерева сборок; её пересборка триггерит пересборку всего, что от неё зависит.
_Избегать_: base build, image job

**Тег образа**:
Конфигурационная переменная TeamCity (`%build_image_cxx%`), хранящая тег Docker-образа, который используют нижестоящие сборки C++ проектов для запуска контейнера сборки.

**Bootstrap**:
Разовый provisioning-контейнер (`scripts/bootstrap/`, запускается через `docker compose run --rm bootstrap` — см. ADR 0008), который после `docker compose up` создаёт репозитории в GitLab через API и наполняет ветки каждого репозитория начальным содержимым (DSL, demo-проекты) из `repos/<repo>/<branch>/` (по одной поддиректории на репозиторий, а внутри — по одной поддиректории на каждую заранее заготовленную ветку, см. ADR 0007).

**Demo-проект**:
Минимальный skeleton C++ проект (CMake), созданный в рамках этой карты для сквозной проверки пайплайна. Их пять (`a`–`e`): `a` через `c` тянется к `d`, образуя цепочку, которая проверяет многоуровневую резолюцию `install_package_config` (см. ADR 0009), `b` и `e` самостоятельны, причём `e` — намеренно самодостаточен (не зависит ни от одного другого demo-проекта).

**Snapshot-зависимость**:
Механизм TeamCity, гарантирующий, что зависимая сборка триггерится и берётся с той же ветки, что и триггерящая сборка, а если такой ветки нет в VCS root'е зависимости — с default branch.
_Избегать_: build trigger dependency

**Artifact-зависимость**:
Механизм TeamCity, передающий собранные бинарники/заголовки одного C++ проекта в другой для линковки, без пересборки с нуля.

**Track** (branch family):
Одно поддерево `cxx_ci_demo/<config_name>/` в `ci-infra` — свой TeamCity-подпроект, свои VCS root'ы, свой набор build configuration'ов, но общие GitLab-репозитории (от `project_a` до `project_e`). Track'и различаются исключительно тем, на какую ветку смотрит каждый VCS root (`branch_default`/`branch_spec`). См. `docs/ru/adding-a-track.md`.
_Избегать_: голого `release` как обозначения самого понятия — это слово теперь называет package variant (см. ниже). Конкретный track всё же может быть *назван* `release_1`, `release_2` и т.д. (см. [ADR 0012](adr/0012-release-instance-names-restored.md)) — их различает позиция в пути `track/repo/variant`, а не само слово. Также избегать: конфигурация сборки (слишком расплывчато — путается с build configuration отдельного проекта внутри track'а)

**config_name**:
Имя track'а, используемое и как имя его директории (`cxx_ci_demo/<config_name>/`), и как базовое имя ветки в demo-проектах (`refs/heads/<config_name>`). Ветки-производные этого track'а именуются `<config_name>-*` (например, track `track_2_0` → ветки `track_2_0`, `track_2_0-hotfix-1`).

**Package variant** (реализовано для track'а `main`; для `release_1`/`release_2`/`release_3` пока планируется — см. [`docs/ru/roadmap.md`](docs/ru/roadmap.md) и [ADR 0013](adr/0013-debug-release-subprojects-and-track-scoped-image-naming.md)):
Квалификатор build-type — `release` или `debug` — различающий переиспользуемые build-артефакты demo-проекта, независимо от того, потребляются они как скачиваемый архив (roadmap Phase 1) или, позже, как ссылка пакетного менеджера (roadmap Phase 2, пока не реализовано). `release` соответствует `CMAKE_BUILD_TYPE=RelWithDebInfo` (см. `BaseBuild.kt` — это не то же самое, что собственное значение CMake `CMAKE_BUILD_TYPE=Release`, несмотря на совпадающее имя), `debug` — `CMAKE_BUILD_TYPE=Debug`. На `main` каждый вариант — полноценный дочерний TeamCity-subproject (`Main_Debug`/`Main_Release`), а не параметр — почему именно так, см. ADR 0013.

**Dev container image** (реализовано для track'а `main`; для `release_1`/`release_2`/`release_3` пока планируется — см. [`docs/ru/roadmap.md`](docs/ru/roadmap.md)):
Docker-образ, собираемый `FROM` образа корневой сборки образа, на который demo-проект напрямую ссылается в своём `devcontainer.json` — чтобы разработчикам не приходилось собирать его самим. На `main` собирается `Main_BuildDevImage` как `cxxci-main-dev:latest` и берётся прямо с общего Docker daemon хоста (см. ADR 0002) — registry для этого demo-стенда не нужен, поскольку разработчик и агент TeamCity делят один и тот же daemon. Настоящий registry остаётся задокументированной опцией на будущее (`roadmap.md`), а не текущим пробелом — он понадобился бы, только если образам вообще потребуется попадать на машину вне этого общего daemon'а.
