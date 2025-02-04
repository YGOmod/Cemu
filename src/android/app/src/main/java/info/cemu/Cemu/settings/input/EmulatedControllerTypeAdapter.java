package info.cemu.Cemu.settings.input;


import java.util.stream.Collectors;
import java.util.stream.Stream;

import info.cemu.Cemu.guibasecomponents.SelectionAdapter;
import info.cemu.Cemu.nativeinterface.NativeInput;

public class EmulatedControllerTypeAdapter extends SelectionAdapter<Integer> {
    private int vpadCount = 0;
    private int wpadCount = 0;

    public EmulatedControllerTypeAdapter() {
        choiceItems = Stream.of(NativeInput.EMULATED_CONTROLLER_TYPE_DISABLED,
                        NativeInput.EMULATED_CONTROLLER_TYPE_VPAD,
                        NativeInput.EMULATED_CONTROLLER_TYPE_PRO,
                        NativeInput.EMULATED_CONTROLLER_TYPE_CLASSIC,
                        NativeInput.EMULATED_CONTROLLER_TYPE_WIIMOTE)
                .map(type -> new ChoiceItem<>(t -> t.setText(ControllerTypeResourceNameMapper.controllerTypeToResourceNameId(type)), type))
                .collect(Collectors.toList());
        setSelectedValue(NativeInput.EMULATED_CONTROLLER_TYPE_DISABLED);
    }

    public void setControllerTypeCounts(int vpadCount, int wpadCount) {
        this.vpadCount = vpadCount;
        this.wpadCount = wpadCount;
        notifyDataSetChanged();
    }

    @Override
    public boolean isEnabled(int position) {
        int currentControllerType = getItem(selectedPosition);
        int type = getItem(position);
        if (type == NativeInput.EMULATED_CONTROLLER_TYPE_DISABLED) {
            return true;
        }
        if (type == NativeInput.EMULATED_CONTROLLER_TYPE_VPAD) {
            return currentControllerType == NativeInput.EMULATED_CONTROLLER_TYPE_VPAD || vpadCount < NativeInput.MAX_VPAD_CONTROLLERS;
        }
        boolean isWPAD = currentControllerType != NativeInput.EMULATED_CONTROLLER_TYPE_VPAD && currentControllerType != NativeInput.EMULATED_CONTROLLER_TYPE_DISABLED;
        return isWPAD || wpadCount < NativeInput.MAX_WPAD_CONTROLLERS;
    }
}
