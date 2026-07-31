// Eretik — Heretic port for Android
// Copyright (C) 2026 Aleksey Karakuts
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see <https://www.gnu.org/licenses/>.

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
