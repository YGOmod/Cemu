package info.cemu.Cemu.emulation;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.KeyEvent;
import android.view.MotionEvent;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;

import info.cemu.Cemu.BuildConfig;
import info.cemu.Cemu.R;
import info.cemu.Cemu.databinding.ActivityEmulationBinding;
import info.cemu.Cemu.input.InputManager;
import info.cemu.Cemu.nativeinterface.NativeSwkbd;

public class EmulationActivity extends AppCompatActivity {
    private boolean hasEmulationError;
    public static final String EXTRA_LAUNCH_PATH = BuildConfig.APPLICATION_ID + ".LaunchPath";
    private final InputManager inputManager = new InputManager();
    private static EmulationActivity emulationActivityInstance;
    private AlertDialog emulationTextInputDialog;

    /**
     * This method is called by swkbd using JNI.
     */
    @SuppressWarnings("unused")
    public static void showEmulationTextInput(String initialText, int maxLength) {
        if (emulationActivityInstance == null || emulationActivityInstance.emulationTextInputDialog != null) {
            return;
        }
        NativeSwkbd.setCurrentInputText(initialText);
        emulationActivityInstance.runOnUiThread(() -> {
            var inputEditTextLayout = emulationActivityInstance.getLayoutInflater().inflate(R.layout.layout_emulation_input, null);
            EmulationTextInputEditText inputEditText = inputEditTextLayout.requireViewById(R.id.emulation_input_text);
            inputEditText.updateText(initialText);
            var dialog = new MaterialAlertDialogBuilder(emulationActivityInstance)
                    .setView(inputEditTextLayout)
                    .setCancelable(false)
                    .setPositiveButton(R.string.done, (d, w) -> {
                    }).show();
            var doneButton = Objects.requireNonNull(dialog.getButton(DialogInterface.BUTTON_POSITIVE));
            doneButton.setEnabled(false);
            doneButton.setOnClickListener(v -> inputEditText.onFinishedEdit());
            inputEditText.setOnTextChangedListener(s -> doneButton.setEnabled(s.length() > 0));
            if (maxLength > 0) {
                inputEditText.appendFilter(new InputFilter.LengthFilter(maxLength));
            }
            emulationActivityInstance.emulationTextInputDialog = dialog;
        });
    }

    /**
     * This method is called by swkbd using JNI.
     */
    @SuppressWarnings("unused")
    public static void hideEmulationTextInput() {
        if (emulationActivityInstance == null || emulationActivityInstance.emulationTextInputDialog == null) {
            return;
        }
        var textInputDialog = emulationActivityInstance.emulationTextInputDialog;
        emulationActivityInstance.emulationTextInputDialog = null;
        emulationActivityInstance.runOnUiThread(textInputDialog::dismiss);
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        if (inputManager.onMotionEvent(event)) {
            return true;
        }
        return super.onGenericMotionEvent(event);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (inputManager.onKeyEvent(event)) {
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        emulationActivityInstance = this;
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitConfirmationDialog();
            }
        });

        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        Uri data = intent.getData();
        String launchPath = null;
        if (extras != null) {
            launchPath = extras.getString(EXTRA_LAUNCH_PATH);
        }
        if (launchPath == null && data != null) {
            launchPath = data.toString();
        }
        if (launchPath == null) {
            throw new RuntimeException("launchPath is null");
        }
        setFullscreen();
        ActivityEmulationBinding binding = ActivityEmulationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        EmulationFragment emulationFragment = (EmulationFragment) getSupportFragmentManager().findFragmentById(R.id.emulation_frame);
        if (emulationFragment == null) {
            emulationFragment = new EmulationFragment(launchPath);
            emulationFragment.setOnEmulationErrorCallback(this::onEmulationError);
            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.emulation_frame, emulationFragment)
                    .commit();
        }

    }

    private void showExitConfirmationDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.exit_confirmation_title)
                .setMessage(R.string.exit_confirm_message)
                .setPositiveButton(R.string.yes, (dialog, which) -> quit())
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.cancel())
                .show();
    }

    private void onEmulationError(String emulationError) {
        if (hasEmulationError) {
            return;
        }
        hasEmulationError = true;
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(R.string.error)
                .setMessage(emulationError)
                .setNeutralButton(R.string.quit, (dialog, which) -> dialog.dismiss())
                .setOnDismissListener(dialog -> quit())
                .show();
    }

    private void setFullscreen() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
    }

    private void quit() {
        finishAffinity();
        System.exit(0);
    }
}