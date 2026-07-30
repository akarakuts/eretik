# AGENTS.md — Eretik

## Обзор проекта

**Eretik** — порт классической игры **Heretic** (Raven Software, 1994) на Android.
Собран на базе [Chocolate Heretic](https://github.com/chocolate-doom/chocolate-doom)
(максимально точного source port оригинального движка) и SDL2.

Репозиторий — это по сути тонкая Android-обвязка: свой код — один Gradle-модуль `app`
с Java-активностью и тач-контролями, а весь движок собирается из исходников
chocolate-doom через ndk-build.

Основной язык документации проекта — русский; комментарии в Java/C-коде — английские
(следуйте этому разделению при правках).

## Технологический стек

| Компонент | Версия | Назначение |
|---|---|---|
| chocolate-doom | git master (shallow clone) | движок (`chocolate-heretic`), C |
| SDL2 | 2.32.10 | видео, ввод, аудио |
| SDL2_mixer | 2.8.1 | звуковые эффекты (только WAV; музыка — через OPL) |
| SDL2_net | 2.2.0 | сетевая игра |
| Android Gradle Plugin | 9.2.1 | сборка APK |
| NDK | 28.2.13676358 (закреплён в `app/build.gradle.kts`) | сборка нативного кода |
| Java | 17+ (тулчейн Gradle) | Java-часть приложения |

Ключевые параметры сборки: `compileSdk`/`targetSdk` 36, `minSdk` 21,
ABI `arm64-v8a` + `armeabi-v7a`, выравнивание страниц 16 КБ
(`-Wl,-z,max-page-size=16384` в `Application.mk`, требование Android 15+).
AndroidX не используется (`android.useAndroidX=false` в `gradle.properties`),
тема — `android:Theme.NoTitleBar.Fullscreen`, ориентация — landscape.

`gradle/libs.versions.toml` содержит много Kotlin/Compose-зависимостей, но они
**не подключены** ни в одном модуле — не добавляйте их без необходимости,
проект чисто Java + ndk-build.

## Структура каталогов

```
app/src/main/
├── AndroidManifest.xml              # activity (landscape, singleInstance), разрешения INTERNET/VIBRATE
├── java/com/eretik/heretic/         # свой код приложения (~560 строк Java)
│   ├── HereticActivity.java         # SDLActivity: список .so, аргументы движка, поиск/копирование IWAD
│   ├── TouchControls.java           # построение экранного оверлея управления
│   ├── JoystickView.java            # виртуальный 8-сторонний джойстик (шлёт SDL key events)
│   └── KeyButtonView.java           # круглые кнопки: режимы HOLD / TOGGLE / TAP
├── java/org/libsdl/app/             # вендорная Java-обвязка SDL2 (не править без нужды)
├── res/values/                      # strings (app_name = "Heretic"), styles, colors
└── jni/                             # нативная сборка (ndk-build)
    ├── Android.mk                   # верхнеуровневый makefile, конфиг кодеков SDL2_mixer (WAV only)
    ├── Application.mk               # ABI, APP_PLATFORM=android-21, 16KB alignment, -O2
    ├── config/config.h              # рукописная замена autotools-заголовку для chocolate-heretic
    ├── heretic/Android.mk           # модуль libheretic.so: движок + src/heretic + textscreen + OPL + pcsound
    ├── SDL, SDL2_mixer, SDL2_net    # симлинки на deps/ (создаются fetch-deps.sh)
    └── chocolate-doom               # симлинк на ./chocolate-doom
chocolate-doom/                      # клон исходников движка (в .gitignore, НЕ редактируется)
deps/                                # распакованные тарболы SDL2 (в .gitignore)
wad/                                 # игровые WAD-файлы для тестов (в .gitignore, кроме самого каталога)
scripts/fetch-deps.sh                # скачивание chocolate-doom и SDL2, создание симлинков
docs/                                # скриншоты для README
```

Как собирается нативная часть: `jni/heretic/Android.mk` берёт `wildcard` по
`chocolate-doom/src/*.c` (исключая `d_dedicated.c`, `i_winmusic.c`,
`w_file_win32.c`, `z_native.c`), `src/heretic/*.c`, `textscreen/*.c`, плюс явный
список файлов OPL и pcsound, и линкует всё в `libheretic.so` вместе с
`SDL2`, `SDL2_mixer`, `SDL2_net`. Порядок загрузки библиотек задан в
`HereticActivity.getLibraries()`.

## Сборка и запуск

Требования: Android SDK (platform 35+), NDK **28.2.13676358**, Java 17+,
`local.properties` с `sdk.dir=...`.

```bash
scripts/fetch-deps.sh        # однократно: клонирует chocolate-doom, качает SDL2, создаёт симлинки
./gradlew assembleDebug      # сборка APK → app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug       # установка на подключённое устройство/эмулятор
```

`deps/`, `chocolate-doom/` и `wad/*.wad` находятся в `.gitignore` — после чистого
клона обязательно запустите `scripts/fetch-deps.sh`.

### Установка IWAD (игровых данных)

Движку нужен IWAD. `HereticActivity` ищет во внутреннем хранилище приложения
(в порядке приоритета): `heretic.wad` (retail), `heretic1.wad` (shareware),
`blasphem.wad` (свободная замена Blasphemer). Файл считается валидным, если
больше 1 МБ.

```bash
# debug-сборка:
adb push heretic1.wad /data/local/tmp/
adb shell "run-as com.eretik.heretic sh -c 'mkdir -p files && cat /data/local/tmp/heretic1.wad > files/heretic1.wad'"
```

Либо положить WAD в `/sdcard/Android/data/com.eretik.heretic/files/` — приложение
само скопирует его при первом запуске (`copyIwadIfPresent()`).

Движку передаются аргументы `-iwad`, `-config` (`default.cfg`), `-extraconfig`
(`extra.cfg`), `-savedir` (`savegames/`) — всё во внутреннем хранилище приложения.

## Соглашения по коду

- Свой код — пакет `com.eretik.heretic` (Java, без AndroidX, без Kotlin).
- Тач-контролы реализованы как кастомные `View` поверх `RelativeLayout` из
  `SDLActivity.getContentView()` и эмулируют клавиатуру через `KeyEvent` —
  движок не модифицирован под тач, все изменения управления делаются на
  Java-стороне через SDL key events.
- **Не редактируйте** файлы в `chocolate-doom/`, `deps/` и
  `app/src/main/java/org/libsdl/app/` — это внешние исходники, обновляемые
  целиком. Изменения в них будут потеряны при переустановке зависимостей.
- Изменения в составе нативной сборки (новые файлы движка, кодеки mixer) — только
  через `app/src/main/jni/Android.mk` и `app/src/main/jni/heretic/Android.mk`.
- Комментарии в Java — на английском (как в существующем коде), README и этот
  файл — на русском.

## Тестирование

Автотестов нет: каталоги `app/src/test` и `app/src/androidTest` отсутствуют,
тестовые зависимости в `libs.versions.toml` объявлены, но не подключены.
Проверка изменений — ручная: `./gradlew assembleDebug` + запуск на устройстве/
эмуляторе с установленным IWAD. В upstream-репозитории chocolate-doom есть свой
тестовый стенд (`quickcheck/`), но он к Android-сборке не подключён.

## Безопасность и лицензии

- **Не коммитьте** `heretic.wad` / `heretic1.wad` — коммерческие данные id Software;
  `wad/*.wad` уже в `.gitignore`. Свободная альтернатива для тестов — Blasphemer.
- Chocolate Doom/Heretic — GPLv2 (см. `chocolate-doom/COPYING.md`); SDL2, SDL2_mixer,
  SDL2_net — zlib. Производные сборки должны соблюдать GPLv2.
- Приложению нужны только разрешения `INTERNET` / `ACCESS_NETWORK_STATE` (сетевая
  игра) и `VIBRATE`; доступ к внешнему хранилищу не запрашивается — используется
  app-specific каталог `getExternalFilesDir()`.
- `local.properties` (путь к SDK) не коммитится.
