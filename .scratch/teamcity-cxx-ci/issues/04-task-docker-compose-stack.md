Type: task
Status: resolved
Blocked by: 01, 02

## Question

Написать `docker-compose.yml` (в корне `ci_cxx`) со всем стендом: сервисы `gitlab`, `teamcity-server`, `teamcity-agent`, общая docker-сеть, volumes для персистентности данных GitLab/TeamCity, монтирование `docker.sock` агенту, все headless-переменные окружения из результатов тикетов 01 (TeamCity) и 02 (GitLab).

Также: файл(ы) с переменными окружения (`.env`, в `.gitignore`) для паролей/токенов, которые не должны попасть в git — per Notes карты, минимизировать ручные шаги, всё, что можно — в compose/`.env`.

Готово, когда `docker compose up` поднимает весь стенд без единого клика в UI (кроме шагов, которые тикеты 01/02 явно пометили как неизбежно ручные — для них здесь же оставить TODO-комментарий со ссылкой на этот случай, не пытаться героически обойти).

## Answer

Созданы: `docker-compose.yml`, `.env.example` (коммитится), `.env` (сгенерирован локально со случайным паролем, добавлен в `.gitignore`), `README.md` с порядком запуска.

- Сервисы: `gitlab` (headless root-пароль через `GITLAB_OMNIBUS_CONFIG`/`initial_root_password`, фиксированный hostname+alias `gitlab.local`, порт 1:1 host:container как того требует NGINX per research, `monitoring_whitelist` расширен на подсеть сети `cxxci` — без этого `/-/readiness` из тикета 08 будет заблокирован), `teamcity-server` (`jetbrains/teamcity-server:2026.1.3`), `teamcity-agent` (`jetbrains/teamcity-agent:2026.1.3`, `user: "0"`, смонтирован `/var/run/docker.sock` + `/opt/buildagent/*` volumes, `SERVER_URL` на `teamcity-server`).
- Сеть `cxxci` с явным subnet (`172.28.0.0/16`), чтобы `monitoring_whitelist` можно было указать точно, а не широким диапазоном.
- Единственный оставшийся ручной шаг задокументирован TODO-комментарием в compose и явно расписан в `README.md`: первый браузерный wizard TeamCity (EULA + admin), плюс отдельно — правка `/etc/hosts` на хосте для резолвинга `gitlab.local` (это не то же самое, что «клик в UI», но такой же неавтоматизируемый headless шаг — GitLab это не документирует, см. research тикета 02 §2); намеренно не трогал `/etc/hosts` хоста сам — системный файл вне репозитория.
- Проверено: `docker compose config -q` — файл синтаксически валиден.
- **Не выполнено в рамках этого тикета**: реальный `docker compose up` всего стенда (скачивание образов ~несколько GB, минуты на старт GitLab). По дизайну карты сквозная проверка — задача тикета 09, после того как появится и тикет 08 (bootstrap), чтобы поднимать и проверять всё разом, а не частями.
