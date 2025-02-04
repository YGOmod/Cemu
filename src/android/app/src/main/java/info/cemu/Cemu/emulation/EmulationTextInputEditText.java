package info.cemu.Cemu.emulation;

import android.content.Context;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import info.cemu.Cemu.nativeinterface.NativeSwkbd;

public class EmulationTextInputEditText extends TextInputEditText {
    static private final Pattern INPUT_PATTERN = Pattern.compile("^[\\da-zA-Z \\-/;:',.?!#\\[\\]$%^&*()_@\\\\<>+=]+$");

    public EmulationTextInputEditText(@NonNull Context context) {
        this(context, null);
    }

    public EmulationTextInputEditText(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, com.google.android.material.R.attr.editTextStyle);
    }

    public void appendFilter(InputFilter inputFilter) {
        setFilters(Stream.concat(Arrays.stream(getFilters()), Stream.of(inputFilter)).toArray(InputFilter[]::new));
    }

    public void updateText(String text) {
        boolean hasFocus = hasFocus();
        if (hasFocus) {
            clearFocus();
        }
        setText(text);
        if (hasFocus) {
            requestFocus();
        }
    }

    public EmulationTextInputEditText(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        appendFilter((source, start, end, dest, dstart, dend) -> {
            if (INPUT_PATTERN.matcher(source).matches()) {
                return null;
            }
            return "";
        });
        setInputType(EditorInfo.TYPE_CLASS_TEXT | EditorInfo.TYPE_TEXT_VARIATION_NORMAL);
        addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                if (!hasFocus()) {
                    return;
                }
                if (onTextChangedListener != null) {
                    onTextChangedListener.onTextChanged(text);
                }
                NativeSwkbd.onTextChanged(text.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    public interface OnTextChangedListener {
        void onTextChanged(CharSequence text);
    }


    private OnTextChangedListener onTextChangedListener;

    @Override
    public void onEditorAction(int actionCode) {
        var text = getText();
        if (actionCode == EditorInfo.IME_ACTION_DONE && text != null && text.length() > 0) {
            onFinishedEdit();
        }
        super.onEditorAction(actionCode);
    }

    public void onFinishedEdit() {
        NativeSwkbd.onFinishedInputEdit();
    }

    public void setOnTextChangedListener(OnTextChangedListener onTextChangedListener) {
        this.onTextChangedListener = onTextChangedListener;
    }
}
