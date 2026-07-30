LOCAL_PATH := $(call my-dir)
ERETIK_JNI := $(LOCAL_PATH)

# ---- SDL2_mixer codec configuration (WAV only; OPL handles game music) ----
SUPPORT_WAV := true
SUPPORT_FLAC_DRFLAC := false
SUPPORT_FLAC_LIBFLAC := false
SUPPORT_OGG_STB := false
SUPPORT_OGG := false
SUPPORT_MP3_MINIMP3 := false
SUPPORT_MP3_MPG123 := false
SUPPORT_WAVPACK := false
SUPPORT_GME := false
SUPPORT_MOD_XMP := false
SUPPORT_MID_TIMIDITY := false

include $(ERETIK_JNI)/SDL/Android.mk
include $(ERETIK_JNI)/SDL2_mixer/Android.mk
include $(ERETIK_JNI)/SDL2_net/Android.mk
include $(ERETIK_JNI)/heretic/Android.mk
