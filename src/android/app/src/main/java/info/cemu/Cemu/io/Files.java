package info.cemu.Cemu.io;

import java.io.File;
import java.io.IOException;

public class Files {
    public static void delete(File file) throws IOException {
        var files = file.listFiles();
        if (files == null) {
            var ignored = file.delete();
            return;
        }
        for (var childFile : files) {
            delete(childFile);
        }
        var ignored = file.delete();
    }
}
