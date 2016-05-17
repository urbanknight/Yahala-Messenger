package com.yahala.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.yahala.messenger.R;
import com.yahala.messenger.FileLog;
import com.yahala.android.LocaleController;
import com.yahala.messenger.NotificationCenter;
import com.yahala.messenger.UserConfig;
import com.yahala.ui.Adapters.MessagesAdapter;
import com.yahala.ui.Views.BaseFragment;
import com.yahala.xmpp.MessagesController;

/**
 * Created by user on 4/24/2014.
 */
public class UpdatesActivity extends BaseFragment implements NotificationCenter.NotificationCenterDelegate {
    private MessagesAdapter listViewAdapter;
    private TextView welcome;
    private TextView updateStatus;
    private EditText statusText;
    private Button updateButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (fragmentView == null) {

                /*searching = false;
                searchWas = false;*/


            // NotificationCenter.getInstance().addObserver(this,MessagesController.dialogDidLoaded);

            fragmentView = inflater.inflate(R.layout.updates, container, false);
            TextView welcome = (TextView) fragmentView.findViewById(R.id.welcome);
            TextView updateStatus = (TextView) fragmentView.findViewById(R.id.updateStatus);
            final EditText statusText = (EditText) fragmentView.findViewById(R.id.status_text);
            Button updateButton = (Button) fragmentView.findViewById(R.id.update_button);

            Typeface typefaceR = Typeface.createFromAsset(ApplicationLoader.applicationContext.getAssets(), "fonts/Roboto-Regular.ttf");
            Typeface typefaceL = Typeface.createFromAsset(ApplicationLoader.applicationContext.getAssets(), "fonts/Roboto-Light.ttf");

            welcome.setTypeface(typefaceR);
            updateStatus.setTypeface(typefaceL);
            updateButton.setTypeface(typefaceL);
            statusText.setTypeface(typefaceL);
            try {
                statusText.setText(UserConfig.clientUserStatus);
            } catch (Exception e) {
                e.printStackTrace();
            }
            updateButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    FileLog.e("Test", "onClick");
                    UserConfig.clientUserStatus = statusText.getText().toString();
                    UserConfig.saveConfig(false);
                }
            });
        } else {
            ViewGroup parent = (ViewGroup) fragmentView.getParent();
            if (parent != null) {
                parent.removeView(fragmentView);
            }
        }
        return fragmentView;
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        // NotificationCenter.getInstance().removeObserver(this, MessagesController.dialogDidLoaded);
    }


    @Override
    public void applySelfActionBar() {
        if (parentActivity == null) {
            return;
        }
        final ActionBar actionBar = parentActivity.getSupportActionBar();

        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayShowHomeEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayUseLogoEnabled(false);
        actionBar.setDisplayShowCustomEnabled(false);
        actionBar.setSubtitle(null);
        actionBar.setCustomView(null);
        actionBar.setTitle(LocaleController.getString("SelectChat", R.string.SelectChat));


        TextView title = (TextView) parentActivity.findViewById(R.id.action_bar_title);
        if (title == null) {
            final int subtitleId = parentActivity.getResources().getIdentifier("action_bar_title", "id", "android");
            title = (TextView) parentActivity.findViewById(subtitleId);
        }
        if (title != null) {
            title.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            title.setCompoundDrawablePadding(0);
        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void didReceivedNotification(int id, Object... args) {
        if (id == MessagesController.dialogDidLoaded) {


        }
    }


}
