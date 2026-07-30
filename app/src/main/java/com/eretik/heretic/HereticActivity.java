package com.eretik.heretic;

import android.os.Bundle;
import android.util.Log;

import org.libsdl.app.SDLActivity;

import java.io.File;

/**
 * Heretic (Chocolate Heretic) launcher activity.
 *
 * The game IWAD (heretic.wad / heretic1.wad shareware) must be placed into
 * the app files directory, e.g. via:
 *   adb push heretic.wad /data/data/com.eretik.heretic/files/heretic.wad
 * (run-as com.eretik.heretic for debug builds), or copied at first start
 * from /sdcard/Android/data/com.eretik.heretic/files/heretic.wad
 */
public class HereticActivity extends SDLActivity {

    private static final String TAG = "Heretic";

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
    }

    @Override
    protected String[] getArguments() {
        File files = getFilesDir();
        File saveDir = new File(files, "savegames");
        //noinspection ResultOfMethodCallIgnored
        saveDir.mkdirs();
        return new String[]{
                "-iwad", new File(files, "heretic.wad").getAbsolutePath(),
                "-config", new File(files, "default.cfg").getAbsolutePath(),
                "-extraconfig", new File(files, "extra.cfg").getAbsolutePath(),
                "-savedir", saveDir.getAbsolutePath()
        };
    }

    /**
     * If the user dropped a WAD into the app-specific external files dir
     * (no permissions needed), copy it into internal storage.
     */
    private void copyIwadIfPresent() {
        File internal = new File(getFilesDir(), "heretic.wad");
        if (internal.exists()) {
            return;
        }
        File externalDir = getExternalFilesDir(null);
        if (externalDir == null) {
            return;
        }
        String[] candidates = {"heretic.wad", "HERETIC.WAD", "heretic1.wad", "HERETIC1.WAD"};
        for (String name : candidates) {
            File src = new File(externalDir, name);
            if (src.exists() && src.length() > 1000000) {
                try {
                    java.nio.file.Files.copy(src.toPath(), internal.toPath());
                    Log.i(TAG, "Copied IWAD from " + src.getAbsolutePath());
                } catch (Exception e) {
                    Log.e(TAG, "Failed to copy IWAD", e);
                }
                return;
            }
        }
    }
}
