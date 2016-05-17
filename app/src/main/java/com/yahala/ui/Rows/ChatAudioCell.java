/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2014.
 */

package com.yahala.ui.Rows;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;

import com.yahala.android.OSUtilities;
import com.yahala.messenger.R;
import com.yahala.messenger.TLRPC;
import com.yahala.objects.MessageObject;
import com.yahala.ui.Views.ImageReceiver;
import com.yahala.ui.Views.ProgressView;
import com.yahala.ui.Views.SeekBar;
import com.yahala.xmpp.ContactsController;
import com.yahala.xmpp.FileLoader;

import com.yahala.android.MediaController;
import com.yahala.messenger.Utilities;

import java.io.File;

public class ChatAudioCell extends ChatBaseCell implements SeekBar.SeekBarDelegate, MediaController.FileDownloadProgressListener {


    private static Drawable[][] statesDrawable = new Drawable[8][2];
    private static TextPaint timePaint;
    public TLRPC.User audioUser;
    private ImageReceiver avatarImage;
    private SeekBar seekBar;
    private ProgressView progressView;
    private int seekBarX;
    private int seekBarY;
    private int buttonState = 0;
    private int buttonX;
    private int buttonY;
    private int buttonPressed = 0;
    private int avatarPressed = 0;
    private StaticLayout timeLayout;
    private int timeX;
    private String lastTimeString = null;
    private int TAG;
    private TLRPC.FileLocation currentPhoto;
    private String currentNameString;

