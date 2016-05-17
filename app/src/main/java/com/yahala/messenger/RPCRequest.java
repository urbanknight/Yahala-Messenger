/*
 * This is the source code of Telegram for Android v. 1.3.2.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013.
 */

package com.yahala.messenger;

import java.util.ArrayList;

public class RPCRequest {
    public static int RPCRequestClassGeneric = 1;
    public static int RPCRequestClassDownloadMedia = 2;
    public static int RPCRequestClassUploadMedia = 4;
    public static int RPCRequestClassTransportMask = (RPCRequestClassGeneric | RPCRequestClassDownloadMedia | RPCRequestClassUploadMedia);
    public static int RPCRequestClassEnableUnauthorized = 8;
    public static int RPCRequestClassFailOnServerErrors = 16;
    public static int RPCRequestClassCanCompress = 32;
    public long token;
    public boolean cancelled;
    public int serverFailureCount;
    public int flags;
    public int retryCount = 0;
    public TLObject rawRequest;
    public TLObject rpcRequest;
    public int serializedLength;
    public RPCRequestDelegate completionBlock;
    public RPCProgressDelegate progressBlock;
    public RPCQuickAckDelegate quickAckBlock;
    public long runningMessageId;
    public int runningMessageSeqNo;
    public int runningDatacenterId;
    public int transportChannelToken;
    public int runningStartTime;
    public int runningMinStartTime;
    public boolean confirmed;
    public boolean initRequest = false;
    public ArrayList<Long> respondsToMessageIds = new ArrayList<Long>();
    boolean requiresCompletion;

    public void addRespondMessageId(long messageId) {
        respondsToMessageIds.add(messageId);
    }

    boolean respondsToMessageId(long messageId) {
        return runningMessageId == messageId || respondsToMessageIds.contains(messageId);
    }

    public interface RPCRequestDelegate {
        void run(TLObject response, TLRPC.TL_error error);
    }

    public interface RPCProgressDelegate {
        void progress(int length, int progress);
    }

    public interface RPCQuickAckDelegate {
        void quickAck();
    }
}

