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

package com.eretik.heretic;

import android.os.Bundle;

import org.libsdl.app.SDLActivity;

import java.io.File;

/**
 * Heretic (Chocolate Heretic) launcher activity.
 *
 * The game IWAD must be placed into the app files directory, e.g. via:
 *   adb push heretic1.wad /data/local/tmp/
 *   adb shell "run-as com.eretik.heretic sh -c 'cat /data/local/tmp/heretic1.wad > files/heretic1.wad'"
 * (debug builds), or dropped into /sdcard/Android/data/com.eretik.heretic/files/
 * from where it is imported automatically at first start.
 */
public class HereticActivity extends SDLActivity {

    private static final String[] LIBRARIES = {
            "SDL2",
            "SDL2_mixer",
            "SDL2_net",
            "heretic"
    };

    @Override
    protected String[] getLibraries() {
        return LIBRARIES;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        IwadLocator.importFromExternalIfPresent(this);
        super.onCreate(savedInstanceState);
        TouchControls.setup(this);
    }

    @Override
    protected String[] getArguments() {
        File files = getFilesDir();
        File saveDir = new File(files, "savegames");
        //noinspection ResultOfMethodCallIgnored
        saveDir.mkdirs();

        File iwad = IwadLocator.find(files);
        if (iwad == null) {
            // Engine will show a clean "IWAD not found" message.
            iwad = new File(files, IwadLocator.IWAD_NAMES[0]);
        }

        return new String[]{
                "-iwad", iwad.getAbsolutePath(),
                "-config", new File(files, "default.cfg").getAbsolutePath(),
                "-extraconfig", new File(files, "extra.cfg").getAbsolutePath(),
                "-savedir", saveDir.getAbsolutePath()
        };
    }
}
