package com.yahala.ui.Adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.yahala.ImageLoader.ImageLoaderInitializer;
import com.yahala.ImageLoader.ImageManager;
import com.yahala.ImageLoader.model.ImageTag;
import com.yahala.ImageLoader.model.ImageTagFactory;
import com.yahala.android.OSUtilities;
import com.yahala.messenger.R;
import com.yahala.android.LocaleController;
import com.yahala.messenger.TLRPC;
import com.yahala.android.emoji.EmojiManager;
import com.yahala.ui.ApplicationLoader;
import com.yahala.ui.LaunchActivity;
import com.yahala.ui.UserProfileActivity;
import com.yahala.ui.Views.CircleImageView.PictureImplCircleImageView;
import com.yahala.xmpp.MessagesController;
import com.yahala.xmpp.XMPPManager;

import org.jivesoftware.smack.packet.Presence;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.ArrayList;

/**
 * Created by user on 4/9/2014.
 */
public class MessagesAdapter extends ArrayAdapter<TLRPC.TL_dialog> {
    public static ActionBarActivity parentFragment;
    //private TextView mesgsage;
    //private ArrayList<TLRPC.TL_dialog> ignoreUsers = new ArrayList<TLRPC.TL_dialog>();

    private RelativeLayout wrapper;

    public MessagesAdapter(Context context, int textViewResourceId, ArrayList<TLRPC.TL_dialog> ignoreUsers) {
        super(context, textViewResourceId);
        //this.ignoreUsers=ignoreUsers;

    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public int getItemViewType(int position) {

        //  if(getItem(position).left)
        //    {
        return 0;
        //    }
        //   else
        //       return 1;
    }

    @Override
    public void add(TLRPC.TL_dialog object) {
        //ignoreUsers.add(object);
        // super.add(object);

    }

    protected ImageTagFactory imageTagFactory;
    protected ImageManager imageManager;

    public int getCount() {
        return MessagesController.getInstance().dialogs.size();
    }

    public TLRPC.TL_dialog getItem(int index) {
        return MessagesController.getInstance().dialogs.get(index);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        TLRPC.TL_dialog user = getItem(position);
        //int type = getItemViewType(position);
        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);


            row = inflater.inflate(R.layout.messages_list_row, parent, false);
        }
        ViewHolder holder = (ViewHolder) row.getTag();
        if (holder == null) {
            holder = new ViewHolder(row);
            row.setTag(holder);
        }
        holder.user = user;
        holder.update();
        if (holder.avatar != null) {
            ImageLoaderInitializer.getInstance().initImageLoader(R.drawable.user_blue, 100, 100);
            ImageTag tag = ImageLoaderInitializer.getInstance().imageTagFactory.build(getItem(position).avatarUrl, ApplicationLoader.applicationContext);
            holder.avatar.setTag(tag);
            ImageLoaderInitializer.getInstance().getImageLoader().getLoader().load(holder.avatar);
        }
        // com.yahala.ui.lazylist.ImageLoader.getInstance().DisplayImage(getItem(position).avatarUrl, holder.avatar);
        //notifyDataSetChanged();
        //wrapper = (RelativeLayout) row.findViewById(R.id.message);

        // ViewHolder holder=new ViewHolder();


        // holder.message.setText(user.first_name);
        // holder.msg_timestamp.setText(new SimpleDateFormat("HH:mm").format(coment.time.getTime()));
        // row.setTag();
        //countryName.setBackgroundResource(coment.left ? R.drawable.bubble_gray : R.drawable.bubble_grayr);

        // Typeface typeFace=Typeface.createFromAsset(this.getContext().getAssets(), "fonts/Roboto-Regular.ttf");
        //message.setTypeface(typeFace);


        //wrapper.setGravity(coment.left ? Gravity.LEFT : Gravity.RIGHT);

