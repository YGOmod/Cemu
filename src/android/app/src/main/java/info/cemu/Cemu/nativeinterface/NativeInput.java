package info.cemu.Cemu.nativeinterface;

import java.util.Map;

public class NativeInput {
    public static final int VPAD_BUTTON_NONE = 0;
    public static final int VPAD_BUTTON_A = 1;
    public static final int VPAD_BUTTON_B = 2;
    public static final int VPAD_BUTTON_X = 3;
    public static final int VPAD_BUTTON_Y = 4;
    public static final int VPAD_BUTTON_L = 5;
    public static final int VPAD_BUTTON_R = 6;
    public static final int VPAD_BUTTON_ZL = 7;
    public static final int VPAD_BUTTON_ZR = 8;
    public static final int VPAD_BUTTON_PLUS = 9;
    public static final int VPAD_BUTTON_MINUS = 10;
    public static final int VPAD_BUTTON_UP = 11;
    public static final int VPAD_BUTTON_DOWN = 12;
    public static final int VPAD_BUTTON_LEFT = 13;
    public static final int VPAD_BUTTON_RIGHT = 14;
    public static final int VPAD_BUTTON_STICKL = 15;
    public static final int VPAD_BUTTON_STICKR = 16;
    public static final int VPAD_BUTTON_STICKL_UP = 17;
    public static final int VPAD_BUTTON_STICKL_DOWN = 18;
    public static final int VPAD_BUTTON_STICKL_LEFT = 19;
    public static final int VPAD_BUTTON_STICKL_RIGHT = 20;
    public static final int VPAD_BUTTON_STICKR_UP = 21;
    public static final int VPAD_BUTTON_STICKR_DOWN = 22;
    public static final int VPAD_BUTTON_STICKR_LEFT = 23;
    public static final int VPAD_BUTTON_STICKR_RIGHT = 24;
    public static final int VPAD_BUTTON_MIC = 25;
    public static final int VPAD_BUTTON_SCREEN = 26;
    public static final int VPAD_BUTTON_HOME = 27;
    public static final int VPAD_BUTTON_MAX = 28;

    public static final int PRO_BUTTON_NONE = 0;
    public static final int PRO_BUTTON_A = 1;
    public static final int PRO_BUTTON_B = 2;
    public static final int PRO_BUTTON_X = 3;
    public static final int PRO_BUTTON_Y = 4;
    public static final int PRO_BUTTON_L = 5;
    public static final int PRO_BUTTON_R = 6;
    public static final int PRO_BUTTON_ZL = 7;
    public static final int PRO_BUTTON_ZR = 8;
    public static final int PRO_BUTTON_PLUS = 9;
    public static final int PRO_BUTTON_MINUS = 10;
    public static final int PRO_BUTTON_HOME = 11;
    public static final int PRO_BUTTON_UP = 12;
    public static final int PRO_BUTTON_DOWN = 13;
    public static final int PRO_BUTTON_LEFT = 14;
    public static final int PRO_BUTTON_RIGHT = 15;
    public static final int PRO_BUTTON_STICKL = 16;
    public static final int PRO_BUTTON_STICKR = 17;
    public static final int PRO_BUTTON_STICKL_UP = 18;
    public static final int PRO_BUTTON_STICKL_DOWN = 19;
    public static final int PRO_BUTTON_STICKL_LEFT = 20;
    public static final int PRO_BUTTON_STICKL_RIGHT = 21;
    public static final int PRO_BUTTON_STICKR_UP = 22;
    public static final int PRO_BUTTON_STICKR_DOWN = 23;
    public static final int PRO_BUTTON_STICKR_LEFT = 24;
    public static final int PRO_BUTTON_STICKR_RIGHT = 25;
    public static final int PRO_BUTTON_MAX = 26;

    public static final int CLASSIC_BUTTON_NONE = 0;
    public static final int CLASSIC_BUTTON_A = 1;
    public static final int CLASSIC_BUTTON_B = 2;
    public static final int CLASSIC_BUTTON_X = 3;
    public static final int CLASSIC_BUTTON_Y = 4;
    public static final int CLASSIC_BUTTON_L = 5;
    public static final int CLASSIC_BUTTON_R = 6;
    public static final int CLASSIC_BUTTON_ZL = 7;
    public static final int CLASSIC_BUTTON_ZR = 8;
    public static final int CLASSIC_BUTTON_PLUS = 9;
    public static final int CLASSIC_BUTTON_MINUS = 10;
    public static final int CLASSIC_BUTTON_HOME = 11;
    public static final int CLASSIC_BUTTON_UP = 12;
    public static final int CLASSIC_BUTTON_DOWN = 13;
    public static final int CLASSIC_BUTTON_LEFT = 14;
    public static final int CLASSIC_BUTTON_RIGHT = 15;
    public static final int CLASSIC_BUTTON_STICKL_UP = 16;
    public static final int CLASSIC_BUTTON_STICKL_DOWN = 17;
    public static final int CLASSIC_BUTTON_STICKL_LEFT = 18;
    public static final int CLASSIC_BUTTON_STICKL_RIGHT = 19;
    public static final int CLASSIC_BUTTON_STICKR_UP = 20;
    public static final int CLASSIC_BUTTON_STICKR_DOWN = 21;
    public static final int CLASSIC_BUTTON_STICKR_LEFT = 22;
    public static final int CLASSIC_BUTTON_STICKR_RIGHT = 23;
    public static final int CLASSIC_BUTTON_MAX = 24;

