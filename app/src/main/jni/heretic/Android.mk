LOCAL_PATH := $(call my-dir)

CHOC := $(LOCAL_PATH)/../crispy-doom

include $(CLEAR_VARS)

LOCAL_MODULE := heretic

LOCAL_C_INCLUDES := \
    $(LOCAL_PATH)/../config \
    $(CHOC)/src \
    $(CHOC)/src/heretic \
    $(CHOC)/textscreen \
    $(CHOC)/opl \
    $(CHOC)/pcsound

# --- Common engine code (src/*.c), minus server/win32/stdc-wad backends ---
ENGINE_SRC := $(wildcard $(CHOC)/src/*.c)
ENGINE_SRC := $(filter-out \
    $(CHOC)/src/d_dedicated.c \
    $(CHOC)/src/i_winmusic.c \
    $(CHOC)/src/w_file_win32.c \
    $(CHOC)/src/z_native.c \
    , $(ENGINE_SRC))

# --- Heretic game code ---
GAME_SRC := $(wildcard $(CHOC)/src/heretic/*.c)

# --- Textscreen library (ENDOOM screen) ---
TXT_SRC := $(wildcard $(CHOC)/textscreen/*.c)

# --- OPL (AdLib/SB music synth) ---
OPL_SRC := \
    $(CHOC)/opl/opl.c \
    $(CHOC)/opl/opl3.c \
    $(CHOC)/opl/opl_queue.c \
    $(CHOC)/opl/opl_sdl.c \
    $(CHOC)/opl/opl_timer.c

# --- PC speaker sound ---
PCS_SRC := \
    $(CHOC)/pcsound/pcsound.c \
    $(CHOC)/pcsound/pcsound_sdl.c

LOCAL_SRC_FILES := $(ENGINE_SRC) $(GAME_SRC) $(TXT_SRC) $(OPL_SRC) $(PCS_SRC) \
    render_smooth.c

LOCAL_CFLAGS := -O2 -Wno-unused -Wno-pointer-sign -DCRISPY_TRUECOLOR

LOCAL_SHARED_LIBRARIES := SDL2 SDL2_mixer SDL2_net

LOCAL_LDLIBS := -llog -lm

include $(BUILD_SHARED_LIBRARY)
