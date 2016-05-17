package com.yahala.ImageLoader.util;

import android.content.Context;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;

public class AnimationHelper {

    public static final int ANIMATION_DISABLED = -1;

    private final Context context;

    public AnimationHelper(Context context) {
        this.context = context.getApplicationContext();
    }

    public Animation loadAnimation(int animationRes) {
        return animationRes == ANIMATION_DISABLED ? new AnimationSet(false) : AnimationUtils.loadAnimation(context, animationRes);
    }
}
