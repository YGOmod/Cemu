package info.cemu.Cemu.nativeinterface;

import android.view.Surface;

public class NativeEmulation {
    public static native void initializeActiveSettings(String dataPath, String cachePath);

    public static native void initializeEmulation();

    public static native void setDPI(float dpi);

    public static native void setSurface(Surface surface, boolean isMainCanvas);

    public static native void clearSurface(boolean isMainCanvas);

    public static native void setSurfaceSize(int width, int height, boolean isMainCanvas);

    public static native void initializeRenderer(Surface surface);

    public static final int START_GAME_SUCCESSFUL = 0;
    public static final int START_GAME_ERROR_GAME_BASE_FILES_NOT_FOUND = 1;
    public static final int START_GAME_ERROR_NO_DISC_KEY = 2;
    public static final int START_GAME_ERROR_NO_TITLE_TIK = 3;
    public static final int START_GAME_ERROR_UNKNOWN = 4;

    public static native int startGame(String launchPath);

    public static native void setReplaceTVWithPadView(boolean swapped);

    public static native void recreateRenderSurface(boolean isMainCanvas);
}
