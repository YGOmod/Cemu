package info.cemu.Cemu.nativeinterface;

public class NativeSwkbd {
    public static native void initializeSwkbd();

    public static native void setCurrentInputText(String text);

    public static native void onTextChanged(String text);

    public static native void onFinishedInputEdit();
}