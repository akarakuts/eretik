# Состояние проекта Eretik (порт Heretic на Android)

Снимок: 2026-07-30. Для продолжения работы в новой сессии (Kimi Code / Kimi Work).

## Что готово

1. **Рабочий порт** Chocolate Heretic → Android в `/Users/a.karakuts/Bars/eretik`:
   - Сборка: `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`
   - Стек: chocolate-doom (git master, клон в `chocolate-doom/`), SDL2 2.32.10,
     SDL2_mixer 2.8.1 (только WAV), SDL2_net 2.2.0 — в `deps/`, подключены симлинками
     из `app/src/main/jni/` (пересоздаются `scripts/fetch-deps.sh`).
   - ABI arm64-v8a + armeabi-v7a, minSdk 21, NDK 28.2.13676358, 16KB alignment.
   - `config.h` для движка — `app/src/main/jni/config/config.h`.
   - Список исходников — `app/src/main/jni/heretic/Android.mk`
     (исключены d_dedicated, i_winmusic, w_file_win32, z_native; нужны ОБА w_file_posix и w_file_stdc).
2. **Экранное управление** (эта сессия):
   - `JoystickView.java` — 8-позиционный цифровой джойстик (вверх/вниз = DPAD_UP/DOWN,
     влево/вправо = поворот DPAD_LEFT/RIGHT, в режиме STR = стрейф COMMA/PERIOD).
   - `KeyButtonView.java` — круглые кнопки, режимы HOLD / TOGGLE / TAP (+цикл оружия).
   - `TouchControls.java` — раскладка оверлея на `SDLActivity.getContentView()`
     (RelativeLayout из SDL). Ключи шлются через `SDLActivity.onNativeKeyDown/Up`.
   - Раскладка: верх-слева RUN(Shift, toggle) и STR (latch стрейфа джойстика);
     верх-справа два ряда: ESC MAP WPN / ] [ ART; низ-слева джойстик;
     низ-справа FIRE(Ctrl), USE(Space), F+(PgUp), F-(Insert — fly down у Heretic = KEY_INS!).
   - Бинды сверены с `src/m_controls.c`: strafe=',' '.'; flyup=PGUP; flydown=INS;
     inv='[' ']'; artifact=ENTER.
3. **HereticActivity** выбирает IWAD по приоритету: heretic.wad → heretic1.wad → blasphem.wad;
   аргументы `-iwad/-config/-extraconfig/-savedir`; копирование WAD из external-files при старте.
4. **Проверено на эмуляторе** (AVD `heretic_test`, Android 35, google_apis_playstore_tablet,
   в `~/.android/avd/heretic_test.avd/config.ini` подправлен `image.sysdir.1` на
   `system-images/android-35/google_apis_playstore_tablet/arm64-v8a/`):
   - с Blasphemer (`wad/blasphem.wad`→heretic.wad): титул, демо E1M1, меню по ESC — работают;
   - скриншоты в `docs/`.

## Открытые проблемы (текущая отладка)

### BUG-1: РЕШЁН — краш меню был вызван битым heretic1.wad
- Старый WAD (PSP-рип `heretic_share.wad`, md5 `c0a132a4d5cc1842c99ac7245539ab7b`)
  отличался от канонического shareware v1.2 ровно на 15 байт, из них ~10 — в хвосте
  глифа **FONTB46 ('N')**: в последней колонке (col12) пост-лист не заканчивался
  байтом-терминатором `0xFF`. `V_DrawPatch` шёл по постам за пределы лампы,
  читал мусор из зонной памяти как topdelta/length и писал далеко за пределы
  экранного буфера (в конец 32-МБ zone) → SIGSEGV на Android.
- **Фикс**: `wad/heretic1.wad` заменён на канонический shareware v1.2
  (md5 `ae779722390ec32fa37b0d361f7d82f8`, 5 120 920 байт — сверено с Doom Wiki /
  libretro-database; источник: github.com/Akbar30Bill/DOOM_wads). Битый рип сохранён
  как `wad/heretic1.wad.psp-rip-broken`.
- **Проверка**: нативная repro-сборка (см. ниже) с ASan: на битом WAD —
  `heap-buffer-overflow v_video.c:183 V_DrawPatch ← MN_DrTextB` (та же строка, что
  в tombstone); на каноническом — чисто, меню открыто, 120 кадров без ошибок.
  На эмуляторе (heretic_test): ESC на ~14 c → меню открывается, краша нет
  (скриншот проверен визуально).
- Побочная находка (не наш баг, живёт в upstream chocolate-doom):
  `S_StartSound` на титульнике читает поле `mobj_t.player` у `degenmobj_t`
  dummy-listener (global-buffer-overflow *read*, безвредное — ASan ловит как
  `s_sound.c S_StartSound` из `MN_ActivateMenu`). Можно зарепортить в chocolate-doom.

### Нативная repro-площадка (build/native-repro/, в .gitignore)
- `build.sh` — собирает chocolate-heretic из тех же исходников, что
  `jni/heretic/Android.mk` (+SDL2_mixer WAV-only, SDL2_net из `deps/`), компилятором
  clang с `-fsanitize=address,recover`, линкует brew `sdl2-compat`.
  Требуется `pkg-config sdl2` (sdl2-compat + sdl3 из Homebrew).
- `d_main_patched.c` — копия `src/heretic/d_main.c` с автотриггером: на тике 280
  `MN_ActivateMenu()`, на тике 400 пишет `repro_result.txt` и `exit(42)`.
- Запуск headless:
  `cd build/native-repro/run && DYLD_FALLBACK_LIBRARY_PATH=/opt/homebrew/lib SDL_VIDEODRIVER=dummy SDL_AUDIODRIVER=dummy ASAN_OPTIONS=halt_on_error=0 ../chocolate-heretic -iwad ../../../wad/heretic1.wad -config default.cfg -extraconfig extra.cfg -savedir .`
  (без `DYLD_FALLBACK_LIBRARY_PATH` sdl2-compat не находит libSDL3 и висит
  в модальном NSAlert до main).

### BUG-2: Blasphemer + демо → I_Error "R_FindPlane: no more visplanes"
- Демо-лупы Blasphemer (ZDoom-семейство) десинхронизируются в vanilla-лимитах
  chocolate → переполнение visplanes. Меню/E1M1 работают. Документировать как
  «частичная совместимость»; основной сценарий — настоящий heretic.wad / heretic1.wad.
- После OK в диалоге I_Error процесс иногда падает с SIGSEGV внутри I_Error
  (тот же tombstone-паттерн, I_Error+432) — вероятно повторный вызов SDL из
  обработчика ошибки на SDL-потоке; разобраться отдельно.

## Окружение и команды

- SDK: `~/Library/Android/sdk`, NDK 28.2.13676358, platform-tools, образ
  `system-images;android-35;google_apis_playstore_tablet;arm64-v8a` (на 37.x ps16k
  avdmanager ругался — не создавал).
- Эмулятор (без root, production): запуск —
  `ANDROID_SDK_ROOT=~/Library/Android/sdk ~/Library/Android/sdk/emulator/emulator -avd heretic_test -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect`
  Стоп: `adb emu kill`. Скриншот: `adb exec-out screencap -p > x.png`.
- Установка WAD (debug): `adb push wad/heretic1.wad /data/local/tmp/ && adb shell "run-as com.eretik.heretic sh -c 'mkdir -p files && cat /data/local/tmp/heretic1.wad > files/heretic1.wad'"`.
- Запуск: `adb shell am start -n com.eretik.heretic/.HereticActivity`.
  Координаты кнопок на 1080x2400: ESC≈(950,110), ART≈(566,299).
- Логи: `adb logcat -d | grep -E "DEBUG|libc"` (stdout движка в logcat НЕ попадает;
  стек — через `F DEBUG` строки tombstone).
- Пиратский retail `heretic.wad` (archive.org `msdos_Heretic_1994`) **не использовать** —
  удалён сознательно; тестовые ассеты только shareware/Blasphemer.

## Ближайшие шаги

1. ~~BUG-1~~ — решён (битый WAD, см. выше). BUG-2 при желании: посмотреть
   повторный SIGSEGV внутри I_Error после диалога.
2. Опционально: release-сборка с подписью, иконка приложения вместо дефолтной SDL.
3. Опционально: отправить в upstream chocolate-doom репорт про OOB-read
   в `S_StartSound` (dummy listener на титульнике).
