Type: research
Status: resolved
Blocked by: (none)

## Question

Как поднять TeamCity server (Professional edition) в docker-compose полностью headless — без ручного прохождения мастера первого запуска через браузер?

Нужно выяснить и задокументировать:
- Принятие EULA и создание первого super-user/admin через переменные окружения или файл конфигурации официального образа `jetbrains/teamcity-server` (в частности переменную `TEAMCITY_SERVER_FIRST_START` и связанные с ней properties-файлы, если применимо к текущей стабильной версии образа).
- Как headless сгенерировать/получить токен доступа TeamCity REST API для последующей автоматизации (bootstrap-скрипт, DSL-приложение) — без захода в UI.
- Как зарегистрировать `jetbrains/teamcity-agent` на сервере автоматически (переменные `SERVER_URL` и т.п.), включая монтирование `docker.sock` агенту для последующего запуска сборочных контейнеров.
- Любые шаги, которые объективно нельзя автоматизировать headless в текущей версии образов — зафиксировать явно как неизбежный ручной шаг (для последующего HITL task/wizard), а не придумывать хрупкий обход.

Результат — что должно попасть в docker-compose.yml (сервисы `teamcity-server`/`teamcity-agent`, их env vars, volumes) для тикета 04.

## Answer

Полные находки: `.scratch/teamcity-cxx-ci/research/teamcity-headless-bootstrap.md` (образ `jetbrains/teamcity-server:2026.1.3` / `jetbrains/teamcity-agent:2026.1.3`).

- **Единственный неустранимый ручной шаг**: первый запуск сервера требует один раз пройти браузерный wizard (подтверждение data dir → выбор БД → принятие EULA → создание admin-аккаунта). Никакой env-переменной/файла для этого не существует ни в документации, ни в стартовых скриптах образа. → нужен HITL-чеклист (тикет 04/09), а не попытка автоматизировать.
- Всё остальное — headless:
  - REST-токен для автоматизации: TeamCity печатает свежий Super User token в лог/stdout при каждом старте сервера; годится как HTTP Basic Auth без логина в UI (`docker compose logs teamcity-server | grep "Super user authentication token:"`).
  - Регистрация агента — headless через `SERVER_URL`/`AGENT_NAME`/`AGENT_TOKEN`.
  - Авторизация агента формально описана как ручная, но имеет REST-обход: `PUT /app/rest/agents/<id>/authorizedInfo`.
  - Доступ к `docker.sock`: монтировать `/var/run/docker.sock`, агент запускать как `-u 0` (root) — так документирует сам JetBrains, чтобы не возиться с GID docker-группы.
- В файле — готовая структура `docker-compose.yml` для обоих сервисов (образы, env vars, volumes, порт 8111) и полный список первоисточников.
