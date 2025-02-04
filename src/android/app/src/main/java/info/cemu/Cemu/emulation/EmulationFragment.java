package info.cemu.Cemu.emulation;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;

import info.cemu.Cemu.R;
import info.cemu.Cemu.databinding.FragmentEmulationBinding;
import info.cemu.Cemu.input.SensorManager;
import info.cemu.Cemu.inputoverlay.InputOverlaySettingsProvider;
import info.cemu.Cemu.inputoverlay.InputOverlaySurfaceView;
import info.cemu.Cemu.nativeinterface.NativeEmulation;
import info.cemu.Cemu.nativeinterface.NativeException;
import info.cemu.Cemu.nativeinterface.NativeInput;

@SuppressLint("ClickableViewAccessibility")
public class EmulationFragment extends Fragment implements PopupMenu.OnMenuItemClickListener {
    static private class OnSurfaceTouchListener implements View.OnTouchListener {
        int currentPointerId = -1;
        final boolean isTV;

        public OnSurfaceTouchListener(boolean isTV) {
            this.isTV = isTV;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int pointerIndex = event.getActionIndex();
            int pointerId = event.getPointerId(pointerIndex);
            if (currentPointerId != -1 && pointerId != currentPointerId) {
                return false;
            }
            int x = (int) event.getX(pointerIndex);
            int y = (int) event.getY(pointerIndex);
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                    NativeInput.onTouchDown(x, y, isTV);
                    return true;
                }
                case MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    currentPointerId = -1;
                    NativeInput.onTouchUp(x, y, isTV);
                    return true;
                }
                case MotionEvent.ACTION_MOVE -> {
                    NativeInput.onTouchMove(x, y, isTV);
                    return true;
                }
            }
            return false;
        }
    }

    private class SurfaceHolderCallback implements SurfaceHolder.Callback {
        final boolean isMainCanvas;
        boolean surfaceSet;

        public SurfaceHolderCallback(boolean isMainCanvas) {
            this.isMainCanvas = isMainCanvas;
        }

        @Override
        public void surfaceCreated(@NonNull SurfaceHolder surfaceHolder) {
        }

        @Override
        public void surfaceChanged(@NonNull SurfaceHolder surfaceHolder, int format, int width, int height) {
            try {
                NativeEmulation.setSurfaceSize(width, height, isMainCanvas);
                if (surfaceSet) {
                    return;
                }
                NativeEmulation.setSurface(surfaceHolder.getSurface(), isMainCanvas);
                surfaceSet = true;
            } catch (NativeException exception) {
                onEmulationError(getString(R.string.failed_create_surface_error, exception.getMessage()));
            }
        }

        @Override
        public void surfaceDestroyed(@NonNull SurfaceHolder surfaceHolder) {
            NativeEmulation.clearSurface(isMainCanvas);
            surfaceSet = false;
        }
    }

    public interface OnEmulationErrorCallback {
        void onEmulationError(String errorMessage);
    }

    private final String launchPath;
    private boolean isGameRunning;
    private SurfaceView padCanvas;
    private Toast toast;
    private FragmentEmulationBinding binding;
    private boolean isMotionEnabled;
    private PopupMenu settingsMenu;
    private InputOverlaySurfaceView inputOverlaySurfaceView;
    private SensorManager sensorManager;
    private OnEmulationErrorCallback onEmulationErrorCallback;
    private boolean hasEmulationError;
    private InputOverlaySettingsProvider.OverlaySettings overlaySettings;

    public EmulationFragment(String launchPath) {
        this.launchPath = launchPath;
    }

    public void setOnEmulationErrorCallback(OnEmulationErrorCallback onEmulationErrorCallback) {
        this.onEmulationErrorCallback = onEmulationErrorCallback;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        var inputOverlaySettingsProvider = new InputOverlaySettingsProvider(requireContext());
        if (sensorManager == null) {
            sensorManager = new SensorManager(requireContext());
        }
        sensorManager.setIsLandscape(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);
        overlaySettings = inputOverlaySettingsProvider.getOverlaySettings();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (sensorManager != null) {
            sensorManager.setIsLandscape(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.pauseListening();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (isMotionEnabled) {
            sensorManager.startListening();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            sensorManager.pauseListening();
        }
    }

    private void createPadCanvas() {
        if (padCanvas != null) {
            return;
        }
        padCanvas = new SurfaceView(requireContext());
        binding.canvasesLayout.addView(
                padCanvas,
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f)
        );
        padCanvas.getHolder().addCallback(new SurfaceHolderCallback(false));
        padCanvas.setOnTouchListener(new OnSurfaceTouchListener(false));
    }

    private void toastMessage(@StringRes int toastTextResId) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(requireContext(), toastTextResId, Toast.LENGTH_SHORT);
        toast.show();
    }

    private void destroyPadCanvas() {
        if (padCanvas == null) {
            return;
        }
        binding.canvasesLayout.removeView(padCanvas);
        padCanvas = null;
    }

    private void setPadViewVisibility(boolean visible) {
        if (visible) {
            createPadCanvas();
            return;
        }
        destroyPadCanvas();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.show_pad) {
            boolean padVisibility = !item.isChecked();
            setPadViewVisibility(padVisibility);
            item.setChecked(padVisibility);
            return true;
        }
        if (itemId == R.id.edit_inputs) {
            binding.editInputsLayout.setVisibility(View.VISIBLE);
            binding.finishEditInputsButton.setVisibility(View.VISIBLE);
            binding.moveInputsButton.performClick();
            return true;
        }
        if (itemId == R.id.replace_tv_with_pad) {
            boolean replaceTVWithPad = !item.isChecked();
            NativeEmulation.setReplaceTVWithPadView(replaceTVWithPad);
            item.setChecked(replaceTVWithPad);
            return true;
        }
        if (itemId == R.id.reset_inputs) {
            inputOverlaySurfaceView.resetInputs();
            return true;
        }
        if (itemId == R.id.enable_motion) {
            isMotionEnabled = !item.isChecked();
            if (isMotionEnabled) {
                sensorManager.startListening();
            } else {
                sensorManager.pauseListening();
            }
            item.setChecked(isMotionEnabled);
            return true;
        }
        if (itemId == R.id.exit_emulation) {
            requireActivity().getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        if (itemId == R.id.show_input_overlay) {
            boolean showInputOverlay = !item.isChecked();
            var menu = settingsMenu.getMenu();
            menu.findItem(R.id.edit_inputs).setEnabled(showInputOverlay);
            menu.findItem(R.id.reset_inputs).setEnabled(showInputOverlay);
            item.setChecked(showInputOverlay);
            inputOverlaySurfaceView.setVisibility(showInputOverlay ? View.VISIBLE : View.GONE);
            return true;
        }
        return false;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentEmulationBinding.inflate(inflater, container, false);
        inputOverlaySurfaceView = binding.inputOverlay;

        binding.moveInputsButton.setOnClickListener(v -> {
            if (inputOverlaySurfaceView.getInputMode() == InputOverlaySurfaceView.InputMode.EDIT_POSITION) {
                return;
            }
            binding.resizeInputsButton.setAlpha(0.5f);
            binding.moveInputsButton.setAlpha(1.0f);
            toastMessage(R.string.input_mode_edit_position);
            inputOverlaySurfaceView.setInputMode(InputOverlaySurfaceView.InputMode.EDIT_POSITION);
        });
        binding.resizeInputsButton.setOnClickListener(v -> {
            if (inputOverlaySurfaceView.getInputMode() == InputOverlaySurfaceView.InputMode.EDIT_SIZE) {
                return;
            }
            binding.moveInputsButton.setAlpha(0.5f);
            binding.resizeInputsButton.setAlpha(1.0f);
            toastMessage(R.string.input_mode_edit_size);
            inputOverlaySurfaceView.setInputMode(InputOverlaySurfaceView.InputMode.EDIT_SIZE);
        });
        binding.finishEditInputsButton.setOnClickListener(v -> {
            inputOverlaySurfaceView.setInputMode(InputOverlaySurfaceView.InputMode.DEFAULT);
            binding.finishEditInputsButton.setVisibility(View.GONE);
            binding.editInputsLayout.setVisibility(View.GONE);
            toastMessage(R.string.input_mode_default);
        });
        settingsMenu = new PopupMenu(requireContext(), binding.emulationSettingsButton);
        settingsMenu.getMenuInflater().inflate(R.menu.emulation, settingsMenu.getMenu());
        settingsMenu.setOnMenuItemClickListener(EmulationFragment.this);
        binding.emulationSettingsButton.setOnClickListener(v -> settingsMenu.show());
        var menu = settingsMenu.getMenu();
        menu.findItem(R.id.show_input_overlay).setChecked(overlaySettings.isOverlayEnabled());
        if (!overlaySettings.isOverlayEnabled()) {
            menu.findItem(R.id.reset_inputs).setEnabled(false);
            menu.findItem(R.id.edit_inputs).setEnabled(false);
            inputOverlaySurfaceView.setVisibility(View.GONE);
        }
        SurfaceView mainCanvas = binding.mainCanvas;
        try {
            SurfaceTexture testSurfaceTexture = new SurfaceTexture(0);
            Surface testSurface = new Surface(testSurfaceTexture);
            NativeEmulation.initializeRenderer(testSurface);
            testSurface.release();
            testSurfaceTexture.release();
        } catch (NativeException exception) {
            onEmulationError(getString(R.string.failed_initialize_renderer_error, exception.getMessage()));
            return binding.getRoot();
        }

        var mainCanvasHolder = mainCanvas.getHolder();
        mainCanvasHolder.addCallback(new SurfaceHolderCallback(true));
        mainCanvasHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(@NonNull SurfaceHolder holder) {
            }

            @Override
            public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
                if (hasEmulationError) {
                    return;
                }
                if (!isGameRunning) {
                    isGameRunning = true;
                    startGame();
                }
            }

            @Override
            public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
            }
        });
        mainCanvas.setOnTouchListener(new OnSurfaceTouchListener(true));
        return binding.getRoot();
    }

    private void startGame() {
        int result = NativeEmulation.startGame(launchPath);
        if (result == NativeEmulation.START_GAME_SUCCESSFUL) {
            return;
        }
        String errorMessage = switch (result) {
            case NativeEmulation.START_GAME_ERROR_GAME_BASE_FILES_NOT_FOUND -> getString(R.string.game_not_found);
            case NativeEmulation.START_GAME_ERROR_NO_DISC_KEY -> getString(R.string.no_disk_key);
            case NativeEmulation.START_GAME_ERROR_NO_TITLE_TIK -> getString(R.string.no_title_tik);
            default -> getString(R.string.game_files_unknown_error, launchPath);
        };
        onEmulationError(errorMessage);
    }

    private void onEmulationError(String errorMessage) {
        hasEmulationError = true;
        if (onEmulationErrorCallback != null) {
            onEmulationErrorCallback.onEmulationError(errorMessage);
        }
    }
}