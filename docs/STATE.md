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

### BUG-1: краш меню с shareware heretic1.wad
- WAD: `wad/heretic1.wad` (= `heretic_share.wad` из PSP-сборки `heretic-0.2.zip`,
  archive.org item `heretic-0.2.7z`, 5 120 920 байт, 1374 lumps).
- Симптом: титульник показывается; через ~11 c тап по ESC (открытие меню) →
  SIGSEGV (SEGV_ACCERR, запись) в `V_DrawPatch` (v_video.c:183) ← `MN_DrTextB`
  (mn_menu.c:405) ← `D_Display` (d_main.c:199). Адреса декодированы
  `llvm-addr2line` по `app/build/intermediates/cxx/Debug/48o1d555/obj/local/arm64-v8a/libheretic.so`.
- **Уже исключено** (проверено скриптом): лампы M_HTIC, M_SKL00 валидны;
  все 95 символов FONTA/FONTB валидны; TITLE/CREDIT/HELP1/HELP2 — raw 64000 (норма
  для Heretic); лампа DEHACKED нет. С blasphem.wad то же меню работает.
- Гипотезы: (а) PSP-рип WAD тонко модифицирован — сверить с каноническим
  shareware heretic1.wad из `htic_v12.zip` (нужен DOS/DEICE, напр. DOSBox);
  (б) баг проявляется только в gamemode=shareware (код MN_* в shareware-ветках);
  (в) Android-специфика (стек/выравнивание) — проверить, собрав chocolate-heretic
  нативно для macOS/Linux с тем же WAD.

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

1. BUG-1: воспроизвести нативно (macOS: собрать chocolate-doom через cmake/autotools
   с `wad/heretic1.wad`) или сравнить с каноническим shareware WAD.
2. Обновить README (сделано ниже) и закоммитить.
3. Опционально: release-сборка с подписью, иконка приложения вместо дефолтной SDL.
