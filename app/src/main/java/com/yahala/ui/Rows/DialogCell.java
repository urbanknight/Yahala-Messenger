/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package com.yahala.ui.Rows;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;

import com.yahala.PhoneFormat.PhoneFormat;
import com.yahala.android.OSUtilities;
import com.yahala.messenger.R;
import com.yahala.messenger.ContactsController;
import com.yahala.android.Emoji;
import com.yahala.android.LocaleController;
import com.yahala.messenger.MessagesController;
import com.yahala.messenger.TLRPC;
import com.yahala.messenger.UserConfig;
import com.yahala.objects.MessageObject;
import com.yahala.ui.Views.ImageReceiver;

import com.yahala.messenger.Utilities;

public class DialogCell extends BaseCell {
    private static TextPaint namePaint;
    private static TextPaint nameEncryptedPaint;
    private static TextPaint nameUnknownPaint;
    private static TextPaint messagePaint;
    private static TextPaint messagePrintingPaint;
    private static TextPaint timePaint;
    private static TextPaint countPaint;

    private static Drawable checkDrawable;
    private static Drawable halfCheckDrawable;
    private static Drawable clockDrawable;
    private static Drawable errorDrawable;
    private static Drawable lockDrawable;
    private static Drawable countDrawable;
    private static Drawable groupDrawable;

    private TLRPC.TL_dialog currentDialog;
    private ImageReceiver avatarImage;

    private DialogCellLayout cellLayout;
    private TLRPC.User user = null;
    private TLRPC.Chat chat = null;
    private TLRPC.EncryptedChat encryptedChat = null;
    private CharSequence lastPrintString = null;

    public DialogCell(Context context) {
        super(context);
        init();
    }

    private void init() {
        if (namePaint == null) {
            namePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            namePaint.setTextSize(OSUtilities.dp(19));
            namePaint.setColor(0xff222222);
            // namePaint.setTypeface(Utilities.getTypeface("fonts/rmedium.ttf"));
        }

        if (nameEncryptedPaint == null) {
            nameEncryptedPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            nameEncryptedPaint.setTextSize(OSUtilities.dp(19));
            nameEncryptedPaint.setColor(0xff00a60e);
            //  nameEncryptedPaint.setTypeface(Utilities.getTypeface("fonts/rmedium.ttf"));
        }

        if (nameUnknownPaint == null) {
            nameUnknownPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            nameUnknownPaint.setTextSize(OSUtilities.dp(19));
            nameUnknownPaint.setColor(0xff316f9f);
            // nameUnknownPaint.setTypeface(Utilities.getTypeface("fonts/rmedium.ttf"));
        }

        if (messagePaint == null) {
            messagePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            messagePaint.setTextSize(OSUtilities.dp(16));
            messagePaint.setColor(0xff808080);
        }

        if (messagePrintingPaint == null) {
            messagePrintingPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            messagePrintingPaint.setTextSize(OSUtilities.dp(16));
            messagePrintingPaint.setColor(0xff316f9f);
        }

        if (timePaint == null) {
            timePaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            timePaint.setTextSize(OSUtilities.dp(14));
            timePaint.setColor(0xff9e9e9e);
        }

        if (countPaint == null) {
            countPaint = new TextPaint(TextPaint.ANTI_ALIAS_FLAG);
            countPaint.setTextSize(OSUtilities.dp(13));
            countPaint.setColor(0xffffffff);
        }

        if (lockDrawable == null) {
            lockDrawable = getResources().getDrawable(R.drawable.ic_lock_green);
        }

        if (checkDrawable == null) {
            checkDrawable = getResources().getDrawable(R.drawable.dialogs_check);
        }

        if (halfCheckDrawable == null) {
            halfCheckDrawable = getResources().getDrawable(R.drawable.dialogs_halfcheck);
        }

        if (clockDrawable == null) {
            clockDrawable = getResources().getDrawable(R.drawable.msg_clock);
        }

        if (errorDrawable == null) {
            errorDrawable = getResources().getDrawable(R.drawable.dialogs_warning);
        }

        if (countDrawable == null) {
            countDrawable = getResources().getDrawable(R.drawable.dialogs_badge);
        }

        if (groupDrawable == null) {
            groupDrawable = getResources().getDrawable(R.drawable.grouplist);
        }

        if (avatarImage == null) {
            avatarImage = new ImageReceiver();
            //avatarImage.parentView = new WeakReference<View>(this);
        }

        if (cellLayout == null) {
            cellLayout = new DialogCellLayout();
        }
    }

