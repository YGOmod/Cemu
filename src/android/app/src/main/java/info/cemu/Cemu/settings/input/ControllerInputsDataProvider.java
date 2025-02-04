package info.cemu.Cemu.settings.input;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import info.cemu.Cemu.R;
import info.cemu.Cemu.nativeinterface.NativeInput;

public class ControllerInputsDataProvider {
    private int getButtonResourceIdName(int controllerType, int buttonId) {
        if (controllerType == NativeInput.EMULATED_CONTROLLER_TYPE_VPAD) {
            return switch (buttonId) {
                case NativeInput.VPAD_BUTTON_A -> R.string.button_a;
                case NativeInput.VPAD_BUTTON_B -> R.string.button_b;
                case NativeInput.VPAD_BUTTON_X -> R.string.button_x;
                case NativeInput.VPAD_BUTTON_Y -> R.string.button_y;
                case NativeInput.VPAD_BUTTON_L -> R.string.button_l;
                case NativeInput.VPAD_BUTTON_R -> R.string.button_r;
                case NativeInput.VPAD_BUTTON_ZL -> R.string.button_zl;
                case NativeInput.VPAD_BUTTON_ZR -> R.string.button_zr;
                case NativeInput.VPAD_BUTTON_PLUS -> R.string.button_plus;
                case NativeInput.VPAD_BUTTON_MINUS -> R.string.button_minus;
                case NativeInput.VPAD_BUTTON_UP -> R.string.button_up;
                case NativeInput.VPAD_BUTTON_DOWN -> R.string.button_down;
                case NativeInput.VPAD_BUTTON_LEFT -> R.string.button_left;
                case NativeInput.VPAD_BUTTON_RIGHT -> R.string.button_right;
                case NativeInput.VPAD_BUTTON_STICKL -> R.string.button_stickl;
                case NativeInput.VPAD_BUTTON_STICKR -> R.string.button_stickr;
                case NativeInput.VPAD_BUTTON_STICKL_UP -> R.string.button_stickl_up;
                case NativeInput.VPAD_BUTTON_STICKL_DOWN -> R.string.button_stickl_down;
                case NativeInput.VPAD_BUTTON_STICKL_LEFT -> R.string.button_stickl_left;
                case NativeInput.VPAD_BUTTON_STICKL_RIGHT -> R.string.button_stickl_right;
                case NativeInput.VPAD_BUTTON_STICKR_UP -> R.string.button_stickr_up;
                case NativeInput.VPAD_BUTTON_STICKR_DOWN -> R.string.button_stickr_down;
                case NativeInput.VPAD_BUTTON_STICKR_LEFT -> R.string.button_stickr_left;
                case NativeInput.VPAD_BUTTON_STICKR_RIGHT -> R.string.button_stickr_right;
                case NativeInput.VPAD_BUTTON_MIC -> R.string.button_mic;
                case NativeInput.VPAD_BUTTON_SCREEN -> R.string.button_screen;
                case NativeInput.VPAD_BUTTON_HOME -> R.string.button_home;
                default -> throw new IllegalArgumentException("Invalid buttonId " + buttonId + " for controllerType " + controllerType);
            };
        }
        if (controllerType == NativeInput.EMULATED_CONTROLLER_TYPE_PRO) {
            return switch (buttonId) {
                case NativeInput.PRO_BUTTON_A -> R.string.button_a;
                case NativeInput.PRO_BUTTON_B -> R.string.button_b;
                case NativeInput.PRO_BUTTON_X -> R.string.button_x;
                case NativeInput.PRO_BUTTON_Y -> R.string.button_y;
                case NativeInput.PRO_BUTTON_L -> R.string.button_l;
                case NativeInput.PRO_BUTTON_R -> R.string.button_r;
                case NativeInput.PRO_BUTTON_ZL -> R.string.button_zl;
                case NativeInput.PRO_BUTTON_ZR -> R.string.button_zr;
                case NativeInput.PRO_BUTTON_PLUS -> R.string.button_plus;
                case NativeInput.PRO_BUTTON_MINUS -> R.string.button_minus;
                case NativeInput.PRO_BUTTON_HOME -> R.string.button_home;
                case NativeInput.PRO_BUTTON_UP -> R.string.button_up;
                case NativeInput.PRO_BUTTON_DOWN -> R.string.button_down;
                case NativeInput.PRO_BUTTON_LEFT -> R.string.button_left;
                case NativeInput.PRO_BUTTON_RIGHT -> R.string.button_right;
                case NativeInput.PRO_BUTTON_STICKL -> R.string.button_stickl;
                case NativeInput.PRO_BUTTON_STICKR -> R.string.button_stickr;
                case NativeInput.PRO_BUTTON_STICKL_UP -> R.string.button_stickl_up;
                case NativeInput.PRO_BUTTON_STICKL_DOWN -> R.string.button_stickl_down;
                case NativeInput.PRO_BUTTON_STICKL_LEFT -> R.string.button_stickl_left;
                case NativeInput.PRO_BUTTON_STICKL_RIGHT -> R.string.button_stickl_right;
                case NativeInput.PRO_BUTTON_STICKR_UP -> R.string.button_stickr_up;
                case NativeInput.PRO_BUTTON_STICKR_DOWN -> R.string.button_stickr_down;
                case NativeInput.PRO_BUTTON_STICKR_LEFT -> R.string.button_stickr_left;
                case NativeInput.PRO_BUTTON_STICKR_RIGHT -> R.string.button_stickr_right;
                default -> throw new IllegalArgumentException("Invalid buttonId " + buttonId + " for controllerType " + controllerType);
            };
        }
        if (controllerType == NativeInput.EMULATED_CONTROLLER_TYPE_CLASSIC) {
            return switch (buttonId) {
                case NativeInput.CLASSIC_BUTTON_A -> R.string.button_a;
                case NativeInput.CLASSIC_BUTTON_B -> R.string.button_b;
                case NativeInput.CLASSIC_BUTTON_X -> R.string.button_x;
                case NativeInput.CLASSIC_BUTTON_Y -> R.string.button_y;
                case NativeInput.CLASSIC_BUTTON_L -> R.string.button_l;
                case NativeInput.CLASSIC_BUTTON_R -> R.string.button_r;
                case NativeInput.CLASSIC_BUTTON_ZL -> R.string.button_zl;
                case NativeInput.CLASSIC_BUTTON_ZR -> R.string.button_zr;
                case NativeInput.CLASSIC_BUTTON_PLUS -> R.string.button_plus;
                case NativeInput.CLASSIC_BUTTON_MINUS -> R.string.button_minus;
                case NativeInput.CLASSIC_BUTTON_HOME -> R.string.button_home;
                case NativeInput.CLASSIC_BUTTON_UP -> R.string.button_up;
                case NativeInput.CLASSIC_BUTTON_DOWN -> R.string.button_down;
                case NativeInput.CLASSIC_BUTTON_LEFT -> R.string.button_left;
                case NativeInput.CLASSIC_BUTTON_RIGHT -> R.string.button_right;
                case NativeInput.CLASSIC_BUTTON_STICKL_UP -> R.string.button_stickl_up;
                case NativeInput.CLASSIC_BUTTON_STICKL_DOWN -> R.string.button_stickl_down;
                case NativeInput.CLASSIC_BUTTON_STICKL_LEFT -> R.string.button_stickl_left;
                case NativeInput.CLASSIC_BUTTON_STICKL_RIGHT -> R.string.button_stickl_right;
                case NativeInput.CLASSIC_BUTTON_STICKR_UP -> R.string.button_stickr_up;
                case NativeInput.CLASSIC_BUTTON_STICKR_DOWN -> R.string.button_stickr_down;
                case NativeInput.CLASSIC_BUTTON_STICKR_LEFT -> R.string.button_stickr_left;
                case NativeInput.CLASSIC_BUTTON_STICKR_RIGHT -> R.string.button_stickr_right;
                default -> throw new IllegalArgumentException("Invalid buttonId " + buttonId + " for controllerType " + controllerType);
            };
        }
        if (controllerType == NativeInput.EMULATED_CONTROLLER_TYPE_WIIMOTE) {
            return switch (buttonId) {
                case NativeInput.WIIMOTE_BUTTON_A -> R.string.button_a;
                case NativeInput.WIIMOTE_BUTTON_B -> R.string.button_b;
                case NativeInput.WIIMOTE_BUTTON_1 -> R.string.button_1;
                case NativeInput.WIIMOTE_BUTTON_2 -> R.string.button_2;
                case NativeInput.WIIMOTE_BUTTON_NUNCHUCK_Z -> R.string.button_nunchuck_z;
                case NativeInput.WIIMOTE_BUTTON_NUNCHUCK_C -> R.string.button_nunchuck_c;
                case NativeInput.WIIMOTE_BUTTON_PLUS -> R.string.button_plus;
                case NativeInput.WIIMOTE_BUTTON_MINUS -> R.string.button_minus;
                case NativeInput.WIIMOTE_BUTTON_UP -> R.string.button_up;
                case NativeInput.WIIMOTE_BUTTON_DOWN -> R.string.button_down;
                case NativeInput.WIIMOTE_BUTTON_LEFT -> R.string.button_left;
                case NativeInput.WIIMOTE_BUTTON_RIGHT -> R.string.button_right;
                case NativeInput.WIIMOTE_BUTTON_NUNCHUCK_UP -> R.string.button_nunchuck_up;
                case NativeInput.WIIMOTE_BUTTON_NUNCHUCK_DOWN -> R.string.button_nunchuck_down;
                case NativeInput.WIIMOTE_BUTTON_NUNCHUCK_LEFT -> R.string.button_nunchuck_left;
                case NativeInput.WIIMOTE_BUTTON_NUNCHUCK_RIGHT -> R.string.button_nunchuck_right;
                case NativeInput.WIIMOTE_BUTTON_HOME -> R.string.button_home;
                default -> throw new IllegalArgumentException("Invalid buttonId " + buttonId + " for controllerType " + controllerType);
            };
        }
        throw new IllegalArgumentException("Invalid controllerType " + controllerType);
    }

