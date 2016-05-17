package com.asghonim.salp;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.widget.Button;

import com.yahala.android.OSUtilities;
import com.yahala.messenger.R;

public class PasswordButton extends Button {

    public PasswordButton(Context context) {
        this(context, null, 0);
    }

    public PasswordButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PasswordButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        int i = getId();
        if (i == R.id.password_button_1) {
            setTag("1");

        } else if (i == R.id.password_button_2) {
            setTag("2");

        } else if (i == R.id.password_button_3) {
            setTag("3");

        } else if (i == R.id.password_button_4) {
            setTag("4");

        } else if (i == R.id.password_button_5) {
            setTag("5");

        } else if (i == R.id.password_button_6) {
            setTag("6");

        } else if (i == R.id.password_button_7) {
            setTag("7");

        } else if (i == R.id.password_button_8) {
            setTag("8");

        } else if (i == R.id.password_button_9) {
            setTag("9");

        }
        setBackgroundResource(R.drawable.loginbuttonunpressed);
    }

    @Override
    public boolean onDragEvent(DragEvent event) {
        PasswordStateObject state = (PasswordStateObject) event.getLocalState();
        if (event.getAction() == DragEvent.ACTION_DRAG_LOCATION) {
            if (!state.toString().contains((String) getTag())) {
                state.append((String) getTag());
                ((PasswordListener) getParent()).onPasswordButtonTouched(this);
                setBackgroundResource(R.drawable.loginbuttonpressed);
            }
        }
        if (event.getAction() == DragEvent.ACTION_DRAG_ENDED)
            if (state != null) {
                state.passwordComplete();
            }
        return true;
    }

    @Override
    public void getHitRect(Rect outRect) {
        outRect.set(getLeft() + OSUtilities.dp(50), getTop() + OSUtilities.dp(50), getRight() + OSUtilities.dp(50), getBottom() + OSUtilities.dp(50));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        startDrag(null, new DragShadowBuilder(this) {
            @Override
            public void onDrawShadow(Canvas canvas) {
            }
        }, new PasswordStateObject() {
            @Override
            protected void onPasswordComplete(String s) {
                android.view.View v = PasswordButton.this;
                ((PasswordListener) getParent()).onPasswordComplete(s);
            }
        }, 0);
        super.onTouchEvent(event);
        return true;
    }
}
