package com.eretik.heretic;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Locates and imports the game IWAD.
 *
 * Recognized IWADs, in priority order: heretic.wad (retail),
 * heretic1.wad (shareware), blasphem.wad (free replacement).
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
     * If internal storage has no IWAD yet, imports one dropped into the
     * app-specific external files dir (no permissions needed).
     */
    static void importFromExternalIfPresent(Context context) {
        File internalDir = context.getFilesDir();
        if (find(internalDir) != null) {
            return;
        }
        File externalDir = context.getExternalFilesDir(null);
        if (externalDir == null) {
            return;
        }
        for (String name : IWAD_NAMES) {
            File src = new File(externalDir, name);
            if (!src.isFile() || src.length() <= MIN_IWAD_BYTES) {
                continue;
            }
            File dst = new File(internalDir, name);
            try {
                Files.copy(src.toPath(), dst.toPath());
                Log.i(TAG, "Imported IWAD from " + src.getAbsolutePath());
            } catch (IOException e) {
                Log.e(TAG, "Failed to import IWAD from " + src.getAbsolutePath(), e);
            }
            return;
        }
    }
}
