package com.yahala.ui.Adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.yahala.SQLite.Messages;
import com.yahala.messenger.R;
import com.yahala.ui.CircleImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

/**
 * Created by user on 4/10/2014.
 */
public class ChatAdapter extends ArrayAdapter<Messages> {

    //private TextView message;
    private ArrayList<Messages> messages = new ArrayList<Messages>();

    private RelativeLayout wrapper;

    public ChatAdapter(Context context, int textViewResourceId) {
        super(context, textViewResourceId);
    }

    public ChatAdapter(Context context, int textViewResourceId, ArrayList<Messages> messages) {
        super(context, textViewResourceId);
        this.messages = messages;
    }

    @Override
    public int getViewTypeCount() {
        return 3;
    }

    @Override
    public int getItemViewType(int position) {

        if (getItem(position).getOut() == 1) {
            return 1;
        } else
            return 0;
    }

    @Override
    public void add(Messages object) {
        messages.add(object);
        super.add(object);

    }

    public int getCount() {
        return this.messages.size();
    }

    public Messages getItem(int index) {
        return this.messages.get(index);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        Messages message = getItem(position);
        int type = getItemViewType(position);
        if (row == null) {
            LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            if (message.getOut() != 1) {
                //row = inflater.inflate(R.layout.chat_incoming_row, parent, false);
            } else {
                // row = inflater.inflate(R.layout.chat_outgoing_row, parent, false);
            }
            //row.setOnClickListener(this);
        }

        //wrapper = (RelativeLayout) row.findViewById(R.id.message);

        ViewHolder holder = new ViewHolder();

        holder.message = (TextView) row.findViewById(R.id.message_text);
        //holder.msg_timestamp = (TextView) row.findViewById(R.id.message_time);
        holder.img = (CircleImageView) row.findViewById(R.id.img);
        try {
            holder.message.setText(message.getMessage());
            holder.msg_timestamp.setText(new SimpleDateFormat("HH:mm").format(message.getDate().getTime()));
        } catch (Exception e) {
            e.printStackTrace();
        }


        //  FileLog.e("Test","message.user.jid : "+message.user.jid);
        try {
            holder.img.setImageBitmap(message.user.avatar);
        } catch (Exception e) {
        }


        // row.setTag();
        //countryName.setBackgroundResource(coment.left ? R.drawable.bubble_gray : R.drawable.bubble_grayr);

        // Typeface typeFace=Typeface.createFromAsset(this.getContext().getAssets(), "fonts/Roboto-Regular.ttf");
        //message.setTypeface(typeFace);


        //wrapper.setGravity(coment.left ? Gravity.LEFT : Gravity.RIGHT);

        return row;
    }

    public Bitmap decodeToBitmap(byte[] decodedByte) {
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
    }

    static class ViewHolder {
        protected CircleImageView img;
        protected TextView message;
        protected boolean outgoing;
        private TextView msg_timestamp;
    }

}