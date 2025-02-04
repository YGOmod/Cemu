package info.cemu.Cemu.gameview;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import info.cemu.Cemu.R;
import info.cemu.Cemu.databinding.FragmentGameProfileEditBinding;
import info.cemu.Cemu.guibasecomponents.GenericRecyclerViewAdapter;
import info.cemu.Cemu.guibasecomponents.HeaderRecyclerViewItem;
import info.cemu.Cemu.guibasecomponents.SingleSelectionRecyclerViewItem;
import info.cemu.Cemu.guibasecomponents.ToggleRecyclerViewItem;
import info.cemu.Cemu.nativeinterface.NativeGameTitles;

public class GameProfileEditFragment extends Fragment {

    private String cpuModeToString(int cpuMode) {
        int resourceId = switch (cpuMode) {
            case NativeGameTitles.CPU_MODE_SINGLECOREINTERPRETER -> R.string.cpu_mode_single_core_interpreter;
            case NativeGameTitles.CPU_MODE_SINGLECORERECOMPILER -> R.string.cpu_mode_single_core_recompiler;
            case NativeGameTitles.CPU_MODE_MULTICORERECOMPILER -> R.string.cpu_mode_multi_core_recompiler;
            default -> R.string.cpu_mode_auto;
        };
        return getString(resourceId);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentGameProfileEditBinding binding = FragmentGameProfileEditBinding.inflate(inflater, container, false);
        var game = new ViewModelProvider(requireActivity()).get(GameViewModel.class).getGame();
        long titleId = game.titleId();

        GenericRecyclerViewAdapter genericRecyclerViewAdapter = new GenericRecyclerViewAdapter();

        genericRecyclerViewAdapter.addRecyclerViewItem(new HeaderRecyclerViewItem(game.name()));

        ToggleRecyclerViewItem loadSharedLibrariesToggle = new ToggleRecyclerViewItem("Load shared libraries",
                "Load libraries from the cafeLibs directory", NativeGameTitles.isLoadingSharedLibrariesForTitleEnabled(titleId),
                checked -> NativeGameTitles.setLoadingSharedLibrariesForTitleEnabled(titleId, checked));
        genericRecyclerViewAdapter.addRecyclerViewItem(loadSharedLibrariesToggle);

        ToggleRecyclerViewItem shaderMultiplicationAccuracyToggle = new ToggleRecyclerViewItem("Shader multiplication accuracy",
                "Controls the accuracy of floating point multiplication in shaders", NativeGameTitles.isShaderMultiplicationAccuracyForTitleEnabled(titleId),
                checked -> NativeGameTitles.setShaderMultiplicationAccuracyForTitleEnabled(titleId, checked));
        genericRecyclerViewAdapter.addRecyclerViewItem(shaderMultiplicationAccuracyToggle);

        SingleSelectionRecyclerViewItem<Integer> cpuModeSelection = new SingleSelectionRecyclerViewItem<>(getString(R.string.cpu_mode),
                NativeGameTitles.getCpuModeForTitle(titleId),
                List.of(NativeGameTitles.CPU_MODE_SINGLECOREINTERPRETER,
                        NativeGameTitles.CPU_MODE_SINGLECORERECOMPILER,
                        NativeGameTitles.CPU_MODE_MULTICORERECOMPILER,
                        NativeGameTitles.CPU_MODE_AUTO),
                this::cpuModeToString,
                cpuMode -> NativeGameTitles.setCpuModeForTitle(titleId, cpuMode));
        genericRecyclerViewAdapter.addRecyclerViewItem(cpuModeSelection);

        SingleSelectionRecyclerViewItem<Integer> threadQuantumSelection = new SingleSelectionRecyclerViewItem<>(getString(R.string.thread_quantum),
                NativeGameTitles.getThreadQuantumForTitle(titleId),
                Arrays.stream(NativeGameTitles.THREAD_QUANTUM_VALUES).boxed().collect(Collectors.toList()),
                String::valueOf,
                threadQuantum -> NativeGameTitles.setThreadQuantumForTitle(titleId, threadQuantum));
        genericRecyclerViewAdapter.addRecyclerViewItem(threadQuantumSelection);

        binding.recyclerView.setAdapter(genericRecyclerViewAdapter);
        NavigationUI.setupWithNavController(binding.gameEditProfileToolbar, NavHostFragment.findNavController(this), new AppBarConfiguration.Builder().build());
        return binding.getRoot();
    }
}