    public ChatAudioCell(Context context) {
        super(context, false);
        TAG = MediaController.getInstance().generateObserverTag();
        avatarImage = new ImageReceiver();
        avatarImage.parentView = this;
        seekBar = new SeekBar(context);
        seekBar.delegate = this;
        progressView = new ProgressView();

        if (timePaint == null) {
            statesDrawable[0][0] = getResources().getDrawable(R.drawable.play2);
            statesDrawable[0][1] = getResources().getDrawable(R.drawable.play2_pressed);
            statesDrawable[1][0] = getResources().getDrawable(R.drawable.pause2);
            statesDrawable[1][1] = getResources().getDrawable(R.drawable.pause2_pressed);
            statesDrawable[2][0] = getResources().getDrawable(R.drawable.audioload2);
            statesDrawable[2][1] = getResources().getDrawable(R.drawable.audioload2_pressed);
            statesDrawable[3][0] = getResources().getDrawable(R.drawable.audiocancel2);
            statesDrawable[3][1] = getResources().getDrawable(R.drawable.audiocancel2_pressed);

            statesDrawable[4][0] = getResources().getDrawable(R.drawable.play2);
            statesDrawable[4][1] = getResources().getDrawable(R.drawable.play2_pressed);
            statesDrawable[5][0] = getResources().getDrawable(R.drawable.pause2);
            statesDrawable[5][1] = getResources().getDrawable(R.drawable.pause2_pressed);
            statesDrawable[6][0] = getResources().getDrawable(R.drawable.audioload2);
            statesDrawable[6][1] = getResources().getDrawable(R.drawable.audioload2_pressed);
            statesDrawable[7][0] = getResources().getDrawable(R.drawable.audiocancel2);
            statesDrawable[7][1] = getResources().getDrawable(R.drawable.audiocancel2_pressed);

            timePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            timePaint.setTextSize(OSUtilities.dp(12));
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (avatarImage != null) {
            avatarImage.clearImage();
            currentPhoto = null;
        }
        MediaController.getInstance().removeLoadingFileObserver(this);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        boolean result = seekBar.onTouch(event.getAction(), event.getX() - seekBarX, event.getY() - seekBarY);
        if (result) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                getParent().requestDisallowInterceptTouchEvent(true);
            }
            invalidate();
        } else {
            int side = OSUtilities.dp(36);
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (x >= buttonX && x <= buttonX + side && y >= buttonY && y <= buttonY + side) {
                    buttonPressed = 1;
                    invalidate();
                    result = true;
                } else if (x >= avatarImage.imageX && x <= avatarImage.imageX + avatarImage.imageW && y >= avatarImage.imageY && y <= avatarImage.imageY + avatarImage.imageH) {
                    avatarPressed = 1;
                    result = true;
                }
            } else if (buttonPressed == 1) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    buttonPressed = 0;
                    playSoundEffect(SoundEffectConstants.CLICK);
                    didPressedButton();
                    invalidate();
                } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                    buttonPressed = 0;
                    invalidate();
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (!(x >= buttonX && x <= buttonX + side && y >= buttonY && y <= buttonY + side)) {
                        buttonPressed = 0;
                        invalidate();
                    }
                }
            } else if (avatarPressed == 1) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    avatarPressed = 0;
                    playSoundEffect(SoundEffectConstants.CLICK);
                    if (delegate != null) {
                        delegate.didPressedUserAvatar(this, audioUser);
                    }
                } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
                    avatarPressed = 0;
                } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    if (!(x >= avatarImage.imageX && x <= avatarImage.imageX + avatarImage.imageW && y >= avatarImage.imageY && y <= avatarImage.imageY + avatarImage.imageH)) {
                        avatarPressed = 0;
                    }
                }
            }
            if (!result) {
                result = super.onTouchEvent(event);
            }
        }

        return result;
    }


    private void didPressedButton() {
        if (buttonState == 0) {
            boolean result = MediaController.getInstance().playAudio(currentMessageObject);
            if (result) {
                buttonState = 1;
                invalidate();
            }
        } else if (buttonState == 1) {
            boolean result = MediaController.getInstance().pauseAudio(currentMessageObject);
            if (result) {
                buttonState = 0;
                invalidate();
            }
        } else if (buttonState == 2) {
            FileLoader.getInstance().loadFile(null, null, null, currentMessageObject.messageOwner.tl_message.media.audio);
            buttonState = 3;
            invalidate();
        } else if (buttonState == 3) {
            FileLoader.getInstance().cancelLoadFile(null, null, null, currentMessageObject.messageOwner.tl_message.media.audio);
            buttonState = 2;
            invalidate();
        }
    }

    public void updateProgress() {
        if (currentMessageObject == null) {
            return;
        }

        if (!seekBar.isDragging()) {
            seekBar.setProgress(currentMessageObject.audioProgress);
        }

        int duration = 0;
        if (!MediaController.getInstance().isPlayingAudio(currentMessageObject)) {
            duration = currentMessageObject.messageOwner.tl_message.media.audio.duration;
        } else {
            duration = currentMessageObject.audioProgressSec;
        }
        String timeString = String.format("%02d:%02d", duration / 60, duration % 60);
        if (lastTimeString == null || lastTimeString != null && !lastTimeString.equals(timeString)) {
            int timeWidth = (int) Math.ceil(timePaint.measureText(timeString));
            timeLayout = new StaticLayout(timeString, timePaint, timeWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
        }
        invalidate();
    }

    public void downloadAudioIfNeed() {
        if (buttonState == 2) {
            FileLoader.getInstance().loadFile(null, null, null, currentMessageObject.messageOwner.tl_message.media.audio);
            buttonState = 3;
            invalidate();
        }
    }

    public void updateButtonState() {
        String fileName = currentMessageObject.getFileName();

        File cacheFile = new File(OSUtilities.getCacheDir(), fileName);
        if (cacheFile.exists()) {
            MediaController.getInstance().removeLoadingFileObserver(this);
            boolean playing = MediaController.getInstance().isPlayingAudio(currentMessageObject);
            if (!playing || playing && MediaController.getInstance().isAudioPaused()) {
                buttonState = 0;
            } else {
                buttonState = 1;
            }
            progressView.setProgress(0);
        } else {
            MediaController.getInstance().addLoadingFileObserver(fileName, this);
            if (!FileLoader.getInstance().isLoadingFile(fileName)) {
                buttonState = 2;
                progressView.setProgress(0);
            } else {
                buttonState = 3;
                Float progress = FileLoader.getInstance().fileProgresses.get(fileName);
                if (progress != null) {
                    progressView.setProgress(progress);
                } else {
                    progressView.setProgress(0);
                }
            }
        }
        updateProgress();
    }

    @Override
    public void onFailedDownload(String fileName) {
        updateButtonState();
    }

    @Override
    public void onSuccessDownload(String fileName) {
        updateButtonState();
    }

    @Override
    public void onProgressDownload(String fileName, float progress) {
        progressView.setProgress(progress);
        invalidate();
    }

    @Override
    public void onProgressUpload(String fileName, float progress, boolean isEncrypted) {

    }

    @Override
    public int getObserverTag() {
        return TAG;
    }

    @Override
    public void onSeekBarDrag(float progress) {
        if (currentMessageObject == null) {
            return;
        }
        currentMessageObject.audioProgress = progress;
        MediaController.getInstance().seekToProgress(currentMessageObject, progress);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        setMeasuredDimension(width, OSUtilities.dp(68));
        if (isChat) {
            backgroundWidth = Math.min(width - OSUtilities.dp(102), OSUtilities.dp(300));
        } else {
            backgroundWidth = Math.min(width - OSUtilities.dp(50), OSUtilities.dp(300));
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        //if (currentMessageObject.messageOwner.getOut().equals("1")) {
        if (currentMessageObject.isOut()) {
            // avatarImage.imageX = layoutWidth - backgroundWidth + AndroidUtilities.dp(9);
            seekBarX = layoutWidth - backgroundWidth + OSUtilities.dp(40);
            buttonX = layoutWidth - backgroundWidth + OSUtilities.dp(10);
            timeX = layoutWidth - backgroundWidth + OSUtilities.dp(14);
        } else {
            // if (chat) {
            //  avatarImage.imageX = AndroidUtilities.dp(69);
            //   seekBarX = AndroidUtilities.dp(158);
            //    buttonX = AndroidUtilities.dp(128);
            //    timeX = AndroidUtilities.dp(132);
            //  } else {
            //   avatarImage.imageX = AndroidUtilities.dp(16);
            seekBarX = OSUtilities.dp(50);
            buttonX = OSUtilities.dp(20);
            timeX = OSUtilities.dp(24);
            // }
        }
        // avatarImage.imageY = AndroidUtilities.dp(9);
        //  avatarImage.imageW = AndroidUtilities.dp(50);
        // avatarImage.imageH = AndroidUtilities.dp(50);

        seekBar.width = backgroundWidth - OSUtilities.dp(112);
        seekBar.height = OSUtilities.dp(30);
        progressView.width = backgroundWidth - OSUtilities.dp(136);
        progressView.height = OSUtilities.dp(30);
        seekBarY = OSUtilities.dp(13);
        buttonY = OSUtilities.dp(10);

        updateProgress();
    }

    @Override
    protected boolean isUserDataChanged() {
        TLRPC.User newUser = ContactsController.getInstance().friendsDict.get(currentMessageObject.messageOwner.tl_message.media.audio.user_id);
        TLRPC.FileLocation newPhoto = null;

        if (avatarImage != null && newUser != null && newUser.photo != null) {
            newPhoto = newUser.photo.photo_small;
        }

        return currentPhoto == null && newPhoto != null || currentPhoto != null && newPhoto == null || currentPhoto != null && newPhoto != null && (currentPhoto.local_id != newPhoto.local_id || currentPhoto.volume_id != newPhoto.volume_id) || super.isUserDataChanged();
    }

    @Override
    public void setMessageObject(MessageObject messageObject) {
        if (currentMessageObject != messageObject || isUserDataChanged()) {
            int uid = messageObject.messageOwner.tl_message.media.audio.user_id;

            // uid = messageObject.messageOwner.getJid();

            audioUser = ContactsController.getInstance().friendsDict.get(uid);
            if (audioUser != null) {
                if (audioUser.photo != null) {
                    currentPhoto = audioUser.photo.photo_small;
                }
                avatarImage.setImage(currentPhoto, "50_50", getResources().getDrawable(Utilities.getUserAvatarForId(uid)));
            } else {
                avatarImage.setImage((TLRPC.FileLocation) null, "50_50", getResources().getDrawable(Utilities.getUserAvatarForId(uid)));
            }

            if (messageObject.messageOwner.tl_message.out) {
                seekBar.type = 0;
                progressView.type = 0;
            } else {
                seekBar.type = 1;
                progressView.type = 1;
            }

            super.setMessageObject(messageObject);
        }
        updateButtonState();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (currentMessageObject == null) {
            return;
        }

        // avatarImage.draw(canvas, avatarImage.imageX, avatarImage.imageY, AndroidUtilities.dp(50), AndroidUtilities.dp(50));

        canvas.save();
        if (buttonState == 0 || buttonState == 1) {
            canvas.translate(seekBarX, seekBarY);
            seekBar.draw(canvas);
        } else {
            canvas.translate(seekBarX + OSUtilities.dp(12), seekBarY);
            progressView.draw(canvas);
        }
        canvas.restore();

        int state = buttonState;
        if (!currentMessageObject.messageOwner.tl_message.out) {
            state += 4;
            timePaint.setColor(0xffa1aab3);
        } else {
            timePaint.setColor(0xffa1aab3);
        }
        Drawable buttonDrawable = statesDrawable[state][buttonPressed];
        int side = OSUtilities.dp(36);
        int x = (side - buttonDrawable.getIntrinsicWidth()) / 2;
        int y = (side - buttonDrawable.getIntrinsicHeight()) / 2;
        setDrawableBounds(buttonDrawable, x + buttonX, y + buttonY);
        buttonDrawable.draw(canvas);

        canvas.save();
        canvas.translate(timeX, OSUtilities.dp(45));
        timeLayout.draw(canvas);
        canvas.restore();
    }

    @Override
    protected void finalize() throws Throwable {
        MediaController.getInstance().removeLoadingFileObserver(this);
        super.finalize();
    }
}
