package info.cemu.Cemu.settings.graphics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.List;

import info.cemu.Cemu.R;
import info.cemu.Cemu.databinding.LayoutGenericRecyclerViewBinding;
import info.cemu.Cemu.guibasecomponents.GenericRecyclerViewAdapter;
import info.cemu.Cemu.guibasecomponents.SingleSelectionRecyclerViewItem;
import info.cemu.Cemu.guibasecomponents.ToggleRecyclerViewItem;
import info.cemu.Cemu.nativeinterface.NativeSettings;

public class GraphicsSettingsFragment extends Fragment {
    private String vsyncModeToString(int vsyncMode) {
        int resourceId = switch (vsyncMode) {
            case NativeSettings.VSYNC_MODE_OFF -> R.string.off;
            case NativeSettings.VSYNC_MODE_DOUBLE_BUFFERING -> R.string.double_buffering;
            case NativeSettings.VSYNC_MODE_TRIPLE_BUFFERING -> R.string.triple_buffering;
            default -> throw new IllegalArgumentException("Invalid vsync mode: " + vsyncMode);
        };
        return getString(resourceId);
    }

    private String fullscreenScalingModeToString(int fullscreenScaling) {
        int resourceId = switch (fullscreenScaling) {
            case NativeSettings.FULLSCREEN_SCALING_KEEP_ASPECT_RATIO -> R.string.keep_aspect_ratio;
            case NativeSettings.FULLSCREEN_SCALING_STRETCH -> R.string.stretch;
            default -> throw new IllegalArgumentException("Invalid fullscreen scaling mode:  " + fullscreenScaling);
        };
        return getString(resourceId);
    }

    private String scalingFilterToString(int scalingFilter) {
        int resourceId = switch (scalingFilter) {
            case NativeSettings.SCALING_FILTER_BILINEAR_FILTER -> R.string.bilinear;
            case NativeSettings.SCALING_FILTER_BICUBIC_FILTER -> R.string.bicubic;
            case NativeSettings.SCALING_FILTER_BICUBIC_HERMITE_FILTER -> R.string.hermite;
            case NativeSettings.SCALING_FILTER_NEAREST_NEIGHBOR_FILTER -> R.string.nearest_neighbor;
            default -> throw new IllegalArgumentException("Invalid scaling filter:  " + scalingFilter);
        };
        return getString(resourceId);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        var binding = LayoutGenericRecyclerViewBinding.inflate(inflater, container, false);

        GenericRecyclerViewAdapter genericRecyclerViewAdapter = new GenericRecyclerViewAdapter();

        ToggleRecyclerViewItem asyncShaderToggle = new ToggleRecyclerViewItem(getString(R.string.async_shader_compile), getString(R.string.async_shader_compile_description), NativeSettings.getAsyncShaderCompile(), NativeSettings::setAsyncShaderCompile);
        genericRecyclerViewAdapter.addRecyclerViewItem(asyncShaderToggle);

        SingleSelectionRecyclerViewItem<Integer> vsyncModeSelection = new SingleSelectionRecyclerViewItem<>(getString(R.string.vsync),
                NativeSettings.getVSyncMode(),
                List.of(NativeSettings.VSYNC_MODE_OFF,
                        NativeSettings.VSYNC_MODE_DOUBLE_BUFFERING,
                        NativeSettings.VSYNC_MODE_TRIPLE_BUFFERING),
                this::vsyncModeToString,
                NativeSettings::setVSyncMode);
        genericRecyclerViewAdapter.addRecyclerViewItem(vsyncModeSelection);

        ToggleRecyclerViewItem accurateBarriersToggle = new ToggleRecyclerViewItem(getString(R.string.accurate_barriers), getString(R.string.accurate_barriers_description), NativeSettings.getAccurateBarriers(), NativeSettings::setAccurateBarriers);
        genericRecyclerViewAdapter.addRecyclerViewItem(accurateBarriersToggle);

        SingleSelectionRecyclerViewItem<Integer> fullscreenScalingSelection = new SingleSelectionRecyclerViewItem<>(getString(R.string.fullscreen_scaling),
                NativeSettings.getFullscreenScaling(),
                List.of(NativeSettings.FULLSCREEN_SCALING_KEEP_ASPECT_RATIO, NativeSettings.FULLSCREEN_SCALING_STRETCH),
                this::fullscreenScalingModeToString,
                NativeSettings::setFullscreenScaling);
        genericRecyclerViewAdapter.addRecyclerViewItem(fullscreenScalingSelection);

        var scalingFilterChoices = List.of(
                NativeSettings.SCALING_FILTER_BILINEAR_FILTER,
                NativeSettings.SCALING_FILTER_BICUBIC_FILTER,
                NativeSettings.SCALING_FILTER_BICUBIC_HERMITE_FILTER,
                NativeSettings.SCALING_FILTER_NEAREST_NEIGHBOR_FILTER
        );

        SingleSelectionRecyclerViewItem<Integer> upscaleFilterSelection = new SingleSelectionRecyclerViewItem<>(getString(R.string.upscale_filter),
                NativeSettings.getUpscalingFilter(),
                scalingFilterChoices,
                this::scalingFilterToString,
                NativeSettings::setUpscalingFilter);
        genericRecyclerViewAdapter.addRecyclerViewItem(upscaleFilterSelection);

        SingleSelectionRecyclerViewItem<Integer> downscaleFilterSelection = new SingleSelectionRecyclerViewItem<>(getString(R.string.downscale_filter),
                NativeSettings.getDownscalingFilter(),
                scalingFilterChoices,
                this::scalingFilterToString,
                NativeSettings::setDownscalingFilter);
        genericRecyclerViewAdapter.addRecyclerViewItem(downscaleFilterSelection);

        binding.recyclerView.setAdapter(genericRecyclerViewAdapter);
        return binding.getRoot();
    }
}