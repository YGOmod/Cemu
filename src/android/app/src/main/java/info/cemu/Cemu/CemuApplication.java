package info.cemu.Cemu;

import android.app.Application;
import android.util.DisplayMetrics;

import java.io.File;

import info.cemu.Cemu.nativeinterface.NativeEmulation;
import info.cemu.Cemu.nativeinterface.NativeGraphicPacks;
import info.cemu.Cemu.nativeinterface.NativeSwkbd;

public class CemuApplication extends Application {
    static {
        System.loadLibrary("CemuAndroid");
    }

    private static CemuApplication application;

    public CemuApplication() {
        application = this;
    }

    public static CemuApplication getApplication() {
        return application;
    }

    public File getInternalFolder() {
        var externalFilesDir = getExternalFilesDir(null);
        if (externalFilesDir != null) {
            return externalFilesDir;
        }
        return getFilesDir();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        NativeEmulation.setDPI(displayMetrics.density);
        NativeEmulation.initializeActiveSettings(getInternalFolder().toString(), getInternalFolder().toString());
        NativeEmulation.initializeEmulation();
        NativeSwkbd.initializeSwkbd();
        NativeGraphicPacks.refreshGraphicPacks();
    }
}
