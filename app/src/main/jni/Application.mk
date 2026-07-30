# Heretic (Chocolate Heretic) Android port

APP_PLATFORM := android-21
APP_ABI := armeabi-v7a arm64-v8a

# 16 KB page-size alignment (required by Android 15+ devices)
APP_LDFLAGS := -Wl,-z,max-page-size=16384

# Optimizations
APP_CFLAGS := -O2 -fvisibility=default

APP_DEPRECATED_HEADERS := true
