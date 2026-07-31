// Force bilinear filtering for all renderer scaling stages.
//
// The engine sets SDL_HINT_RENDER_SCALE_QUALITY to "nearest" for the
// intermediate texture blit (chocolate-doom i_video.c), which keeps the
// picture gritty and pixelated. Setting the same hint here with
// SDL_HINT_OVERRIDE priority makes those later SDL_SetHint() calls no-ops,
// so every scale step (320x200 -> upscaled -> screen) uses linear filtering
// and the image looks softer and more natural.
//
// Runs at library load time, before the engine creates its textures
// (SDL reads the hint when a texture is created).

#include <SDL.h>

__attribute__((constructor))
static void ForceSmoothScaling(void)
{
    SDL_SetHintWithPriority(SDL_HINT_RENDER_SCALE_QUALITY, "linear",
                            SDL_HINT_OVERRIDE);
}
