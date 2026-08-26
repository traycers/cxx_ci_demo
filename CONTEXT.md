# CI CXX

Docker-compose demo CI stand: GitLab (VCS) + TeamCity (build server, single agent) build C++ projects inside Docker containers, with a root Docker-image build as the base dependency for the whole build tree.

## Language

**ci_cxx**:
Репозиторий на хосте, где живут docker-compose и bootstrap-обвязка для подъёма стенда (GitLab + TeamCity). Сам по себе не содержит C++ кода и не является CI-конфигурацией TeamCity.
_Avoid_: проект, монорепо

**ci-infra**:
Центральный репозиторий внутри GitLab, хранящий Kotlin DSL (versioned settings) TeamCity для всего дерева сборок — включая build configuration корневой сборки образа — и Dockerfile этого образа.
_Avoid_: settings repo, teamcity repo

**Корневая сборка образа**:
Build configuration в TeamCity, которая собирает Docker-образ для сборки C++ (по Dockerfile из ci-infra) и является самой корневой зависимостью всего дерева сборок; её пересборка триггерит пересборку всего, что от неё зависит.
_Avoid_: base build, image job

**Тег образа**:
Конфигурационная переменная TeamCity (`%build_image_cxx%`), хранящая тег Docker-образа, который используют нижестоящие сборки C++ проектов для запуска контейнера сборки.

**Bootstrap**:
Разовый automated-скрипт (и директория `bootstrap/` с поддиректорией на каждый репозиторий), который после `docker compose up` создаёт репозитории в GitLab через API и наполняет их начальным содержимым (DSL, demo-проекты).

**Demo-проект**:
Минимальный skeleton C++ проект (CMake), созданный в рамках этой карты для сквозной проверки пайплайна. Один из двух demo-проектов зависит от другого — для проверки резолюции зависимостей по ветке.

**Snapshot-зависимость**:
Механизм TeamCity, гарантирующий, что зависимая сборка триггерится и берётся с той же ветки, что и триггерящая сборка, а если такой ветки нет в VCS root'е зависимости — с default branch.
_Avoid_: build trigger dependency

**Artifact-зависимость**:
Механизм TeamCity, передающий собранные бинарники/заголовки одного C++ проекта в другой для линковки, без пересборки с нуля.

**Релиз** (branch family):
Один поддерево `cxx_ci_demo/<config_name>/` в `ci-infra` — своя TeamCity-подпроект, свои VCS root'ы, свой набор build configuration'ов, но общие GitLab-репозитории demo-проектов (`demo-project-a`/`demo-project-b`). Релизы различаются исключительно тем, на какую ветку смотрит каждый VCS root (`branch_default`/`branch_spec`). См. `docs/adding-a-release.md`.
_Avoid_: конфигурация сборки (слишком расплывчато — путается с build configuration отдельного проекта внутри релиза)

**config_name**:
Имя релиза, используемое и как имя его директории (`cxx_ci_demo/<config_name>/`), и как базовое имя ветки в demo-проектах (`refs/heads/<config_name>`). Ветки-производные этого релиза именуются `<config_name>-*` (например, релиз `release_2_0` → ветки `release_2_0`, `release_2_0-hotfix-1`).
