package org.joinmastodon.android.ui.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import me.grishka.appkit.utils.V;

public class TextInputFrameLayout extends FrameLayout {
    private final EditText editText;

    public TextInputFrameLayout(@NonNull Context context, CharSequence hint, CharSequence text) {
        this(context, null, 0, 0, hint, text);
    }

    public TextInputFrameLayout(@NonNull Context context) {
        this(context, null);
    }

    public TextInputFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TextInputFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public TextInputFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this(context, attrs, defStyleAttr, defStyleRes, null, null);
    }

    public TextInputFrameLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes, CharSequence hint, CharSequence text) {
        super(context, attrs, defStyleAttr, defStyleRes);
        editText = new EditText(context);
        editText.setHint(hint);
        editText.setText(text);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(V.dp(24), V.dp(4), V.dp(24), 0);
        editText.setLayoutParams(params);
        addView(editText);
    }

    public EditText getEditText() {
        return editText;
    }
}
