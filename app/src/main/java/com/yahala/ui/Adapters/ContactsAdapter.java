package com.yahala.ui.Adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.yahala.ImageLoader.ImageLoaderInitializer;
import com.yahala.ImageLoader.model.ImageTag;
import com.yahala.messenger.ContactsController;
import com.yahala.messenger.R;
import com.yahala.messenger.TLRPC;
import com.yahala.ui.ApplicationLoader;
import com.yahala.ui.Views.CircleImageView.PictureImplCircleImageView;
import com.yahala.ui.Views.PinnedSectionListView;
import com.yahala.xmpp.XMPPManager;
import com.yahala.xmpp.call.WebRtcPhone;

import org.jivesoftware.smack.packet.Presence;

import java.util.ArrayList;

/**
 * Created by user on 4/9/2014.
 */
public class ContactsAdapter extends ArrayAdapter<TLRPC.User> implements PinnedSectionListView.PinnedSectionListAdapter {

    //private TextView message;
    //private List<TLRPC.User> ignoreUsers = new ArrayList<TLRPC.User>();
    public boolean isScrolling = false;
    private RelativeLayout wrapper;

    public ContactsAdapter(Context context, int textViewResourceId, ArrayList<TLRPC.User> ignoreUsers) {
        super(context, textViewResourceId);
        // this.ignoreUsers = ignoreUsers;


    }

    @Override
    public int getViewTypeCount() {
        //this.getPosition()
        //    if (this.==2)
        return 4;
    }

    @Override
    public int getItemViewType(int position) {

        if (position == 1)
            return 1;
        else if (position == 0)
            return 2;
      /*  if (position == 2)
            return 1;
        else if (position == 0)
            return 2;
        else if (position == 1)
            return 3;
*/

        return 0;
    }

    @Override
    public void add(TLRPC.User object) {
        //   com.yahala.xmpp.ContactsController.getInstance().friends.add(object);
        // super.add(object);

    }

    @Override
    public boolean isItemViewTypePinned(int viewType) {
        if (viewType == 1)
            return true;

        return false;

    }

    public int getCount() {
        return com.yahala.xmpp.ContactsController.getInstance().friends.size();
    }

    public TLRPC.User getItem(int index) {
        return com.yahala.xmpp.ContactsController.getInstance().friends.get(index);
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View row = convertView;
        int type = 0;
        try {
            if (position == 1)
                type = 1;
            else if (position == 0)
                type = 2;
        } catch (Exception e) {
            e.printStackTrace();
        }
        //  FileLog.e("Test","type:" + type+", user: "+user.first_name + " " +user.last_name );

        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (type == 1) {
                row = inflater.inflate(R.layout.section_header, parent, false);


            } else if (type == 2) {
                row = inflater.inflate(R.layout.contacts_invite_row_layout, parent, false);

            } else if (type == 3) {
                row = inflater.inflate(R.layout.add_group_chat_row_layout, parent, false);
            } else {
                row = inflater.inflate(R.layout.contact_list_row, parent, false);
            }


        }
/* with group
        int type = 0;
        try {
            if (position == 2)
                type = 1;
            else if (position == 0)
                type = 2;
            else if (position == 1)
                type = 3;
        } catch (Exception e) {
            e.printStackTrace();
        }
        //  FileLog.e("Test","type:" + type+", user: "+user.first_name + " " +user.last_name );

        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (type == 1) {
                row = inflater.inflate(R.layout.section_header, parent, false);


            } else if (type == 2) {
                row = inflater.inflate(R.layout.contacts_invite_row_layout, parent, false);

            } else if (type == 3) {

                row = inflater.inflate(R.layout.add_group_chat_row_layout, parent, false);
            } else {
                row = inflater.inflate(R.layout.contact_list_row, parent, false);
            }


        }*/
        ViewHolder holder = (ViewHolder) row.getTag();
        if (holder == null) {
            holder = new ViewHolder(row, type);
            holder.user = getItem(position);
            row.setTag(holder);
        } else {
            holder.user = getItem(position);
           /* int num = 7;
            Random rand = new Random();
            int ran = rand.nextInt(num);
            DisplayImageOptions options = new DisplayImageOptions.Builder()
                    .cacheInMemory(false)
                    .cacheOnDisk(true)
                    .considerExifParams(true)
                    .displayer(new FadeInBitmapDisplayer(150))
                    .showImageForEmptyUri(Utilities.arrUsersAvatars[ran])
                    .showImageOnFail(Utilities.arrUsersAvatars[ran])
                            // .showImageOnLoading(Utilities.arrUsersAvatars[ran])
                    .imageScaleType(ImageScaleType.EXACTLY)
                    .bitmapConfig(Bitmap.Config.ARGB_8888)
                    .preProcessor(new BitmapProcessor() {
                        public Bitmap process(Bitmap src) {
                            return Utilities.scaleCenterCrop(src, Utilities.dp(50), Utilities.dp(50));
                        }
                    })
                    .build();
            // FileLog.d("ImageLoader",userd.avatarUrl+ " ");
           // if(holder.avatar!=null)
            //    ImageLoader.getInstance().displayImage("file:///" +holder.user.avatarUrl,holder.avatar, options);*/

        }
        holder.position = position;
        holder.update(isScrolling);
        if (holder.avatar != null) {
            ImageLoaderInitializer.getInstance().initImageLoader(R.drawable.user_blue, 100, 100);
            ImageTag tag = ImageLoaderInitializer.getInstance().imageTagFactory.build(getItem(position).avatarUrl, ApplicationLoader.applicationContext);
            holder.avatar.setTag(tag);
            ImageLoaderInitializer.getInstance().getImageLoader().getLoader().load(holder.avatar);
        }
        // com.yahala.ui.lazylist.ImageLoader.getInstance().DisplayImage(getItem(position).avatarUrl, holder.avatar);

