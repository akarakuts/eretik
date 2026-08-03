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

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Locates and imports the game IWAD.
 *
 * Recognized IWADs, in priority order: heretic.wad (retail),
 * heretic1.wad (shareware), blasphem.wad (free replacement).
 *
 * Sources (first match wins): internal files dir, app-specific external
 * files dir, then a bundled asset (typically shareware heretic1.wad).
 */
final class IwadLocator {

    private static final String TAG = "Heretic";

    /** Files at or below this size are treated as corrupt/incomplete. */
    private static final long MIN_IWAD_BYTES = 1_000_000;

    static final String[] IWAD_NAMES = {
            "heretic.wad",      // Heretic: Shadow of the Serpent Riders (retail)
            "heretic1.wad",     // Heretic shareware (episode 1)
            "blasphem.wad"      // Blasphemer (free replacement, partial)
    };

    private IwadLocator() {}

    /** Returns the first valid IWAD in the directory, or null if there is none. */
    static File find(File dir) {
        for (String name : IWAD_NAMES) {
            File candidate = new File(dir, name);
            if (candidate.isFile() && candidate.length() > MIN_IWAD_BYTES) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Ensures an IWAD is present in internal storage: import from external
     * files if needed, otherwise extract a bundled asset.
     */
    static void ensureIwad(Context context) {
        File internalDir = context.getFilesDir();
        if (find(internalDir) != null) {
            return;
        }
        if (importFromExternal(context, internalDir)) {
            return;
        }
        extractBundledAsset(context, internalDir);
    }

    /**
     * If internal storage has no IWAD yet, imports one dropped into the
     * app-specific external files dir (no permissions needed).
     *
     * @return true if an IWAD was imported
     */
    private static boolean importFromExternal(Context context, File internalDir) {
        File externalDir = context.getExternalFilesDir(null);
        if (externalDir == null) {
            return false;
        }
        for (String name : IWAD_NAMES) {
            File src = new File(externalDir, name);
            if (!src.isFile() || src.length() <= MIN_IWAD_BYTES) {
                continue;
            }
            File dst = new File(internalDir, name);
            try {
                Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
                Log.i(TAG, "Imported IWAD from " + src.getAbsolutePath());
                return true;
            } catch (IOException e) {
                Log.e(TAG, "Failed to import IWAD from " + src.getAbsolutePath(), e);
            }
        }
        return false;
    }

    /** Extracts the first matching IWAD asset into internal storage, if present. */
    private static void extractBundledAsset(Context context, File internalDir) {
        AssetManager assets = context.getAssets();
        for (String name : IWAD_NAMES) {
            try (InputStream in = assets.open(name)) {
                File dst = new File(internalDir, name);
                try (OutputStream out = new FileOutputStream(dst)) {
                    byte[] buf = new byte[64 * 1024];
                    int n;
                    while ((n = in.read(buf)) >= 0) {
                        out.write(buf, 0, n);
                    }
                }
                if (dst.length() <= MIN_IWAD_BYTES) {
                    //noinspection ResultOfMethodCallIgnored
                    dst.delete();
                    Log.w(TAG, "Bundled asset " + name + " is too small, ignored");
                    continue;
                }
                Log.i(TAG, "Extracted bundled IWAD asset " + name
                        + " (" + dst.length() + " bytes)");
                return;
            } catch (IOException e) {
                // Asset not packaged in this build — try the next name.
            }
        }
    }
}
