Type: research
Status: resolved
Blocked by: (none)

## Question

Как поднять GitLab (образ `gitlab/gitlab-ce` или аналог) в docker-compose headless и сделать его одинаково доступным и с хоста (браузер разработчика), и изнутри docker-сети (TeamCity server/agent делают git clone по HTTP)?

Нужно выяснить и задокументировать:
- Установка root-пароля через переменную окружения (`GITLAB_ROOT_PASSWORD` / `GITLAB_OMNIBUS_CONFIG`) без ручного шага смены пароля при первом входе.
- Конфигурация `external_url`/hostname так, чтобы он резолвился и с хоста, и из других контейнеров compose-сети (например, через `extra_hosts`/фиксированное service-имя в общей docker-сети, либо через проброс порта на хост и единый hostname для обоих случаев) — это нужно для корректных clone-URL в VCS root'ах TeamCity.
- Как headless сгенерировать Personal Access Token для root/service-пользователя (через `gitlab-rails runner` или GitLab API) — токен нужен и для bootstrap-скрипта (тикет 08, создание репозиториев), и как креды для VCS root'ов TeamCity (HTTP + PAT, см. решения в `map.md`).
- Ресурсные требования (RAM/время старта) и health-check, на который можно ориентироваться, чтобы bootstrap-скрипт не стартовал раньше, чем GitLab реально готов принимать API-запросы.

Результат — что должно попасть в docker-compose.yml (сервис `gitlab`, env vars, network, healthcheck) для тикета 04, и какой механизм получения PAT использовать в тикете 08.

## Answer

Полные находки: `.scratch/teamcity-cxx-ci/research/gitlab-headless-bootstrap.md`.

- **Root-пароль headless**: для Docker — через `GITLAB_OMNIBUS_CONFIG` с `gitlab_rails['initial_root_password'] = "..."` (не через голый `GITLAB_ROOT_PASSWORD` — тот подтверждён только для apt-установки, не для Docker).
- **external_url/hostname**: один и тот же фиксированный hostname используется и как `hostname:` контейнера, и в `external_url` — и как порт (NGINX слушает именно тот порт, что указан в `external_url`, не обязательно 80!), с 1:1 проброс порта на хост (`8929:8929`, не `8929:80`). Резолвинг самого hostname с хоста (браузер) и из соседних контейнеров одновременно — GitLab это не документирует; нужен `extra_hosts`/фиксированный локальный домен в `/etc/hosts` — стандартная docker-механика, не gitlab-специфика.
- **Headless PAT**: `docker exec gitlab gitlab-rails runner "..."`, создаёт токен для `root` программно. В официальном примере скоупы read-only — для bootstrap-скрипта (создание репо) нужен `scopes: ['api']`, а не пример из доков как есть. Токен должен быть ровно 20 символов.
- **Создание репозитория**: `POST /api/v4/projects` с заголовком `PRIVATE-TOKEN`, минимум — `name` или `path`.
- **Readiness-check для bootstrap-скрипта**: `GET /-/readiness` (не `/-/health`, не `/health_check` — доки прямо предупреждают не использовать последний для автоматизации). ⚠️ **Готча**: по умолчанию monitoring-эндпоинты разрешены только с `127.0.0.0/8` — если bootstrap-скрипт стучится из другого контейнера той же compose-сети, нужно расширить `gitlab_rails['monitoring_whitelist']` до подсети compose-сети, либо опрашивать изнутри контейнера `gitlab` через `docker exec`.
- **Ресурсы**: продакшен-рекомендация 8 vCPU/16GB, но документированный "memory-constrained" пол — 1 CPU/64-bit + ~2-3GB RAM, с оговоркой о деградации — достаточно для demo. Точное время старта нигде не задокументировано — закладывать щедрый таймаут эмпирически.
- Не задокументировано нигде (честно, не выдумано): точная семантика persistence `initial_root_password` при перезапуске уже проинициализированного volume; client-side DNS резолвинг hostname на два фронта; время старта контейнера.
