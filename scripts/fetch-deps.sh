#!/bin/bash
# Скачивает исходники chocolate-doom и SDL2-библиотек, создаёт симлинки для ndk-build.
set -e
cd "$(dirname "$0")/.."

SDL2_VER=2.32.10
MIXER_VER=2.8.1
NET_VER=2.2.0

if [ ! -d chocolate-doom ]; then
  git clone --depth 1 https://github.com/chocolate-doom/chocolate-doom.git
fi

mkdir -p deps
cd deps
[ -d "SDL2-$SDL2_VER" ]           || { curl -sL -o sdl2.tar.gz  "https://github.com/libsdl-org/SDL/releases/download/release-$SDL2_VER/SDL2-$SDL2_VER.tar.gz" && tar xzf sdl2.tar.gz; }
[ -d "SDL2_mixer-$MIXER_VER" ]    || { curl -sL -o mixer.tar.gz "https://github.com/libsdl-org/SDL_mixer/releases/download/release-$MIXER_VER/SDL2_mixer-$MIXER_VER.tar.gz" && tar xzf mixer.tar.gz; }
[ -d "SDL2_net-$NET_VER" ]        || { curl -sL -o net.tar.gz   "https://github.com/libsdl-org/SDL_net/releases/download/release-$NET_VER/SDL2_net-$NET_VER.tar.gz" && tar xzf net.tar.gz; }
cd ..

JNI=app/src/main/jni
ln -sfn "../../../../deps/SDL2-$SDL2_VER"        "$JNI/SDL"
ln -sfn "../../../../deps/SDL2_mixer-$MIXER_VER" "$JNI/SDL2_mixer"
ln -sfn "../../../../deps/SDL2_net-$NET_VER"     "$JNI/SDL2_net"
ln -sfn "../../../../chocolate-doom"             "$JNI/chocolate-doom"

echo "Готово. Теперь: ./gradlew assembleDebug"
