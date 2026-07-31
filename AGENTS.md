# AGENTS.md — Eretik

## Обзор проекта

**Eretik** — порт классической игры **Heretic** (Raven Software, 1994) на Android.
Собран на базе [Crispy Heretic](https://github.com/fabiangreffrath/crispy-doom)
(форк Chocolate Heretic с high-res рендером 640×400, truecolor и uncapped FPS)
и SDL2. Truecolor включён на этапе компиляции (`-DCRISPY_TRUECOLOR` в
`app/src/main/jni/heretic/Android.mk`); `crispy_hires` / `crispy_truecolor`
включаются в `default.cfg` / `extra.cfg`.

Репозиторий — это по сути тонкая Android-обвязка: свой код — один Gradle-модуль `app`
с Java-активностью и тач-контролями, а весь движок собирается из исходников
crispy-doom через ndk-build.

Основной язык документации проекта — русский; комментарии в Java/C-коде — английские
(следуйте этому разделению при правках).

## Технологический стек

| Компонент | Версия | Назначение |
|---|---|---|
| crispy-doom | git master (shallow clone) | движок (`crispy-heretic`), C |
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
├── java/com/eretik/heretic/         # свой код приложения (~670 строк Java)
│   ├── HereticActivity.java         # SDLActivity: список .so, аргументы движка
│   ├── IwadLocator.java             # поиск/импорт IWAD (приоритет: heretic → heretic1 → blasphem)
│   ├── TouchControls.java           # декларативная раскладка экранного оверлея управления
│   ├── JoystickView.java            # виртуальный 8-сторонний джойстик (шлёт SDL key events)
│   ├── KeyButtonView.java           # круглые кнопки: enum Mode { HOLD / TOGGLE / TAP }, хаптика
│   └── OverlayStyle.java            # общие цвета/кисти оверлея
├── java/org/libsdl/app/             # вендорная Java-обвязка SDL2 (не править без нужды)
├── res/values/                      # strings (app_name = "Heretic"), styles, colors
└── jni/                             # нативная сборка (ndk-build)
    ├── Android.mk                   # верхнеуровневый makefile, конфиг кодеков SDL2_mixer (WAV only)
    ├── Application.mk               # ABI, APP_PLATFORM=android-21, 16KB alignment, -O2
    ├── config/config.h              # рукописная замена autotools-заголовку для crispy-heretic
    ├── heretic/Android.mk           # модуль libheretic.so: движок + src/heretic + textscreen + OPL + pcsound + render_smooth.c
    ├── SDL, SDL2_mixer, SDL2_net    # симлинки на deps/ (создаются fetch-deps.sh)
    └── crispy-doom                  # симлинк на ./crispy-doom
crispy-doom/                         # клон исходников движка (в .gitignore, НЕ редактируется)
deps/                                # распакованные тарболы SDL2 (в .gitignore)
wad/                                 # игровые WAD-файлы для тестов (в .gitignore, кроме самого каталога)
scripts/fetch-deps.sh                # скачивание crispy-doom и SDL2, создание симлинков
docs/                                # скриншоты для README
```

Как собирается нативная часть: `jni/heretic/Android.mk` берёт `wildcard` по
`crispy-doom/src/*.c` (исключая `d_dedicated.c`, `i_winmusic.c`,
`w_file_win32.c`, `z_native.c`), `src/heretic/*.c`, `textscreen/*.c`, плюс явный
список файлов OPL и pcsound, и линкует всё в `libheretic.so` вместе с
`SDL2`, `SDL2_mixer`, `SDL2_net`. Порядок загрузки библиотек задан в
`HereticActivity.getLibraries()`. Свой `jni/heretic/render_smooth.c` форсирует
билинейную фильтрацию при масштабировании (`SDL_HINT_OVERRIDE` на
`SDL_RENDER_SCALE_QUALITY=linear`).

## Сборка и запуск

Требования: Android SDK (platform 35+), NDK **28.2.13676358**, Java 17+,
`local.properties` с `sdk.dir=...`.

```bash
scripts/fetch-deps.sh        # однократно: клонирует crispy-doom, качает SDL2, создаёт симлинки
./gradlew assembleDebug      # сборка APK → app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug       # установка на подключённое устройство/эмулятор
```

`deps/`, `crispy-doom/` и `wad/*.wad` находятся в `.gitignore` — после чистого
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
В `default.cfg` дописаны `crispy_hires 1` и `crispy_truecolor 1` — high-res
рендер 640×400 и truecolor; crispy-heretic биндит эти переменные в
`src/heretic/d_main.c` и пересохраняет их при выходе. Там же можно включить
`crispy_uncapped 1` (анкапнутый FPS), `crispy_freelook`, `crispy_widescreen`.

## Соглашения по коду

- Свой код — пакет `com.eretik.heretic` (Java, без AndroidX, без Kotlin).
- Тач-контролы реализованы как кастомные `View` поверх `RelativeLayout` из
  `SDLActivity.getContentView()` и эмулируют клавиатуру через `KeyEvent` —
  движок не модифицирован под тач, все изменения управления делаются на
  Java-стороне через SDL key events.
- **Не редактируйте** файлы в `crispy-doom/`, `deps/` и
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
эмуляторе с установленным IWAD. В upstream-репозитории crispy-doom есть свой
тестовый стенд (`quickcheck/`), но он к Android-сборке не подключён.

## Безопасность и лицензии

- **Не коммитьте** `heretic.wad` / `heretic1.wad` — коммерческие данные id Software;
  `wad/*.wad` уже в `.gitignore`. Свободная альтернатива для тестов — Blasphemer.
- Собственный код и приложение целиком — **GPL-2.0+** (текст лицензии — `COPYING`
  в корне, заголовки — во всех своих `.java`/`.c`). Это требование движка:
  Crispy/Chocolate Doom и код Heretic (id/Raven) под GPL-2.0+
  (см. `crispy-doom/COPYING.md`); SDL2, SDL2_mixer, SDL2_net — zlib
  (GPL-совместимая). Производные сборки должны соблюдать GPLv2.
- Приложению нужны только разрешения `INTERNET` / `ACCESS_NETWORK_STATE` (сетевая
  игра) и `VIBRATE`; доступ к внешнему хранилищу не запрашивается — используется
  app-specific каталог `getExternalFilesDir()`.
- `local.properties` (путь к SDK) не коммитится.
