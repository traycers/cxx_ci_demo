# cxx_ci_demo

[🇬🇧 English](../../README.md) · 🇷🇺 Русский · [🇨🇳 中文](../zh/README.md)

_Перевод `README.md`. При изменении оригинала обновите и эту версию — см. [ADR 0006](adr/0006-trilingual-docs-mirror-tree.md)._


Демо-стенд CI на docker-compose: GitLab + TeamCity собирают C++ проекты в контейнерах. Глоссарий — в `CONTEXT.md`, архитектурные решения — в `docs/ru/adr/`. Полный план живёт на wayfinder-карте `.scratch/teamcity-cxx-ci/map.md`. Документация ведётся на английском, русском и китайском (`docs/ru/`, `docs/zh/`) — конвенция описана в [ADR 0006](adr/0006-trilingual-docs-mirror-tree.md).

## Поднятие стенда

1. `cp .env.example .env` и заполните `GITLAB_ROOT_PASSWORD` (локально может уже существовать `.env` со сгенерированным паролем из предыдущей настройки — проверьте, прежде чем перезаписывать).
2. Добавьте `127.0.0.1 gitlab.local` в `/etc/hosts` хоста (или тот hostname, что указан в `GITLAB_HOSTNAME`). Это единственный момент, который не покрывает документация GitLab — сеть compose бесплатно даёт соседним контейнерам DNS-резолвинг, но хостовой ОС нужна эта запись, чтобы резолвить тот же hostname так же, как это будут делать VCS root'ы и clone-ссылки TeamCity. См. `.scratch/teamcity-cxx-ci/research/gitlab-headless-bootstrap.md` §2.
3. `docker compose up -d`
4. **Один неизбежный ручной шаг**: откройте `http://localhost:${TEAMCITY_HTTP_PORT:-8111}` и один раз пройдите мастер первого запуска TeamCity (подтвердить data dir, принять EULA, создать аккаунт администратора). В текущем образе headless-эквивалента нет — см. `.scratch/teamcity-cxx-ci/research/teamcity-headless-bootstrap.md` §1.
5. GitLab доступен на `http://gitlab.local:${GITLAB_HTTP_PORT:-8929}` под `root` / паролем из `.env`.
6. `docker compose run --rm bootstrap` — создаёт 6 репозиториев в GitLab (`ci-infra` и пять `demo-project-*`), пушит в них посевное содержимое из `repos/<repo>/<branch>/` и направляет versioned settings TeamCity на `ci-infra`. Работает как одноразовый контейнер, подключённый напрямую к сети `cxxci` (см. ADR 0008), а не как хостовой скрипт — поэтому ничего здесь не зависит от версий `curl`/`git`/`docker` на хосте. Безопасно перезапускать.

## Диагностика проблем

- **`docker compose up` падает при монтировании `/opt/buildagent/*`** (permission denied): этот путь
  требует, чтобы демон докера мог создавать/владеть директориями внутри `/opt` — верно для
  обычной rootful-установки Docker, но не для rootless Docker или учётной записи хоста без root.
  Задайте `BUILDAGENT_DATA_DIR` в `.env` на директорию, которой вы реально владеете (например,
  `BUILDAGENT_DATA_DIR=${HOME}/.local/share/cxxci-buildagent`) и перезапустите. Эти пути обязаны
  быть именно host bind mount'ами, а не именованными volume — почему, см. комментарий у
  `teamcity-agent` в `docker-compose.yml`.
- **"Test connection"/сборка VCS root падает с `HTTP Basic: Access denied` или
  `Authentication failed`**: это проблема учётных данных, а не сети/DNS, хотя на первый взгляд
  может выглядеть похоже. Если это бьёт конкретно по одному из VCS root'ов `demo-project-*`,
  обычно значит, что контейнер `bootstrap` не дошёл до шага внедрения credentials (шаг 4 в
  `provision_teamcity()` из `scripts/bootstrap/teamcity_ops.py`) — он выполняется только после
  того, как появился `CxxCiDemo_Main_DemoProjectA`, то есть только после того, как versioned
  settings успешно импортировали дерево DSL. Перезапустите `docker compose run --rm bootstrap`;
  каждый его REST-вызов проверяет статус ответа и громко падает с реальным HTTP-кодом и телом
  ответа вместо тихого продолжения (более ранняя версия так не делала, и прогон на чистой машине
  показал ровно это: versioned settings тихо не включились, поэтому дерево — и внедрение
  credentials — так и не произошли, а запутывающий "Access denied" шагом позже был реальным, но
  отложенным симптомом).
- **`gitlab` недоступен из контейнера `bootstrap`** (connection refused/timeout, а не ошибка
  аутентификации) — проверьте, что контейнеры реально в сети `cxxci` (`docker compose ps`). В
  отличие от шага с браузером выше, контейнер `bootstrap` обращается к `gitlab`/`teamcity-server`
  напрямую по их именам compose-сервисов через сеть `cxxci` — он никогда не идёт через
  `gitlab.local`, опубликованный хостовый порт или хостовый прокси, так что хостовые сетевые
  особенности (hairpin NAT, локальный прокси, перехватывающий `localhost`), влияющие на браузер и
  хостовой `git`, на него не распространяются. См. ADR 0008.

## Добавление нового релиза

Проект `cxx_ci_demo` в TeamCity разбит на одну директорию на релиз внутри
`repos/ci-infra/main/.teamcity/cxx_ci_demo/` (сейчас только релиз с именем `main` — не путать
с внешним `repos/ci-infra/main/`: это заготовленная git-ветка, см. ADR 0007/0008). Пошаговую
процедуру и конвенцию именования веток `<config_name>`/`<config_name>-*` см. в
`docs/ru/adding-a-release.md`.
