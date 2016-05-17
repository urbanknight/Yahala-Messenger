package com.yahala.xmpp;

import com.yahala.messenger.TLObject;
import com.yahala.messenger.TLRPC;

import java.util.HashMap;

/**
 * Created by user on 4/11/2014.
 */
public class YHC {
    public static class Chat extends TLObject {
        public int id;
        public String title;
        public int date;
        public HashMap<String, TLRPC.User> participants;
    }

    public static class group extends TLObject {
        public int id;
        public String title;
        public int date;
        public HashMap<String, TLRPC.User> participants;

    }
}
