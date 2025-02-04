package info.cemu.Cemu.input;

import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import info.cemu.Cemu.nativeinterface.NativeInput;

public class InputManager {
    private static class InvalidAxisException extends Exception {
        public InvalidAxisException(int axis) {
            super("Invalid axis " + axis);
        }
    }

    private int getNativeAxisKey(int axis, boolean isPositive) throws InvalidAxisException {
        if (isPositive) {
            return switch (axis) {
                case MotionEvent.AXIS_X -> NativeInput.AXIS_X_POS;
                case MotionEvent.AXIS_Y -> NativeInput.AXIS_Y_POS;
                case MotionEvent.AXIS_RX, MotionEvent.AXIS_Z -> NativeInput.ROTATION_X_POS;
                case MotionEvent.AXIS_RY, MotionEvent.AXIS_RZ -> NativeInput.ROTATION_Y_POS;
                case MotionEvent.AXIS_LTRIGGER -> NativeInput.TRIGGER_X_POS;
                case MotionEvent.AXIS_RTRIGGER -> NativeInput.TRIGGER_Y_POS;
                case MotionEvent.AXIS_HAT_X -> NativeInput.DPAD_RIGHT;
                case MotionEvent.AXIS_HAT_Y -> NativeInput.DPAD_DOWN;
                default -> throw new InvalidAxisException(axis);
            };
        } else {
            return switch (axis) {
                case MotionEvent.AXIS_X -> NativeInput.AXIS_X_NEG;
                case MotionEvent.AXIS_Y -> NativeInput.AXIS_Y_NEG;
                case MotionEvent.AXIS_RX, MotionEvent.AXIS_Z -> NativeInput.ROTATION_X_NEG;
                case MotionEvent.AXIS_RY, MotionEvent.AXIS_RZ -> NativeInput.ROTATION_Y_NEG;
                case MotionEvent.AXIS_HAT_X -> NativeInput.DPAD_LEFT;
                case MotionEvent.AXIS_HAT_Y -> NativeInput.DPAD_UP;
                default -> throw new InvalidAxisException(axis);
            };
        }
    }

    private boolean isMotionEventFromJoystickOrGamepad(MotionEvent event) {
        return (event.getSource() & InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK || (event.getSource() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD;
    }

    private static final float MIN_ABS_AXIS_VALUE = 0.33f;

    public boolean mapMotionEventToMappingId(int controllerIndex, int mappingId, MotionEvent event) {
        if (!isMotionEventFromJoystickOrGamepad(event)) {
            return false;
        }
        InputDevice device = event.getDevice();
        float maxAbsAxisValue = 0.0f;
        int maxAxis = -1;
        int actionPointerIndex = event.getActionIndex();
        for (InputDevice.MotionRange motionRange : device.getMotionRanges()) {
            float axisValue = event.getAxisValue(motionRange.getAxis(), actionPointerIndex);
            int axis;
            try {
                axis = getNativeAxisKey(motionRange.getAxis(), axisValue > 0);
            } catch (InvalidAxisException e) {
                continue;
            }
            if (Math.abs(axisValue) > maxAbsAxisValue) {
                maxAxis = axis;
                maxAbsAxisValue = Math.abs(axisValue);
            }
        }
        if (maxAbsAxisValue > MIN_ABS_AXIS_VALUE) {
            NativeInput.setControllerMapping(device.getDescriptor(), device.getName(), controllerIndex, mappingId, maxAxis);
            return true;
        }
        return false;
    }

    private boolean isSpecialKey(KeyEvent event) {
        int keyCode = event.getKeyCode();
        return keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
                keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
                keyCode == KeyEvent.KEYCODE_CAMERA ||
                keyCode == KeyEvent.KEYCODE_ZOOM_IN ||
                keyCode == KeyEvent.KEYCODE_ZOOM_OUT;
    }

    private boolean isController(InputDevice inputDevice) {

        int sources = inputDevice.getSources();
        return (sources & InputDevice.SOURCE_CLASS_JOYSTICK) != 0 ||
                ((sources & InputDevice.SOURCE_DPAD) == InputDevice.SOURCE_DPAD) ||
                ((sources & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD);
    }

    public boolean onKeyEvent(KeyEvent event) {
        if (isSpecialKey(event)) {
            return false;
        }
        if (event.getDeviceId() < 0) {
            return false;
        }
        InputDevice device = event.getDevice();
        if (!isController(device)) {
            return false;
        }
        NativeInput.onNativeKey(device.getDescriptor(), device.getName(), event.getKeyCode(), event.getAction() == KeyEvent.ACTION_DOWN);
        return true;
    }

    public boolean onMotionEvent(MotionEvent event) {
        if (!isMotionEventFromJoystickOrGamepad(event)) {
            return false;
        }
        InputDevice device = event.getDevice();
        int actionPointerIndex = event.getActionIndex();
        for (InputDevice.MotionRange motionRange : device.getMotionRanges()) {
            float axisValue = event.getAxisValue(motionRange.getAxis(), actionPointerIndex);
            int axis = motionRange.getAxis();
            NativeInput.onNativeAxis(device.getDescriptor(), device.getName(), axis, axisValue);
        }
        return true;
    }

    public boolean mapKeyEventToMappingId(int controllerIndex, int mappingId, KeyEvent event) {
        InputDevice device = event.getDevice();
        NativeInput.setControllerMapping(device.getDescriptor(), device.getName(), controllerIndex, mappingId, event.getKeyCode());
        return true;
    }
}