    public static final int WIIMOTE_BUTTON_NONE = 0;
    public static final int WIIMOTE_BUTTON_A = 1;
    public static final int WIIMOTE_BUTTON_B = 2;
    public static final int WIIMOTE_BUTTON_1 = 3;
    public static final int WIIMOTE_BUTTON_2 = 4;
    public static final int WIIMOTE_BUTTON_NUNCHUCK_Z = 5;
    public static final int WIIMOTE_BUTTON_NUNCHUCK_C = 6;
    public static final int WIIMOTE_BUTTON_PLUS = 7;
    public static final int WIIMOTE_BUTTON_MINUS = 8;
    public static final int WIIMOTE_BUTTON_UP = 9;
    public static final int WIIMOTE_BUTTON_DOWN = 10;
    public static final int WIIMOTE_BUTTON_LEFT = 11;
    public static final int WIIMOTE_BUTTON_RIGHT = 12;
    public static final int WIIMOTE_BUTTON_NUNCHUCK_UP = 13;
    public static final int WIIMOTE_BUTTON_NUNCHUCK_DOWN = 14;
    public static final int WIIMOTE_BUTTON_NUNCHUCK_LEFT = 15;
    public static final int WIIMOTE_BUTTON_NUNCHUCK_RIGHT = 16;
    public static final int WIIMOTE_BUTTON_HOME = 17;
    public static final int WIIMOTE_BUTTON_MAX = 18;

    public static final int EMULATED_CONTROLLER_TYPE_VPAD = 0;
    public static final int EMULATED_CONTROLLER_TYPE_PRO = 1;
    public static final int EMULATED_CONTROLLER_TYPE_CLASSIC = 2;
    public static final int EMULATED_CONTROLLER_TYPE_WIIMOTE = 3;
    public static final int EMULATED_CONTROLLER_TYPE_DISABLED = -1;

    public static final int DPAD_UP = 34;
    public static final int DPAD_DOWN = 35;
    public static final int DPAD_LEFT = 36;
    public static final int DPAD_RIGHT = 37;
    public static final int AXIS_X_POS = 38;
    public static final int AXIS_Y_POS = 39;
    public static final int ROTATION_X_POS = 40;
    public static final int ROTATION_Y_POS = 41;
    public static final int TRIGGER_X_POS = 42;
    public static final int TRIGGER_Y_POS = 43;
    public static final int AXIS_X_NEG = 44;
    public static final int AXIS_Y_NEG = 45;
    public static final int ROTATION_X_NEG = 46;
    public static final int ROTATION_Y_NEG = 47;

    public static final int MAX_CONTROLLERS = 8;
    public static final int MAX_VPAD_CONTROLLERS = 2;
    public static final int MAX_WPAD_CONTROLLERS = 7;

    public static native void onNativeKey(String deviceDescriptor, String deviceName, int key, boolean isPressed);

    public static native void onNativeAxis(String deviceDescriptor, String deviceName, int axis, float value);

    public static native void setControllerType(int index, int emulatedControllerType);

    public static native boolean isControllerDisabled(int index);

    public static native int getControllerType(int index);

    public static native int getWPADControllersCount();

    public static native int getVPADControllersCount();

    public static native void setControllerMapping(String deviceDescriptor, String deviceName, int index, int mappingId, int buttonId);

    public static native void clearControllerMapping(int index, int mappingId);

    public static native String getControllerMapping(int index, int mappingId);

    public static native Map<Integer, String> getControllerMappings(int index);

    public static native void onTouchDown(int x, int y, boolean isTV);

    public static native void onTouchMove(int x, int y, boolean isTV);

    public static native void onTouchUp(int x, int y, boolean isTV);

    public static native void onMotion(long timestamp, float gyroX, float gyroY, float gyroZ, float accelX, float accelY, float accelZ);

    public static native void setMotionEnabled(boolean motionEnabled);

    public static native void onOverlayButton(int controllerIndex, int mappingId, boolean value);

    public static native void onOverlayAxis(int controllerIndex, int mappingId, float value);
}
