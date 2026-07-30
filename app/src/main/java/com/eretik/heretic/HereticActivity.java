package com.eretik.heretic;

import android.os.Bundle;
import android.util.Log;

import org.libsdl.app.SDLActivity;

import java.io.File;

/**
 * Heretic (Chocolate Heretic) launcher activity.
 *
 * The game IWAD must be placed into the app files directory, e.g. via:
 *   adb push heretic1.wad /data/local/tmp/
 *   adb shell "run-as com.eretik.heretic sh -c 'cat /data/local/tmp/heretic1.wad > files/heretic1.wad'"
 * (debug builds), or dropped into /sdcard/Android/data/com.eretik.heretic/files/
 * from where it is copied automatically at first start.
 *
 * Recognized IWADs, in priority order: heretic.wad (retail),
 * heretic1.wad (shareware), blasphem.wad (free replacement).
 */
public class HereticActivity extends SDLActivity {

    private static final String TAG = "Heretic";

    private static final String[] IWAD_NAMES = {
            "heretic.wad",      // Heretic: Shadow of the Serpent Riders (retail)
            "heretic1.wad",     // Heretic shareware (episode 1)
            "blasphem.wad"      // Blasphemer (free replacement, partial)
    };

    @Override
    protected String[] getLibraries() {
        return new String[]{
                "SDL2",
                "SDL2_mixer",
                "SDL2_net",
                "heretic"
        };
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        copyIwadIfPresent();
        super.onCreate(savedInstanceState);
        TouchControls.setup(this);
    }

    @Override
    protected String[] getArguments() {
        File files = getFilesDir();
        File saveDir = new File(files, "savegames");
        //noinspection ResultOfMethodCallIgnored
        saveDir.mkdirs();

        File iwad = findIwad();
        if (iwad == null) {
            // Engine will show a clean "IWAD not found" message.
            iwad = new File(files, IWAD_NAMES[0]);
        }

        return new String[]{
                "-iwad", iwad.getAbsolutePath(),
                "-config", new File(files, "default.cfg").getAbsolutePath(),
                "-extraconfig", new File(files, "extra.cfg").getAbsolutePath(),
                "-savedir", saveDir.getAbsolutePath()
        };
    }

    private File findIwad() {
        for (String name : IWAD_NAMES) {
            File f = new File(getFilesDir(), name);
            if (f.exists() && f.length() > 1000000) {
                return f;
            }
        }
        return null;
    }

    /**
     * If the user dropped a WAD into the app-specific external files dir
     * (no permissions needed), copy it into internal storage.
     */
    private void copyIwadIfPresent() {
        if (findIwad() != null) {
            return;
        }
        File externalDir = getExternalFilesDir(null);
        if (externalDir == null) {
            return;
        }
        for (String name : IWAD_NAMES) {
            File src = new File(externalDir, name);
            if (src.exists() && src.length() > 1000000) {
                File dst = new File(getFilesDir(), name);
                try {
                    java.nio.file.Files.copy(src.toPath(), dst.toPath());
                    Log.i(TAG, "Copied IWAD from " + src.getAbsolutePath());
                } catch (Exception e) {
                    Log.e(TAG, "Failed to copy IWAD", e);
                }
                return;
            }
        }
    }
}