    public static class ControllerInput {
        public int buttonResourceIdName;
        public int buttonId;
        public String boundInput;

        public ControllerInput(int buttonResourceIdName, int buttonId, String boundInput) {
            this.boundInput = boundInput;
            this.buttonId = buttonId;
            this.buttonResourceIdName = buttonResourceIdName;
        }
    }

    public static class ControllerInputGroup {
        public int groupResourceIdName;
        public List<ControllerInput> inputs;

        public ControllerInputGroup(int groupResourceIdName, List<ControllerInput> inputs) {
            this.groupResourceIdName = groupResourceIdName;
            this.inputs = inputs;
        }
    }

    private void addControllerInputsGroup(List<ControllerInputGroup> controllerInputItems, int groupResourceIdName, int controllerType, List<Integer> buttons, Map<Integer, String> inputs) {
        List<ControllerInput> controllerInputs = buttons.stream().map(buttonId -> new ControllerInput(getButtonResourceIdName(controllerType, buttonId), buttonId, inputs.getOrDefault(buttonId, ""))).collect(Collectors.toList());
        controllerInputItems.add(new ControllerInputGroup(groupResourceIdName, controllerInputs));
    }

    public List<ControllerInputGroup> getControllerInputs(int controllerType, Map<Integer, String> inputs) {
        List<ControllerInputGroup> controllerInputItems = new ArrayList<>();
        if (controllerType == NativeInput.EMULATED_CONTROLLER_TYPE_VPAD) {
            addControllerInputsGroup(controllerInputItems, R.string.buttons, controllerType, List.of(NativeInput.VPAD_BUTTON_A, NativeInput.VPAD_BUTTON_B, NativeInput.VPAD_BUTTON_X, NativeInput.VPAD_BUTTON_Y, NativeInput.VPAD_BUTTON_L, NativeInput.VPAD_BUTTON_R, NativeInput.VPAD_BUTTON_ZL, NativeInput.VPAD_BUTTON_ZR, NativeInput.VPAD_BUTTON_PLUS, NativeInput.VPAD_BUTTON_MINUS), inputs);
            addControllerInputsGroup(controllerInputItems, R.string.d_pad, controllerType, List.of(NativeInput.VPAD_BUTTON_UP, NativeInput.VPAD_BUTTON_DOWN, NativeInput.VPAD_BUTTON_LEFT, NativeInput.VPAD_BUTTON_RIGHT), inputs);
            addControllerInputsGroup(controllerInputItems, R.string.left_axis, controllerType, List.of(NativeInput.VPAD_BUTTON_STICKL, NativeInput.VPAD_BUTTON_STICKL_UP, NativeInput.VPAD_BUTTON_STICKL_DOWN, NativeInput.VPAD_BUTTON_STICKL_LEFT, NativeInput.VPAD_BUTTON_STICKL_RIGHT), inputs);
            addControllerInputsGroup(controllerInputItems, R.string.right_axis, controllerType, List.of(NativeInput.VPAD_BUTTON_STICKR, NativeInput.VPAD_BUTTON_STICKR_UP, NativeInput.VPAD_BUTTON_STICKR_DOWN, NativeInput.VPAD_BUTTON_STICKR_LEFT, NativeInput.VPAD_BUTTON_STICKR_RIGHT), inputs);
            addControllerInputsGroup(controllerInputItems, R.string.extra, controllerType, List.of(NativeInput.VPAD_BUTTON_MIC, NativeInput.VPAD_BUTTON_SCREEN, NativeInput.VPAD_BUTTON_HOME), inputs);
        }
        if (controllerType == NativeInput.EMULATED_CONTROLLER_TYPE_PRO) {
            addControllerInputsGroup(controllerInputItems, R.string.buttons, controllerType, List.of(NativeInput.PRO_BUTTON_A, NativeInput.PRO_BUTTON_B, NativeInput.PRO_BUTTON_X, NativeInput.PRO_BUTTON_Y, NativeInput.PRO_BUTTON_L, NativeInput.PRO_BUTTON_R, NativeInput.PRO_BUTTON_ZL, NativeInput.PRO_BUTTON_ZR, NativeInput.PRO_BUTTON_PLUS, NativeInput.PRO_BUTTON_MINUS, NativeInput.PRO_BUTTON_HOME), inputs);
            addControllerInputsGroup(controllerInputItems, R.string.left_axis, controllerType, List.of(NativeInput.PRO_BUTTON_STICKL, NativeInput.PRO_BUTTON_STICKL_UP, NativeInput.PRO_BUTTON_STICKL_DOWN, NativeInput.PRO_BUTTON_STICKL_LEFT, NativeInput.PRO_BUTTON_STICKL_RIGHT), inputs);
            addControllerInputsGroup(controllerInputItems, R.string.right_axis, controllerType, List.of(NativeInput.PRO_BUTTON_STICKR, NativeInput.PRO_BUTTON_STICKR_UP, NativeInput.PRO_BUTTON_STICKR_DOWN, NativeInput.PRO_BUTTON_STICKR_LEFT, NativeInput.PRO_BUTTON_STICKR_RIGHT), inputs);
            addControllerInputsGroup(controllerInputItems, R.string.d_pad, controllerType, List.of(NativeInput.PRO_BUTTON_UP, NativeInput.PRO_BUTTON_DOWN, NativeInput.PRO_BUTTON_LEFT, NativeInput.PRO_BUTTON_RIGHT), inputs);
        }
        if (controllerType == NativeInput.EMULATED_CONTROLLER_TYPE_CLASSIC) {
            addControllerInputsGroup(controllerInputItems, R.string.buttons, controllerType, List.of(NativeInput.CLASSIC_BUTTON_A, NativeInput.CLASSIC_BUTTON_B, NativeInput.CLASSIC_BUTTON_X, NativeInput.CLASSIC_BUTTON_Y, NativeInput.CLASSIC_BUTTON_L, NativeInput.CLASSIC_BUTTON_R, NativeInput.CLASSIC_BUTTON_ZL, NativeInput.CLASSIC_BUTTON_ZR, NativeInput.CLASSIC_BUTTON_PLUS, NativeInput.CLASSIC_BUTTON_MINUS, NativeInput.CLASSIC_BUTTON_HOME), inputs);
            addControllerInputsGroup(controllerInputItems, R.string.left_axis, controllerType, List.of(NativeInput.CLASSIC_BUTTON_STICKL_UP, NativeInput.CLASSIC_BUTTON_STICKL_DOWN, NativeInput.CLASSIC_BUTTON_STICKL_LEFT, NativeInput.CLASSIC_BUTTON_STICKL_RIGHT), inputs);
            addControllerInputsGroup(controllerInputItems, R.string.right_axis, controllerType, List.of(NativeInput.CLASSIC_BUTTON_STICKR_UP, NativeInput.CLASSIC_BUTTON_STICKR_DOWN, NativeInput.CLASSIC_BUTTON_STICKR_LEFT, NativeInput.CLASSIC_BUTTON_STICKR_RIGHT), inputs);
            addControllerInputsGroup(controllerInputItems, R.string.d_pad, controllerType, List.of(NativeInput.CLASSIC_BUTTON_UP, NativeInput.CLASSIC_BUTTON_DOWN, NativeInput.CLASSIC_BUTTON_LEFT, NativeInput.CLASSIC_BUTTON_RIGHT), inputs);
        }
        if (controllerType == NativeInput.EMULATED_CONTROLLER_TYPE_WIIMOTE) {
            addControllerInputsGroup(controllerInputItems, R.string.buttons, controllerType, List.of(NativeInput.WIIMOTE_BUTTON_A, NativeInput.WIIMOTE_BUTTON_B, NativeInput.WIIMOTE_BUTTON_1, NativeInput.WIIMOTE_BUTTON_2, NativeInput.WIIMOTE_BUTTON_NUNCHUCK_Z, NativeInput.WIIMOTE_BUTTON_NUNCHUCK_C, NativeInput.WIIMOTE_BUTTON_PLUS, NativeInput.WIIMOTE_BUTTON_MINUS, NativeInput.WIIMOTE_BUTTON_HOME), inputs);
            addControllerInputsGroup(controllerInputItems, R.string.d_pad, controllerType, List.of(NativeInput.WIIMOTE_BUTTON_UP, NativeInput.WIIMOTE_BUTTON_DOWN, NativeInput.WIIMOTE_BUTTON_LEFT, NativeInput.WIIMOTE_BUTTON_RIGHT), inputs);
            addControllerInputsGroup(controllerInputItems, R.string.nunchuck, controllerType, List.of(NativeInput.WIIMOTE_BUTTON_NUNCHUCK_UP, NativeInput.WIIMOTE_BUTTON_NUNCHUCK_DOWN, NativeInput.WIIMOTE_BUTTON_NUNCHUCK_LEFT, NativeInput.WIIMOTE_BUTTON_NUNCHUCK_RIGHT), inputs);
        }
        return controllerInputItems;
    }

}