        /*new AsyncTask<ViewHolder, Void, Bitmap>() {
            private ViewHolder v;

            @Override
            protected Bitmap doInBackground(ViewHolder... params) {
                v = params[0];
                return  getItem(v.position).avatar;
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                super.onPostExecute(result);
                if (v.position == position) {
                    // If this item hasn't been recycled already, hide the
                    // progress and set and show the image
                    //v.progress.setVisibility(View.GONE);
                    //v.icon.setVisibility(View.VISIBLE);
                  if(v.avatar!=null)
                      v.avatar.setImageBitmap(result);
                }
            }
        }.execute(holder);*/

        return row;
    }

    @Override
    public boolean isEnabled(int position) {
        if (/*position == 0 || position == 1 ||*/ position == 1) {
            return false;
        }
        return true;
    }

    public Bitmap decodeToBitmap(byte[] decodedByte) {
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
    }

    public static class ViewHolder {
        public TLRPC.User user;
        public int type;
        int position;
        protected TextView available;
        protected TextView displayName;
        protected boolean outgoing;
        protected TextView sectionTitle;
        protected TextView countText;
        Handler handler = new Handler() {
            @Override
            public void handleMessage(Message message) {
                // do setImageBitmap(bitmap)
            }
        };
        private TextView status;
        private PictureImplCircleImageView avatar;
        private ImageView presenceImg;
        private ImageView callVoice;
        private ImageView callVideo;

        public ViewHolder(View view, int type) {
            this.type = type;

            displayName = (TextView) view.findViewById(R.id.displayName);
            status = (TextView) view.findViewById(R.id.statusMessage);
            avatar = (PictureImplCircleImageView) view.findViewById(R.id.img);
            Typeface typefaceR = Typeface.createFromAsset(ApplicationLoader.applicationContext.getAssets(), "fonts/Roboto-Regular.ttf");
            Typeface typefaceL = Typeface.createFromAsset(ApplicationLoader.applicationContext.getAssets(), "fonts/Roboto-Light.ttf");

            if (avatar != null)

                //avatar.setBorderColor(0x00000000);
                //available = (TextView) view.findViewById(R.id.presence);
                presenceImg = (ImageView) view.findViewById(R.id.presenceImg);
            if (type == 1 || type == 2 || type == 3) {
                sectionTitle = (TextView) view.findViewById(R.id.messages_list_row_name);
                sectionTitle.setTypeface(typefaceR);
            } else if (type == 0) {

                callVoice = (ImageView) view.findViewById(R.id.callVoice);
                callVideo = (ImageView) view.findViewById(R.id.callVideo);
                callVoice.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (com.yahala.xmpp.ContactsController.getInstance().friendsDict.containsKey(user.jid)) {

                            TLRPC.User obj = com.yahala.xmpp.ContactsController.getInstance().friendsDict.get(user.jid);

                            if (obj.presence.getType() == Presence.Type.available && XMPPManager.getInstance().isConnected()) {
                                WebRtcPhone.getInstance().Call(user.jid, true, false);
                            } else {
                                Toast toast = Toast.makeText(ApplicationLoader.applicationContext, R.string.offline, Toast.LENGTH_LONG);
                                toast.show();
                            }
                        }
                    }
                });
                callVideo.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (com.yahala.xmpp.ContactsController.getInstance().friendsDict.containsKey(user.jid)) {

                            TLRPC.User obj = com.yahala.xmpp.ContactsController.getInstance().friendsDict.get(user.jid);

                            if (obj.presence.getType() == Presence.Type.available && XMPPManager.getInstance().isConnected()) {
                                WebRtcPhone.getInstance().Call(user.jid, true, true);
                            } else {
                                Toast toast = Toast.makeText(ApplicationLoader.applicationContext, R.string.offline, Toast.LENGTH_LONG);
                                toast.show();
                            }
                        }
                    }
                });
            } else {
                displayName.setTypeface(typefaceR);
                status.setTypeface(typefaceL);
            }
            if (type == 1) {
                countText = (TextView) view.findViewById(R.id.count_text);
                countText.setTypeface(typefaceR);
            }

        }

        public void update(boolean isScrolling) {
            if (type == 1) {//try{
                countText.setText(user.phone);
            }
            //  catch (Exception e){}
            // }
            if (type == 1 || type == 2 || type == 3) {
                try {
                    sectionTitle.setText(user.last_name);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (type == 0) {
                displayName.setText(user.first_name + " " + user.last_name);
                if (true/*!isScrolling*/) {


                    try {
                        String unavailableUri = "drawable://" + R.drawable.presence_unavailable;
                        String availableUri = "drawable://" + R.drawable.presence_available;
                        String awayUri = "drawable://" + R.drawable.presence_away;
                        //  Utilities.xmppQueue.postRunnable(new Runnable() {
                        //     @Override
                        //     public void run() {
                        final String lastSeenMessage = user.last_seen;
                        //FileLog.e("lastSeenMessage", lastSeenMessage);


                        if (user.presence == null) {


                            status.setText(lastSeenMessage);

                            presenceImg.setVisibility(View.INVISIBLE);
                            //presenceImg.setImageBitmap(ImageLoader.getInstance().loadImageSync(unavailableUri));
                        } else {
                            if (user.presence.getType() == Presence.Type.available) {   // GradientDrawable bgShape = (GradientDrawable) available.getBackground();
                                // bgShape.setColor(0xFF81B85C);
                                // available.setTextColor(Color.WHITE);
                                // available.setText(LocaleController.getString("Online", R.string.Online));
                                // Bitmap preIcon = BitmapFactory.decodeResource(ApplicationLoader.applicationContext.getResources(),
                                //      R.drawable.ic_type_available);
                                presenceImg.setVisibility(View.VISIBLE);
                                try {
                                    status.setText(user.presence.getStatus());
                                } catch (Exception e) {

                                }
                                if (user.presence.getMode() == Presence.Mode.away) {
                                    presenceImg.setImageResource(R.drawable.presence_away);
                                    //presenceImg.setImageBitmap(ImageLoader.getInstance().loadImageSync(awayUri));}
                                } else if (user.presence.getMode() == Presence.Mode.dnd) {
                                    presenceImg.setImageResource(R.drawable.presence_dnd);
                                    //presenceImg.setImageBitmap(ImageLoader.getInstance().loadImageSync(awayUri));}
                                } else {
                                    presenceImg.setImageResource(R.drawable.presence_available);
                                }

                            } else if (user.presence.getType() == Presence.Type.unavailable) {

                                // available.setText(LocaleController.getString("Offline", R.string.Offline));
                                //  Bitmap preIcon = BitmapFactory.decodeResource(ApplicationLoader.applicationContext.getResources(),
                                //          R.drawable.ic_type_unavailable);
                                presenceImg.setVisibility(View.INVISIBLE);
                                //presenceImg.setImageBitmap(ImageLoader.getInstance().loadImageSync(unavailableUri));

                                if (lastSeenMessage != null && !lastSeenMessage.isEmpty())
                                    status.setText(lastSeenMessage);

                            } else {

                                status.setText(XMPPManager.getInstance().getLastSeenMessage(user.jid));//available.setText(user.last_seen );

                                presenceImg.setVisibility(View.INVISIBLE);
                                // presenceImg.setImageBitmap(ImageLoader.getInstance().loadImageSync(unavailableUri));
                            }


                        }
                        //     }
                        //  });

                        //     }
                        //   });
                        // avatar.setImageBitmap(user.avatar);
                      /*  Utilities.RunOnUIThread(new Runnable() {
                            @Override
                            public void run() {

                            }
                        });*/

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                // try {
                // Bitmap icon = user.avatar;


                //      user.avatar=icon;
                //  }
                /*} catch (Exception e) {
                    e.printStackTrace();
                    Bitmap icon = BitmapFactory.decodeResource(ApplicationLoader.applicationContext.getResources(),
                            R.drawable.user_placeholder);
                    //     user.avatar=icon;

                }*/
            }
        }


    }

}
