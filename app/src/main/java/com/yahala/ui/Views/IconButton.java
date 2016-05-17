package com.yahala.ui.Views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.yahala.messenger.R;

/**
 * Created by user on 4/24/2014.
 */
public class IconButton extends RelativeLayout {
    public TextView textView;
    public ImageView newUpdateImageView;
    public ImageView imageView;

    public IconButton(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initView(context);
    }

    public IconButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    public IconButton(Context context) {
        super(context);
        initView(context);
    }

    private void initView(Context context) {


        View view = LayoutInflater.from(context).inflate(R.layout.icon_button, null);
        textView = (TextView) view.findViewById(R.id.icon_button_text);
        textView.setSingleLine();

        imageView = (ImageView) view.findViewById(R.id.icon_button_img);
        newUpdateImageView = (ImageView) view.findViewById(R.id.new_update_img);
        addView(view);

    }

    public void setText(String text) {
        textView.setText(text);
    }

    public void setTextColor(int color) {
        textView.setTextColor(color);
    }

    public void setNewUpdateImageVisibility(int visibility) {
        newUpdateImageView.setVisibility(visibility);
    }

    public void setImageResource(int rid) {
        String availableUri = "drawable://" + rid;
        // ImageLoader.getInstance().displayImage(availableUri, imageView);
        imageView.setImageResource(rid);
    }
}
