Type: task
Status: resolved
Blocked by: (none)

## Question

Создать содержимое двух минимальных demo C++ проектов (попадут в `bootstrap/demo-project-a/` и `bootstrap/demo-project-b/` — см. тикет 08):

- `demo-project-b` — простая библиотека (например статическая lib с одной функцией), CMake-проект, собирается образом из тикета 05.
- `demo-project-a` — исполняемый файл, который использует библиотеку из `demo-project-b` (заголовок + линковка) через artifact-зависимость TeamCity, а не через copy/submodule.

Готово, когда оба проекта собираются локально в контейнере из образа тикета 05 (`docker run` + `cmake`/`make` вручную, чтобы проверить сборку до появления TeamCity DSL), и понятен путь к артефактам `demo-project-b`, которые должна забрать artifact-зависимость (нужно для тикета 07).

## Answer

- `bootstrap/demo-project-b/` — статическая либа `mathutils` (`mathutils::add`), `CMakeLists.txt` с `install(TARGETS ... ARCHIVE DESTINATION lib)` + `install(FILES include/mathutils.h DESTINATION include)`.
- `bootstrap/demo-project-a/` — исполняемый `app_a`, линкуется на `mathutils` как `IMPORTED` static lib, ожидая её по пути `deps/mathutils/{include,lib}` относительно своего checkout root. Этот путь **не в git** (и не submodule/copy) — nammеренно, его должна заполнить TeamCity artifact-зависимость на этапе сборки (тикет 07).
- **Проверено вживую** в контейнере из тикета 05: `cmake --install` проекта B кладёт `lib/libmathutils.a` + `include/mathutils.h` в staging-директорию → скопировал её в `demo-project-a/deps/mathutils/` (имитация artifact-зависимости) → собрал и запустил `app_a` → `2 + 3 = 5`. Билд-директории/`deps/` подчищены, в `.gitignore` добавлены паттерны, чтобы такие локальные smoke-тесты не мусорили репозиторий.
- **Для тикета 07**: артефакты B, которые нужно публиковать (artifactRules) — содержимое staging-директории после `cmake --install --prefix <dist>`, т.е. `<dist>/lib/libmathutils.a` и `<dist>/include/mathutils.h`; artifact-зависимость A должна класть их в `deps/mathutils/lib` и `deps/mathutils/include` соответственно (не в один плоский каталог — CMakeLists.txt проекта A уже жёстко ждёт именно такую раскладку).
