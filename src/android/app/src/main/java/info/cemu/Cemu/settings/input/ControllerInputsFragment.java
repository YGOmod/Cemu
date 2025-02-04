package info.cemu.Cemu.settings.input;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.HashMap;
import java.util.Map;

import info.cemu.Cemu.R;
import info.cemu.Cemu.databinding.LayoutGenericRecyclerViewBinding;
import info.cemu.Cemu.guibasecomponents.GenericRecyclerViewAdapter;
import info.cemu.Cemu.guibasecomponents.HeaderRecyclerViewItem;
import info.cemu.Cemu.guibasecomponents.SingleSelectionRecyclerViewItem;
import info.cemu.Cemu.input.InputManager;
import info.cemu.Cemu.nativeinterface.NativeInput;

public class ControllerInputsFragment extends Fragment {
    public static final String CONTROLLER_INDEX = "ControllerIndex";
    private int controllerIndex;
    private int controllerType;
    private final InputManager inputManager = new InputManager();
    private final ControllerInputsDataProvider controllerInputsDataProvider = new ControllerInputsDataProvider();
    private final GenericRecyclerViewAdapter genericRecyclerViewAdapter = new GenericRecyclerViewAdapter();
    private final EmulatedControllerTypeAdapter emulatedControllerTypeAdapter = new EmulatedControllerTypeAdapter();

    private void onTypeChanged(int controllerType) {
        if (this.controllerType == controllerType) {
            return;
        }
        this.controllerType = controllerType;
        NativeInput.setControllerType(controllerIndex, controllerType);
        genericRecyclerViewAdapter.clearRecyclerViewItems();
        setControllerInputs(new HashMap<>());
    }

    private void setControllerInputs(Map<Integer, String> inputs) {
        emulatedControllerTypeAdapter.setSelectedValue(controllerType);
        emulatedControllerTypeAdapter.setControllerTypeCounts(NativeInput.getVPADControllersCount(), NativeInput.getWPADControllersCount());
        String controllerTypeName = getString(ControllerTypeResourceNameMapper.controllerTypeToResourceNameId(controllerType));

        SingleSelectionRecyclerViewItem<Integer> emulatedControllerSelection = new SingleSelectionRecyclerViewItem<>(getString(R.string.emulated_controller_label),
                getString(R.string.emulated_controller_selection_description, controllerTypeName),
                emulatedControllerTypeAdapter,
                (controllerType, selectionRecyclerViewItem) -> onTypeChanged(controllerType));
        genericRecyclerViewAdapter.addRecyclerViewItem(emulatedControllerSelection);
        for (var controllerInputsGroups : controllerInputsDataProvider.getControllerInputs(controllerType, inputs)) {
            genericRecyclerViewAdapter.addRecyclerViewItem(new HeaderRecyclerViewItem(controllerInputsGroups.groupResourceIdName));
            for (var controllerInput : controllerInputsGroups.inputs) {
                int buttonId = controllerInput.buttonId;
                int buttonResourceIdName = controllerInput.buttonResourceIdName;
                InputRecyclerViewItem inputRecyclerViewItem = new InputRecyclerViewItem(controllerInput.buttonResourceIdName, controllerInput.boundInput, inputItem -> {
                    var inputDialog = new MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.inputBindingDialogTitle)
                            .setMessage(getString(R.string.inputBindingDialogMessage, getString(buttonResourceIdName)))
                            .setNeutralButton(getString(R.string.clear), (dialogInterface, i) -> {
                                NativeInput.clearControllerMapping(controllerIndex, buttonId);
                                inputItem.clearBoundInput();
                                dialogInterface.dismiss();
                            })
                            .setNegativeButton(getString(R.string.cancel), (dialogInterface, i) -> dialogInterface.dismiss())
                            .show();
                    TextView messageTextView = inputDialog.requireViewById(android.R.id.message);
                    messageTextView.setFocusableInTouchMode(true);
                    messageTextView.requestFocus();
                    messageTextView.setOnKeyListener((v, keyCode, event) -> {
                        if (inputManager.mapKeyEventToMappingId(controllerIndex, buttonId, event)) {
                            inputItem.setBoundInput(NativeInput.getControllerMapping(controllerIndex, buttonId));
                            inputDialog.dismiss();
                        }
                        return true;
                    });
                    messageTextView.setOnGenericMotionListener((v, event) -> {
                        if (inputManager.mapMotionEventToMappingId(controllerIndex, buttonId, event)) {
                            inputItem.setBoundInput(NativeInput.getControllerMapping(controllerIndex, buttonId));
                            inputDialog.dismiss();
                        }
                        return true;
                    });
                });
                genericRecyclerViewAdapter.addRecyclerViewItem(inputRecyclerViewItem);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        controllerIndex = requireArguments().getInt(CONTROLLER_INDEX);
        ActionBar actionBar = ((AppCompatActivity) requireActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.controller_numbered, controllerIndex + 1));
        }
        if (NativeInput.isControllerDisabled(controllerIndex)) {
            controllerType = NativeInput.EMULATED_CONTROLLER_TYPE_DISABLED;
        } else {
            controllerType = NativeInput.getControllerType(controllerIndex);
        }

        setControllerInputs(NativeInput.getControllerMappings(controllerIndex));

        var binding = LayoutGenericRecyclerViewBinding.inflate(inflater, container, false);
        binding.recyclerView.setAdapter(genericRecyclerViewAdapter);
        return binding.getRoot();
    }
}