        return row;
    }

    public static class ViewHolder {
        public TLRPC.TL_dialog user;
        public TextView unreadCount;
        protected TextView available;
        protected TextView displayName;
        protected boolean outgoing;
        protected ImageView checkImg;
        protected ImageView halfcheckImg;
        private TextView status;
        private PictureImplCircleImageView avatar;
        private ImageView presenceImg;

        public ViewHolder(View view) {
            displayName = (TextView) view.findViewById(R.id.displayName);
            status = (TextView) view.findViewById(R.id.statusMessage);
            avatar = (PictureImplCircleImageView) view.findViewById(R.id.img);

            //avatar.setBorderColor(0x00000000);
            available = (TextView) view.findViewById(R.id.presence);
            checkImg = (ImageView) view.findViewById(R.id.checkImg);
            halfcheckImg = (ImageView) view.findViewById(R.id.halfcheckImg);
            presenceImg = (ImageView) view.findViewById(R.id.presenceImg);
            unreadCount = (TextView) view.findViewById(R.id.unreadCount);
            Typeface typefaceR = Typeface.createFromAsset(ApplicationLoader.applicationContext.getAssets(), "fonts/Roboto-Regular.ttf");
            Typeface typefaceL = Typeface.createFromAsset(ApplicationLoader.applicationContext.getAssets(), "fonts/Roboto-Light.ttf");
            available.setTypeface(typefaceL);
            unreadCount.setTypeface(typefaceR);
            status.setTypeface(typefaceL);
            displayName.setTypeface(typefaceR);

        }

        public void update() {
            displayName.setText(user.fname + " " + user.lname);
            avatar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    UserProfileActivity fragment = new UserProfileActivity();
                    Bundle args = new Bundle();
                    args.putString("user_id", user.jid);
                    fragment.setArguments(args);
                    ((LaunchActivity) parentFragment).presentFragment(fragment, "user_" + Math.random(), false);

                }
            });
            if (user.topMessage.getType() == 0 || user.topMessage.getType() == 1) {
                status.setText(EmojiManager.getInstance().replaceEmoji(user.topMessage.getMessage(), status.getPaint().getFontMetricsInt(), OSUtilities.dp(42)));
            } else if (user.topMessage.getType() == 2 || user.topMessage.getType() == 3) {
                status.setText(LocaleController.getString("NotificationMessagePhoto", R.string.ChatPhoto));
            } else if (user.topMessage.getType() == 4 || user.topMessage.getType() == 5) {
                status.setText(LocaleController.getString("NotificationMessageMap", R.string.ChatLocation));
            } else if (user.topMessage.getType() == 6 || user.topMessage.getType() == 7) {
                status.setText(LocaleController.getString("NotificationMessageVideo", R.string.ChatVideo));
            } else if (user.topMessage.getType() == 16 || user.topMessage.getType() == 17) {
                status.setText(LocaleController.getString("AttachDocument", R.string.AttachDocument));
            } else if (user.topMessage.getType() == 18 || user.topMessage.getType() == 19) {
                status.setText(LocaleController.getString("AttachDocument", R.string.AttachAudio));
            }
            String unavailableUri = "drawable://" + R.drawable.presence_unavailable;
            String availableUri = "drawable://" + R.drawable.presence_available;
            String awayUri = "drawable://" + R.drawable.presence_away;
            try {
                try {
                    if (user.presence == null) {
                        // Bitmap preIcon = BitmapFactory.decodeResource(ApplicationLoader.applicationContext.getResources(),
                        // R.drawable.ic_type_unavailable);
                        //presenceImg.setImageBitmap(ImageLoader.getInstance().loadImageSync(unavailableUri));
                        presenceImg.setImageResource(R.drawable.presence_unavailable);
                    } else if (user.presence.getType() == Presence.Type.available) {
                        if (user.presence.getMode() == Presence.Mode.away) {
                            presenceImg.setImageResource(R.drawable.presence_away);
                            //presenceImg.setImageBitmap(ImageLoader.getInstance().loadImageSync(awayUri));}
                        } else if (user.presence.getMode() == Presence.Mode.dnd) {
                            presenceImg.setImageResource(R.drawable.presence_dnd);
                            //presenceImg.setImageBitmap(ImageLoader.getInstance().loadImageSync(awayUri));}
                        } else {
                            // presenceImg.setImageBitmap(ImageLoader.getInstance().loadImageSync(availableUri));
                            presenceImg.setImageResource(R.drawable.presence_available);
                        }
                    } else if (user.presence.getType() == Presence.Type.unavailable) {


                        //Bitmap preIcon = BitmapFactory.decodeResource(ApplicationLoader.applicationContext.getResources(),
                        //  R.drawable.ic_type_unavailable);
                        presenceImg.setImageResource(R.drawable.presence_unavailable);
                        ////presenceImg.setImageBitmap(ImageLoader.getInstance().loadImageSync(unavailableUri));
                        // available.setText(XmppManager.getInstance().getLastSeen(user.jid));
                    } else {
                        //available.setText(XmppManager.getInstance().getLastSeen(user.jid));

                        // presenceImg.setVisibility(View.GONE);
                    }
                   /* if (user.avatar != null) {
                        Utilities.RunOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                avatar.setImageBitmap(user.avatar);
                            }
                        });
                    }*/
                        /*  else
                           {
                               Bitmap icon = BitmapFactory.decodeResource(ApplicationLoader.applicationContext.getResources(),
                                       R.drawable.user_placeholder);
                               user.avatar=icon;
                           }*/
                } catch (Exception e) {
                    e.printStackTrace();
                    Bitmap icon = ImageLoader.getInstance().loadImageSync("drawable://" + R.drawable.user_blue);
                    user.avatar = icon;
                }

                if (user.unread_count > 0) {
                    unreadCount.setText(Integer.toString(user.unread_count));
                    unreadCount.setVisibility(View.VISIBLE);
                    avatar.setBottomCircleOffColor(ApplicationLoader.applicationContext.getResources().getColor(R.color.SkyBlue2));
                    avatar.setBottomCircleIndent(OSUtilities.dp(1));
                    // avatar.setBottomCircleOnColor(0x1996f9ef);
                    // avatar.setbottomCircleOffColor="#1996f9ef"
                    /*vincestyling:bottomCircleOnColor="#3396f9ef"
                    vincestyling:bottomCircleIndent="5dp"
                    vincestyling:middleCircleOffColor="#ffffff"
                    vincestyling:middleCircleOnColor="#d3d3d3"
                    vincestyling:middleCircleIndent="2dp"*/
                    //avatar.setBorderWidth(2);
                    //avatar.setBorderColor(ApplicationLoader.applicationContext.getResources().getColor(R.color.SkyBlue2));
                    status.setTextColor(ApplicationLoader.applicationContext.getResources().getColor(R.color.SkyBlue2));
                } else {
                    // avatar.setBorderWidth(0);
                    //  avatar.setBorderColor(0x00000000);
                    avatar.setBottomCircleIndent(OSUtilities.dp(1));
                    avatar.setBottomCircleOffColor(ApplicationLoader.applicationContext.getResources().getColor(R.color.transparent));

                    status.setTextColor(ApplicationLoader.applicationContext.getResources().getColor(R.color.LightBlack));
                    unreadCount.setVisibility(View.GONE);
                }
                PrettyTime p = new PrettyTime();

                //prints: “moments from now”

                available.setText(p.format(user.topMessage.getDate()));

             /* checkImg.setVisibility(View.GONE);
                halfcheckImg.setVisibility(View.GONE);*/

                //FileLog.e("topMessage.getOut", user.topMessage.getOut() + " " + user.topMessage.getRead_state());
                if (user.topMessage.getOut() == 1) {
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

                    if (user.topMessage.getSend_state() == XMPPManager.MESSAGE_SEND_STATE_AKN) {
                        checkImg.setVisibility(View.VISIBLE);
                        halfcheckImg.setVisibility(View.VISIBLE);

                        if (LocaleController.isRTL) {
                            lp.setMargins(OSUtilities.dp(9), 0, 0, 0);
                        } else {
                            lp.setMargins(0, 0, (int) OSUtilities.dpf2(-9L), 0);
                        }

                        checkImg.setLayoutParams(lp);
                    } else if (user.topMessage.getSend_state() == XMPPManager.MESSAGE_SEND_STATE_SENT) {


                        if (!LocaleController.isRTL) {
                            lp.setMargins(OSUtilities.dp(5), 0, 0, 0);
                            checkImg.setLayoutParams(lp);
                        } else {
                            lp.setMargins(0, 0, OSUtilities.dp(5), 0);
                            checkImg.setLayoutParams(lp);
                        }

                        checkImg.setVisibility(View.VISIBLE);
                        halfcheckImg.setVisibility(View.GONE);
                    } else {
                        checkImg.setVisibility(View.GONE);
                        halfcheckImg.setVisibility(View.GONE);
                    }
                } else {
                    checkImg.setVisibility(View.GONE);
                    halfcheckImg.setVisibility(View.GONE);
                }
                //prints: “10 minutes from now”
                //   if (user.avatar != null)
                //   {  holder.avatar.setImageBitmap(user.avatar);}
                //  else
                //   {
                /*Bitmap icon = BitmapFactory.decodeResource(ApplicationLoader.applicationContext.getResources(),
                        R.drawable.user_placeholder);
                avatar.setImageBitmap(icon);*/

                //      user.avatar=icon;
                //}
            } catch (Exception e) {
                e.printStackTrace();
               /* Bitmap icon = BitmapFactory.decodeResource(ApplicationLoader.applicationContext.getResources(),
                        R.drawable.user_placeholder);*/
                //     user.avatar=icon;

            }
        }
    }

    /*public Bitmap decodeToBitmap(byte[] decodedByte) {
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
    }*/
}