    public void setDialog(TLRPC.TL_dialog dialog) {
        currentDialog = dialog;
        update(0);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), OSUtilities.dp(70));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (currentDialog == null) {
            super.onLayout(changed, left, top, right, bottom);
            return;
        }
        if (changed) {
            buildLayout();
        }
    }

    public void buildLayout() {
        cellLayout.build(getMeasuredWidth(), getMeasuredHeight());
    }

    public void update(int mask) {
        if (mask != 0) {
            boolean continueUpdate = false;
            if ((mask & MessagesController.UPDATE_MASK_USER_PRINT) != 0) {
                CharSequence printString = MessagesController.getInstance().printingStrings.get(currentDialog.id);
                if (lastPrintString != null && printString == null || lastPrintString == null && printString != null || lastPrintString != null && printString != null && !lastPrintString.equals(printString)) {
                    continueUpdate = true;
                }
            }
            if ((mask & MessagesController.UPDATE_MASK_AVATAR) != 0) {
                if (chat == null) {
                    continueUpdate = true;
                }
            }
            if ((mask & MessagesController.UPDATE_MASK_NAME) != 0) {
                if (chat == null) {
                    continueUpdate = true;
                }
            }
            if ((mask & MessagesController.UPDATE_MASK_CHAT_AVATAR) != 0) {
                if (user == null) {
                    continueUpdate = true;
                }
            }
            if ((mask & MessagesController.UPDATE_MASK_CHAT_NAME) != 0) {
                if (user == null) {
                    continueUpdate = true;
                }
            }
            if ((mask & MessagesController.UPDATE_MASK_READ_DIALOG_MESSAGE) != 0) {
                continueUpdate = true;
            }

            if (!continueUpdate) {
                return;
            }
        }
        user = null;
        chat = null;
        encryptedChat = null;

        int lower_id = (int) currentDialog.id;
        if (lower_id != 0) {
            if (lower_id < 0) {
                chat = MessagesController.getInstance().chats.get(-lower_id);
            } else {
                user = MessagesController.getInstance().users.get(lower_id);
            }
        } else {
            encryptedChat = MessagesController.getInstance().encryptedChats.get((int) (currentDialog.id >> 32));
            if (encryptedChat != null) {
                user = MessagesController.getInstance().users.get(encryptedChat.user_id);
            }
        }

        int placeHolderId = 0;
        TLRPC.FileLocation photo = null;
        if (user != null) {
            if (user.photo != null) {
                photo = user.photo.photo_small;
            }
            placeHolderId = Utilities.getUserAvatarForId(user.id);
        } else if (chat != null) {
            if (chat.photo != null) {
                photo = chat.photo.photo_small;
            }
            placeHolderId = Utilities.getGroupAvatarForId(chat.id);
        }
        avatarImage.setImage(photo, "50_50", placeHolderId == 0 ? null : getResources().getDrawable(placeHolderId));

        if (getMeasuredWidth() != 0 || getMeasuredHeight() != 0) {
            buildLayout();
        } else {
            requestLayout();
        }

        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (currentDialog == null) {
            return;
        }

        if (cellLayout == null) {
            requestLayout();
            return;
        }

        if (cellLayout.drawNameLock) {
            setDrawableBounds(lockDrawable, cellLayout.nameLockLeft, cellLayout.nameLockTop);
            lockDrawable.draw(canvas);
        } else if (cellLayout.drawNameGroup) {
            setDrawableBounds(groupDrawable, cellLayout.nameLockLeft, cellLayout.nameLockTop);
            groupDrawable.draw(canvas);
        }

        canvas.save();
        canvas.translate(cellLayout.nameLeft, cellLayout.nameTop);
        cellLayout.nameLayout.draw(canvas);
        canvas.restore();

        canvas.save();
        canvas.translate(cellLayout.timeLeft, cellLayout.timeTop);
        cellLayout.timeLayout.draw(canvas);
        canvas.restore();

        canvas.save();
        canvas.translate(cellLayout.messageLeft, cellLayout.messageTop);
        cellLayout.messageLayout.draw(canvas);
        canvas.restore();

        if (cellLayout.drawClock) {
            setDrawableBounds(clockDrawable, cellLayout.checkDrawLeft, cellLayout.checkDrawTop);
            clockDrawable.draw(canvas);
        } else if (cellLayout.drawCheck2) {
            if (cellLayout.drawCheck1) {
                setDrawableBounds(halfCheckDrawable, cellLayout.halfCheckDrawLeft, cellLayout.checkDrawTop);
                halfCheckDrawable.draw(canvas);
                setDrawableBounds(checkDrawable, cellLayout.checkDrawLeft, cellLayout.checkDrawTop);
                checkDrawable.draw(canvas);
            } else {
                setDrawableBounds(checkDrawable, cellLayout.checkDrawLeft, cellLayout.checkDrawTop);
                checkDrawable.draw(canvas);
            }
        }

        if (cellLayout.drawError) {
            setDrawableBounds(errorDrawable, cellLayout.errorLeft, cellLayout.errorTop);
            errorDrawable.draw(canvas);
        } else if (cellLayout.drawCount) {
            setDrawableBounds(countDrawable, cellLayout.countLeft - OSUtilities.dp(5), cellLayout.countTop, cellLayout.countWidth + OSUtilities.dp(10), countDrawable.getIntrinsicHeight());
            countDrawable.draw(canvas);
            canvas.save();
            canvas.translate(cellLayout.countLeft, cellLayout.countTop + OSUtilities.dp(3));
            cellLayout.countLayout.draw(canvas);
            canvas.restore();
        }

        avatarImage.draw(canvas, cellLayout.avatarLeft, cellLayout.avatarTop, OSUtilities.dp(54), OSUtilities.dp(54));
    }

    private class DialogCellLayout {
        private int nameLeft;
        private int nameTop = OSUtilities.dp(10);
        private int nameWidth;
        private StaticLayout nameLayout;
        private boolean drawNameLock;
        private boolean drawNameGroup;
        private int nameLockLeft;
        private int nameLockTop;

        private int timeLeft;
        private int timeTop = OSUtilities.dp(13);
        private int timeWidth;
        private StaticLayout timeLayout;

        private boolean drawCheck1;
        private boolean drawCheck2;
        private boolean drawClock;
        private int checkDrawLeft;
        private int checkDrawTop = OSUtilities.dp(15);
        private int halfCheckDrawLeft;

        private int messageTop = OSUtilities.dp(40);
        private int messageLeft;
        private int messageWidth;
        private StaticLayout messageLayout;

        private boolean drawError;
        private int errorTop = OSUtilities.dp(37);
        private int errorLeft;

        private boolean drawCount;
        private int countTop = OSUtilities.dp(37);
        private int countLeft;
        private int countWidth;
        private StaticLayout countLayout;

        private int avatarTop = OSUtilities.dp(8);
        private int avatarLeft;

        public void build(int width, int height) {
            MessageObject message = MessagesController.getInstance().dialogMessage.get(currentDialog.top_message);
            String nameString = "";
            String timeString = "";
            String countString = null;
            CharSequence messageString = "";
            CharSequence printingString = MessagesController.getInstance().printingStrings.get(currentDialog.id);
            TextPaint currentNamePaint = namePaint;
            TextPaint currentMessagePaint = messagePaint;
            boolean checkMessage = true;

            if (encryptedChat != null) {
                drawNameLock = true;
                drawNameGroup = false;
                nameLockTop = OSUtilities.dp(13);
                if (!LocaleController.isRTL) {
                    nameLockLeft = OSUtilities.dp(77);
                    nameLeft = OSUtilities.dp(81) + lockDrawable.getIntrinsicWidth();
                } else {
                    nameLockLeft = width - OSUtilities.dp(77) - lockDrawable.getIntrinsicWidth();
                    nameLeft = OSUtilities.dp(14);
                }
            } else {
                drawNameLock = false;
                if (chat != null) {
                    drawNameGroup = true;
                    nameLockTop = OSUtilities.dp(14);
                    if (!LocaleController.isRTL) {
                        nameLockLeft = OSUtilities.dp(77);
                        nameLeft = OSUtilities.dp(81) + groupDrawable.getIntrinsicWidth();
                    } else {
                        nameLockLeft = width - OSUtilities.dp(77) - groupDrawable.getIntrinsicWidth();
                        nameLeft = OSUtilities.dp(14);
                    }
                } else {
                    drawNameGroup = false;
                    if (!LocaleController.isRTL) {
                        nameLeft = OSUtilities.dp(77);
                    } else {
                        nameLeft = OSUtilities.dp(14);
                    }
                }
            }

            if (message == null) {
                if (printingString != null) {
                    lastPrintString = messageString = printingString;
                    currentMessagePaint = messagePrintingPaint;
                } else {
                    lastPrintString = null;
                    if (encryptedChat != null) {
                        currentMessagePaint = messagePrintingPaint;
                        if (encryptedChat instanceof TLRPC.TL_encryptedChatRequested) {
                            messageString = LocaleController.getString("EncryptionProcessing", R.string.EncryptionProcessing);
                        } else if (encryptedChat instanceof TLRPC.TL_encryptedChatWaiting) {
                            if (user != null && user.first_name != null) {
                                messageString = LocaleController.formatString("AwaitingEncryption", R.string.AwaitingEncryption, user.first_name);
                            } else {
                                messageString = LocaleController.formatString("AwaitingEncryption", R.string.AwaitingEncryption, "");
                            }
                        } else if (encryptedChat instanceof TLRPC.TL_encryptedChatDiscarded) {
                            messageString = LocaleController.getString("EncryptionRejected", R.string.EncryptionRejected);
                        } else if (encryptedChat instanceof TLRPC.TL_encryptedChat) {
                            if (encryptedChat.admin_id == UserConfig.clientUserId) {
                                if (user != null && user.first_name != null) {
                                    messageString = LocaleController.formatString("EncryptedChatStartedOutgoing", R.string.EncryptedChatStartedOutgoing, user.first_name);
                                } else {
                                    messageString = LocaleController.formatString("EncryptedChatStartedOutgoing", R.string.EncryptedChatStartedOutgoing, "");
                                }
                            } else {
                                messageString = LocaleController.getString("EncryptedChatStartedIncoming", R.string.EncryptedChatStartedIncoming);
                            }
                        }
                    }
                }
                if (currentDialog.last_message_date != 0) {
                    timeString = LocaleController.stringForMessageListDate(currentDialog.last_message_date);
                }
                drawCheck1 = false;
                drawCheck2 = false;
                drawClock = false;
                drawCount = false;
                drawError = false;
            } else {
               /* TLRPC.User fromUser = MessagesController.getInstance().users.get(message.messageOwner.from_id);

                if (currentDialog.last_message_date != 0) {
                    timeString = LocaleController.stringForMessageListDate(currentDialog.last_message_date);
                } else {
                    timeString = LocaleController.stringForMessageListDate(message.messageOwner.date);
                }
                if (printingString != null) {
                    lastPrintString = messageString = printingString;
                    currentMessagePaint = messagePrintingPaint;
                } else {
                    lastPrintString = null;
                    if (message.messageOwner instanceof TLRPC.TL_messageService) {
                        messageString = message.messageText;
                        currentMessagePaint = messagePrintingPaint;
                    } else {
                        if (chat != null) {
                            String name = "";
                            if (message.messageOwner.from_id == UserConfig.clientUserId) {
                                name = LocaleController.getString("FromYou", R.string.FromYou);
                            } else {
                                if (fromUser != null) {
                                    if (fromUser.first_name.length() > 0) {
                                        name = fromUser.first_name;
                                    } else {
                                        name = fromUser.last_name;
                                    }
                                }
                            }
                            if (message.messageOwner.media != null && !(message.messageOwner.media instanceof TLRPC.TL_messageMediaEmpty)) {
                                messageString = message.messageText;
                                currentMessagePaint = messagePrintingPaint;
                            } else {
                                checkMessage = false;
                                if (message.messageOwner.message != null) {
                                    messageString = Emoji.replaceEmoji(Html.fromHtml(String.format("<font color=#316f9f>%s:</font> <font color=#808080>%s</font>", name, message.messageOwner.message.replace("\n", " "))));
                                }
                            }
                        } else {
                            messageString = message.messageText;
                            if (message.messageOwner.media != null && !(message.messageOwner.media instanceof TLRPC.TL_messageMediaEmpty)) {
                                currentMessagePaint = messagePrintingPaint;
                            }
                        }
                    }
                }*/

                if (currentDialog.unread_count != 0) {
                    drawCount = true;
                    countString = String.format("%d", currentDialog.unread_count);
                } else {
                    drawCount = false;
                }
/**
 if (message.messageOwner.id < 0 && message.messageOwner.send_state != MessagesController.MESSAGE_SEND_STATE_SENT) {
 if (MessagesController.getInstance().sendingMessages.get(message.messageOwner.id) == null) {
 message.messageOwner.send_state = MessagesController.MESSAGE_SEND_STATE_SEND_ERROR;
 }
 }

 if (message.messageOwner.from_id == UserConfig.clientUserId) {
 if (message.messageOwner.send_state == MessagesController.MESSAGE_SEND_STATE_SENDING) {
 drawCheck1 = false;
 drawCheck2 = false;
 drawClock = true;
 drawError = false;
 } else if (message.messageOwner.send_state == MessagesController.MESSAGE_SEND_STATE_SEND_ERROR) {
 drawCheck1 = false;
 drawCheck2 = false;
 drawClock = false;
 drawError = true;
 drawCount = false;
 } else if (message.messageOwner.send_state == MessagesController.MESSAGE_SEND_STATE_SENT) {
 if (!message.messageOwner.unread) {
 drawCheck1 = true;
 drawCheck2 = true;
 } else {
 drawCheck1 = false;
 drawCheck2 = true;
 }
 drawClock = false;
 drawError = false;
 }
 } else {
 drawCheck1 = false;
 drawCheck2 = false;
 drawClock = false;
 drawError = false;
 }*/
            }

            timeWidth = (int) Math.ceil(timePaint.measureText(timeString));
            timeLayout = new StaticLayout(timeString, timePaint, timeWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
            if (!LocaleController.isRTL) {
                timeLeft = width - OSUtilities.dp(11) - timeWidth;
            } else {
                timeLeft = OSUtilities.dp(11);
            }

            if (chat != null) {
                nameString = chat.title;
            } else if (user != null) {
                if (user.id / 1000 != 333 && ContactsController.getInstance().contactsDict.get(user.id) == null) {
                    if (ContactsController.getInstance().contactsDict.size() == 0 && (!ContactsController.getInstance().contactsLoaded || ContactsController.getInstance().loadingContacts)) {
                        nameString = Utilities.formatName(user.first_name, user.last_name);
                    } else {
                        if (user.phone != null && user.phone.length() != 0) {
                            nameString = PhoneFormat.getInstance().format("+" + user.phone);
                        } else {
                            currentNamePaint = nameUnknownPaint;
                            nameString = Utilities.formatName(user.first_name, user.last_name);
                        }
                    }
                } else {
                    nameString = Utilities.formatName(user.first_name, user.last_name);
                }
                if (encryptedChat != null) {
                    currentNamePaint = nameEncryptedPaint;
                }
            }
            if (nameString.length() == 0) {
                nameString = LocaleController.getString("HiddenName", R.string.HiddenName);
            }

            if (!LocaleController.isRTL) {
                nameWidth = width - nameLeft - OSUtilities.dp(14) - timeWidth;
            } else {
                nameWidth = width - nameLeft - OSUtilities.dp(77) - timeWidth;
                nameLeft += timeWidth;
            }
            if (drawNameLock) {
                nameWidth -= OSUtilities.dp(4) + lockDrawable.getIntrinsicWidth();
            } else if (drawNameGroup) {
                nameWidth -= OSUtilities.dp(4) + groupDrawable.getIntrinsicWidth();
            }
            if (drawClock) {
                int w = clockDrawable.getIntrinsicWidth() + OSUtilities.dp(2);
                nameWidth -= w;
                if (!LocaleController.isRTL) {
                    checkDrawLeft = timeLeft - w;
                } else {
                    checkDrawLeft = timeLeft + timeWidth + OSUtilities.dp(2);
                    nameLeft += w;
                }
            } else if (drawCheck2) {
                int w = checkDrawable.getIntrinsicWidth() + OSUtilities.dp(2);
                nameWidth -= w;
                if (drawCheck1) {
                    nameWidth -= halfCheckDrawable.getIntrinsicWidth() - OSUtilities.dp(5);
                    if (!LocaleController.isRTL) {
                        halfCheckDrawLeft = timeLeft - w;
                        checkDrawLeft = halfCheckDrawLeft - OSUtilities.dp(5);
                    } else {
                        checkDrawLeft = timeLeft + timeWidth + OSUtilities.dp(2);
                        halfCheckDrawLeft = checkDrawLeft + OSUtilities.dp(5);
                        nameLeft += w + halfCheckDrawable.getIntrinsicWidth() - OSUtilities.dp(5);
                    }
                } else {
                    if (!LocaleController.isRTL) {
                        checkDrawLeft = timeLeft - w;
                    } else {
                        checkDrawLeft = timeLeft + timeWidth + OSUtilities.dp(2);
                        nameLeft += w;
                    }
                }
            }

            CharSequence nameStringFinal = TextUtils.ellipsize(nameString.replace("\n", " "), currentNamePaint, nameWidth - OSUtilities.dp(12), TextUtils.TruncateAt.END);
            nameLayout = new StaticLayout(nameStringFinal, currentNamePaint, nameWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

            messageWidth = width - OSUtilities.dp(88);
            if (!LocaleController.isRTL) {
                messageLeft = OSUtilities.dp(77);
                avatarLeft = OSUtilities.dp(11);
            } else {
                messageLeft = OSUtilities.dp(11);
                avatarLeft = width - OSUtilities.dp(65);
            }
            avatarImage.imageX = avatarLeft;
            avatarImage.imageY = avatarTop;
            avatarImage.imageW = OSUtilities.dp(54);
            avatarImage.imageH = OSUtilities.dp(54);
            if (drawError) {
                int w = errorDrawable.getIntrinsicWidth() + OSUtilities.dp(8);
                messageWidth -= w;
                if (!LocaleController.isRTL) {
                    errorLeft = width - errorDrawable.getIntrinsicWidth() - OSUtilities.dp(11);
                } else {
                    errorLeft = OSUtilities.dp(11);
                    messageLeft += w;
                }
            } else if (countString != null) {
                countWidth = Math.max(OSUtilities.dp(12), (int) Math.ceil(countPaint.measureText(countString)));
                countLayout = new StaticLayout(countString, countPaint, countWidth, Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);
                int w = countWidth + OSUtilities.dp(18);
                messageWidth -= w;
                if (!LocaleController.isRTL) {
                    countLeft = width - countWidth - OSUtilities.dp(16);
                } else {
                    countLeft = OSUtilities.dp(16);
                    messageLeft += w;
                }
                drawCount = true;
            } else {
                drawCount = false;
            }

            if (checkMessage) {
                if (messageString == null) {
                    messageString = "";
                }
                String mess = messageString.toString().replace("\n", " ");
                if (mess.length() > 150) {
                    mess = mess.substring(0, 150);
                }
                messageString = Emoji.replaceEmoji(mess, messagePaint.getFontMetricsInt(), OSUtilities.dp(20));
            }

            CharSequence messageStringFinal = TextUtils.ellipsize(messageString, currentMessagePaint, messageWidth - OSUtilities.dp(12), TextUtils.TruncateAt.END);
            messageLayout = new StaticLayout(messageStringFinal, currentMessagePaint, messageWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);

            double widthpx = 0;
            float left = 0;
            if (LocaleController.isRTL) {
                if (nameLayout.getLineCount() > 0) {
                    left = nameLayout.getLineLeft(0);
                    if (left == 0) {
                        widthpx = Math.ceil(nameLayout.getLineWidth(0));
                        if (widthpx < nameWidth) {
                            nameLeft += (nameWidth - widthpx);
                        }
                    }
                }
                if (messageLayout.getLineCount() > 0) {
                    left = messageLayout.getLineLeft(0);
                    if (left == 0) {
                        widthpx = Math.ceil(messageLayout.getLineWidth(0));
                        if (widthpx < messageWidth) {
                            messageLeft += (messageWidth - widthpx);
                        }
                    }
                }
            } else {
                if (nameLayout.getLineCount() > 0) {
                    left = nameLayout.getLineRight(0);
                    if (left == nameWidth) {
                        widthpx = Math.ceil(nameLayout.getLineWidth(0));
                        if (widthpx < nameWidth) {
                            nameLeft -= (nameWidth - widthpx);
                        }
                    }
                }
                if (messageLayout.getLineCount() > 0) {
                    left = messageLayout.getLineRight(0);
                    if (left == messageWidth) {
                        widthpx = Math.ceil(messageLayout.getLineWidth(0));
                        if (widthpx < messageWidth) {
                            messageLeft -= (messageWidth - widthpx);
                        }
                    }
                }
            }
        }
    }
}
