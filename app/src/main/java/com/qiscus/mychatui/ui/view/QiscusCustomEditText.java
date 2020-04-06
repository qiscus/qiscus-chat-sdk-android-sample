package com.qiscus.mychatui.ui.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;

import androidx.annotation.NonNull;

public class QiscusCustomEditText extends androidx.appcompat.widget.AppCompatEditText {

    public QiscusCustomEditText(Context context) {
        super(context);
    }

    public QiscusCustomEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public QiscusCustomEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);
        if (listener != null)
            listener.onStateChanged(this, true);
    }

    @Override
    public boolean onKeyPreIme(int keyCode, @NonNull KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK
                && event.getAction() == KeyEvent.ACTION_UP) {
            if (listener != null)
                listener.onStateChanged(this, false);
        }
        return super.onKeyPreIme(keyCode, event);
    }

    /**
     * Keyboard Listener
     */
    KeyboardListener listener;

    public void setOnKeyboardListener(KeyboardListener listener) {
        this.listener = listener;
    }

    public interface KeyboardListener {
        void onStateChanged(QiscusCustomEditText keyboardEditText, boolean showing);
    }
}

