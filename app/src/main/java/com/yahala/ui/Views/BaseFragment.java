/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package com.yahala.ui.Views;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.yahala.messenger.FileLog;
import com.yahala.ui.ApplicationLoader;
import com.yahala.ui.Rows.ConnectionsManager;
import com.yahala.ui.LaunchActivity;

public class BaseFragment extends Fragment {
    public int animationType = 0;
    public boolean isFinish = false;
    public View fragmentView;
    public ActionBarActivity parentActivity;
    public int classGuid = 0;
    public boolean firstStart = true;
    public boolean animationInProgress = false;
    private VelocityTracker velocityTracker = null;
    private boolean removeParentOnDestroy = false;
    private boolean removeParentOnAnimationEnd = true;
    private AlertDialog visibleDialog = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parentActivity = (ActionBarActivity) getActivity();
    }

    public void willBeHidden() {

    }

    public ActionBarActivity getParentActivity() {
        return parentActivity;
    }

    public void setParentActivity(ActionBarActivity activity) {
        if (parentActivity != activity) {
            parentActivity = activity;
            if (fragmentView != null) {
                ViewGroup parent = (ViewGroup) fragmentView.getParent();
                if (parent != null) {
                    parent.removeView(fragmentView);
                }
                fragmentView = null;
            }
            if (parentActivity != null) {

            }
        }
    }

    public void finishFragment() {
        finishFragment(false);
    }

    public void finishFragment(boolean bySwipe) {
        if (isFinish || animationInProgress) {
            return;
        }
        isFinish = true;
        if (parentActivity == null) {
            ApplicationLoader.fragmentsStack.remove(this);
            onFragmentDestroy();
            return;
        }

        ((LaunchActivity) parentActivity).finishFragment(bySwipe);
        ((LaunchActivity) parentActivity).updateActionBar();
        if (getActivity() == null) {
            if (fragmentView != null) {
                ViewGroup parent = (ViewGroup) fragmentView.getParent();
                if (parent != null) {
                    parent.removeView(fragmentView);
                }
                fragmentView = null;
            }
            parentActivity = null;
        } else {
            removeParentOnDestroy = true;
        }
    }

    public void removeSelfFromStack() {
        if (isFinish) {
            return;
        }
        isFinish = true;
        if (parentActivity == null) {
            ApplicationLoader.fragmentsStack.remove(this);
            onFragmentDestroy();
            return;
        }
        ((LaunchActivity) parentActivity).removeFromStack(this);
        if (getActivity() == null) {
            if (fragmentView != null) {
                ViewGroup parent = (ViewGroup) fragmentView.getParent();
                if (parent != null) {
                    parent.removeView(fragmentView);
                }
                fragmentView = null;
            }
            parentActivity = null;
        } else {
            removeParentOnDestroy = true;
        }
    }

    public boolean onFragmentCreate() {
        classGuid = ConnectionsManager.getInstance().generateClassGuid();
        return true;
    }

    public void onFragmentDestroy() {
        ConnectionsManager.getInstance().cancelRpcsForClassGuid(classGuid);
        removeParentOnDestroy = true;
        isFinish = true;
    }

    public void onAnimationStart() {
        animationInProgress = true;
    }

    public void onAnimationEnd() {
        animationInProgress = false;
    }

    public boolean onBackPressed() {
        return true;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (removeParentOnDestroy) {
            if (fragmentView != null) {
                ViewGroup parent = (ViewGroup) fragmentView.getParent();
                if (parent != null) {
                    parent.removeView(fragmentView);
                }
                fragmentView = null;
            }
            parentActivity = null;
        }
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {

        // HW layer support only exists on API 11+
        Animation animation = super.onCreateAnimation(transit, enter, nextAnim);

        // HW layer support only exists on API 11+
        if (Build.VERSION.SDK_INT >= 11) {
            if (animation == null && nextAnim != 0) {
                animation = AnimationUtils.loadAnimation(getActivity(), nextAnim);
            }

            if (animation != null) {
                getView().setLayerType(View.LAYER_TYPE_HARDWARE, null);

                animation.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                        BaseFragment.this.onAnimationStart();
                    }

                    public void onAnimationEnd(Animation animation) {
                        try {
                            getView().setLayerType(View.LAYER_TYPE_NONE, null);
                            BaseFragment.this.onAnimationEnd();
                        } catch (Exception e) {
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }


                });
            }
        }

        return animation;

        /*if (nextAnim != 0) {
            Animation animation = AnimationUtils.loadAnimation(getActivity(), nextAnim);
            if (Build.VERSION.SDK_INT >= 11) {
                getView().setLayerType(View.LAYER_TYPE_HARDWARE, null);
            }
            animation.setAnimationListener(new Animation.AnimationListener() {

                public void onAnimationStart(Animation animation) {
                    BaseFragment.this.onAnimationStart();
                }

                public void onAnimationRepeat(Animation animation) {

                }

                public void onAnimationEnd(Animation animation){
                    BaseFragment.this.onAnimationEnd();
                    if (Build.VERSION.SDK_INT >= 11) {
                        getView().setLayerType(View.LAYER_TYPE_NONE, null);
                  }
                }
            });

            return animation;
        } else {
            return super.onCreateAnimation(transit, enter, nextAnim);
        }*/
    }

    public boolean canApplyUpdateStatus() {
        return true;
    }

    public void applySelfActionBar() {

    }

    protected void showAlertDialog(AlertDialog.Builder builder) {
        if (parentActivity == null /*|| parentActivity.checkTransitionAnimation() || parentActivity.animationInProgress || parentActivity.startedTracking*/) {
            return;
        }
        try {
            if (visibleDialog != null) {
                visibleDialog.dismiss();
                visibleDialog = null;
            }
        } catch (Exception e) {
            FileLog.e("tmessages", e);
        }
        visibleDialog = builder.show();
        visibleDialog.setCanceledOnTouchOutside(true);
        visibleDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                visibleDialog = null;
            }
        });
    }
}
