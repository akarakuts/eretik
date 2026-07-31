# Eretik — порт Heretic на Android

Порт классической игры **Heretic** (Raven Software, 1994) на Android на базе
[Crispy Heretic](https://github.com/fabiangreffrath/crispy-doom) —
форка Chocolate Heretic с high-res рендером 640×400, truecolor и uncapped FPS —
и SDL2.

![Скриншот](docs/screenshot-demo.png)

## Состав

| Компонент | Версия | Назначение |
|---|---|---|
| crispy-doom | git master | движок (`crispy-heretic`) |
| SDL2 | 2.32.10 | видео, ввод, аудио |
| SDL2_mixer | 2.8.1 | звуковые эффекты (WAV) |
| SDL2_net | 2.2.0 | сетевая игра |

Исходники лежат в `deps/` (скачиваются скриптом ниже) и подключаются
в сборку через симлинки `app/src/main/jni/`.

## Требования

- Android SDK (platforms android-35+, build-tools)
- Android NDK **28.2.13676358** (прописан в `app/build.gradle.kts`)
- Java 17+
- `local.properties` с `sdk.dir=/путь/к/Android/sdk`

## Сборка

```bash
./gradlew assembleDebug
```

Готовый APK: `app/build/outputs/apk/debug/app-debug.apk`
(ABI: `arm64-v8a`, `armeabi-v7a`; minSdk 21; 16 КБ page-size alignment для Android 15+).

## Установка игровых данных (IWAD)

Движку нужен IWAD с ресурсами игры. Варианты:

- **Полная игра**: `heretic.wad` из легальной копии Heretic (Steam/GOG).
- **Shareware**: `heretic1.wad` (первый эпизод, свободно распространяется,
  зеркала idgames, файл `htic_v12.zip`).
- **Свободная замена**: [Blasphemer](https://github.com/Blasphemer/blasphemer)
  (`wad/blasphem.wad` уже лежит в репозитории; копия под именем `heretic.wad`
  использовалась для теста на скриншоте).

Файл нужно переименовать в `heretic.wad` и положить в одно из мест:

1. **Внутреннее хранилище приложения** (debug-сборка):
   ```bash
   adb push heretic.wad /data/local/tmp/
   adb shell "run-as com.eretik.heretic sh -c 'mkdir -p files && cat /data/local/tmp/heretic.wad > files/heretic.wad'"
   ```
2. **Внешняя папка приложения** — `/sdcard/Android/data/com.eretik.heretic/files/heretic.wad`:
   при первом запуске игра сама скопирует файл во внутреннее хранилище.

Конфиги (`default.cfg`, `extra.cfg`) и сейвы (`savegames/`) лежат во внутреннем
хранилище приложения, пути передаются движку аргументами из `HereticActivity`.

## Управление

Есть экранные контролы (оверлей поверх игры), а также работают клавиатура,
мышь и геймпад — как в обычном Crispy Heretic.

- **Джойстик** (слева внизу): движение вперёд/назад, повороты; диагонали
  нажимают две клавиши сразу, как на клавиатуре
- **STR**: фиксатор стрейфа — в активном состоянии джойстик влево/вправо стрейфит
- **RUN**: фиксатор бега (Shift)
- **FIRE** (Ctrl), **USE** (Space), **F+ / F-** (полёт вверх/вниз)
- **ESC**, **MAP** (Tab), **WPN** (циклическая смена оружия 1–7),
  **[ / ]** (инвентарь), **ART** (Enter — применить артефакт / подтвердить в меню)

## Известные проблемы

- С shareware `heretic1.wad` из PSP-сборки меню (ESC) падает с SIGSEGV в
  `V_DrawPatch` ← `MN_DrTextB` — расследуется, см. `docs/STATE.md` (BUG-1).
- Blasphemer: демо-заставки десинхронизируются (vanilla-лимиты) и могут
  приводить к `R_FindPlane: no more visplanes` — играйте с настоящим IWAD
  (BUG-2 в `docs/STATE.md`).

## Структура проекта

```
app/src/main/
├── AndroidManifest.xml            # activity, landscape, разрешения
├── java/com/eretik/heretic/
│   └── HereticActivity.java       # SDLActivity: библиотеки, аргументы, копирование WAD
├── java/org/libsdl/app/           # Java-обвязка SDL2
└── jni/
    ├── Android.mk                 # верхнеуровневый ndk-build
    ├── Application.mk             # ABI, APP_PLATFORM, 16KB alignment
    ├── config/config.h            # конфиг для crispy-heretic
    ├── heretic/Android.mk         # модуль libheretic.so (списки исходников + render_smooth.c)
    ├── SDL -> deps/SDL2-2.32.10
    ├── SDL2_mixer -> deps/SDL2_mixer-2.8.1
    ├── SDL2_net -> deps/SDL2_net-2.2.0
    └── crispy-doom -> ../crispy-doom
```

## Лицензии

- Crispy/Chocolate Doom — GPLv2 (см. `crispy-doom/COPYING.md`).
- SDL2, SDL2_mixer, SDL2_net — zlib.
- Blasphemer — свободные ресурсы (см. его репозиторий).
- Оригинальный `heretic.wad` — коммерческие данные, в репозиторий не входят.
