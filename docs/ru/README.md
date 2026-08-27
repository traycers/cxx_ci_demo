# cxx_ci_demo

[🇬🇧 English](../../README.md) · 🇷🇺 Русский

_Перевод `README.md`. При изменении оригинала обновите и эту версию — см. [ADR 0005](adr/0005-bilingual-docs-mirror-tree.md)._


Демо-стенд CI на docker-compose: GitLab + TeamCity собирают C++ проекты в контейнерах. Глоссарий — в `CONTEXT.md`, архитектурные решения — в `docs/ru/adr/`. Полный план живёт на wayfinder-карте `.scratch/teamcity-cxx-ci/map.md`. Документация ведётся на английском и русском (`docs/ru/`) — конвенция описана в [ADR 0005](adr/0005-bilingual-docs-mirror-tree.md).

## Поднятие стенда

1. `cp .env.example .env` и заполните `GITLAB_ROOT_PASSWORD` (локально может уже существовать `.env` со сгенерированным паролем из предыдущей настройки — проверьте, прежде чем перезаписывать).
2. Добавьте `127.0.0.1 gitlab.local` в `/etc/hosts` хоста (или тот hostname, что указан в `GITLAB_HOSTNAME`). Это единственный момент, который не покрывает документация GitLab — сеть compose бесплатно даёт соседним контейнерам DNS-резолвинг, но хостовой ОС нужна эта запись, чтобы резолвить тот же hostname так же, как это будут делать VCS root'ы и clone-ссылки TeamCity. См. `.scratch/teamcity-cxx-ci/research/gitlab-headless-bootstrap.md` §2.
3. `docker compose up -d`
4. **Один неизбежный ручной шаг**: откройте `http://localhost:${TEAMCITY_HTTP_PORT:-8111}` и один раз пройдите мастер первого запуска TeamCity (подтвердить data dir, принять EULA, создать аккаунт администратора). В текущем образе headless-эквивалента нет — см. `.scratch/teamcity-cxx-ci/research/teamcity-headless-bootstrap.md` §1.
5. Возьмите Super User token TeamCity для скриптового доступа:
   `docker compose logs teamcity-server | grep "Super user authentication token:"`
6. GitLab доступен на `http://gitlab.local:${GITLAB_HTTP_PORT:-8929}` под `root` / паролем из `.env`.

Всё, что дальше (создание репозиториев, Kotlin DSL, demo-проекты), автоматизирует `bootstrap.sh` — см. тикет 08 на карте.

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
  может выглядеть похоже — `git` реально достучался до `gitlab.local` и получил настоящий ответ
  от GitLab, ему просто не понравился пароль. Если это бьёт конкретно по VCS root'ам
  `demo-project-a`/`demo-project-b`, обычно значит, что `bootstrap.sh` не дошёл до шага внедрения
  credentials (шаг 4 в `provision_teamcity`) — он выполняется только после того, как появился
  `CxxCiDemo_Main_DemoProjectA`, то есть только после того, как versioned settings успешно
  импортировали дерево DSL. Перезапустите `bootstrap.sh`; теперь каждый его REST-вызов проверяет
  статус ответа и громко падает с реальным HTTP-кодом и телом ответа вместо тихого продолжения
  (более ранняя версия так не делала, и прогон на чистой машине показал ровно это: versioned
  settings тихо не включились, поэтому дерево — и внедрение credentials — так и не произошли, а
  запутывающий "Access denied" шагом позже был реальным, но отложенным симптомом).
- **`gitlab.local` действительно недоступен изнутри контейнера** (connection refused/timeout,
  а не ошибка аутентификации) — это другая проблема: проверьте, что контейнеры реально в сети
  `cxxci` (`docker compose ps`) и что ничто на хосте не перехватывает трафик на опубликованных
  портах (в этом репозитории именно так вёл себя локальный прокси во время разработки — см.
  комментарии в `bootstrap.sh` о том, почему его собственные REST-вызовы идут через одноразовый
  контейнер в сети `cxxci`, а не через опубликованные хостовые порты).

## Добавление нового релиза

Проект `cxx_ci_demo` в TeamCity разбит на одну директорию на релиз внутри
`bootstrap/ci-infra/.teamcity/cxx_ci_demo/` (сейчас только `main/`). Пошаговую процедуру и
конвенцию именования веток `<config_name>`/`<config_name>-*` см. в `docs/ru/adding-a-release.md`.
