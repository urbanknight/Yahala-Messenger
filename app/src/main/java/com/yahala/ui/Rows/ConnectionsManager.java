package com.yahala.ui.Rows;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.yahala.messenger.FileLog;
import com.yahala.messenger.RPCRequest;
import com.yahala.messenger.UserConfig;
import com.yahala.ui.ApplicationLoader;

import com.yahala.messenger.Utilities;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by user on 4/5/2014.
 */
public class ConnectionsManager {
    public static long lastPauseTime = 0;// System.currentTimeMillis();
    public static boolean appPaused = true;
    private static volatile ConnectionsManager Instance = null;
    public int timeDifference = 0;
    //================================================================================
    // Requests manage
    //================================================================================
    int lastClassGuid = 1;
    private ConcurrentHashMap<Integer, ArrayList<Long>> requestsByGuids = new ConcurrentHashMap<Integer, ArrayList<Long>>(100, 1.0f, 2);
    private ConcurrentHashMap<Long, Integer> requestsByClass = new ConcurrentHashMap<Long, Integer>(100, 1.0f, 2);
    private ArrayList<RPCRequest> requestQueue = new ArrayList<RPCRequest>();
    private ArrayList<RPCRequest> runningRequests = new ArrayList<RPCRequest>();

    public static ConnectionsManager getInstance() {
        ConnectionsManager localInstance = Instance;
        if (localInstance == null) {
            synchronized (ConnectionsManager.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new ConnectionsManager();
                }
            }
        }
        return localInstance;
    }

    public static void resetLastPauseTime() {
        if (appPaused) {
            return;
        }
        FileLog.e("tmessages", "reset app pause time");
        if (lastPauseTime != 0 && System.currentTimeMillis() - lastPauseTime > 5000) {
            //ContactsController.getInstance().checkContacts();
        }
        lastPauseTime = 0;
        //ConnectionsManager.getInstance().applicationMovedToForeground();
    }

    public int getCurrentTime() {
        return (int) (System.currentTimeMillis() / 1000) + timeDifference;
    }

    public void switchBackend() {
        Utilities.stageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                Utilities.stageQueue.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        UserConfig.clearConfig();
                        System.exit(0);
                    }
                });
            }
        });
    }

    public int generateClassGuid() {
        int guid = lastClassGuid++;
        ArrayList<Long> requests = new ArrayList<Long>();
        requestsByGuids.put(guid, requests);
        return guid;
    }

    public static boolean isConnectedToWiFi() {
        try {
            ConnectivityManager cm = (ConnectivityManager) ApplicationLoader.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo netInfo = cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            if (netInfo != null && netInfo.getState() == NetworkInfo.State.CONNECTED) {
                return true;
            }
        } catch (Exception e) {
            FileLog.e("tmessages", e);
            return true;
        }
        return false;
    }

    public void cancelRpc(final long token, final boolean notifyServer) {
        Utilities.stageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                boolean found = false;

                for (int i = 0; i < requestQueue.size(); i++) {
                    RPCRequest request = requestQueue.get(i);
                    if (request.token == token) {
                        found = true;
                        request.cancelled = true;
                        FileLog.d("tmessages", "===== Cancelled queued rpc request " + request.rawRequest);
                        requestQueue.remove(i);
                        break;
                    }
                }

                for (int i = 0; i < runningRequests.size(); i++) {
                    RPCRequest request = runningRequests.get(i);
                    if (request.token == token) {
                        found = true;

                        FileLog.d("tmessages", "===== Cancelled running rpc request " + request.rawRequest);

                      /*  if ((request.flags & RPCRequest.RPCRequestClassGeneric) != 0) {
                            if (notifyServer) {
                                TLRPC.TL_rpc_drop_answer dropAnswer = new TLRPC.TL_rpc_drop_answer();
                                dropAnswer.req_msg_id = request.runningMessageId;
                                performRpc(dropAnswer, null, null, false, request.flags);
                            }
                        }*/

                        request.cancelled = true;
                        runningRequests.remove(i);
                        break;
                    }
                }
                if (!found) {
                    FileLog.d("tmessages", "***** Warning: cancelling unknown request");
                }
            }
        });
    }

    public void cancelRpcsForClassGuid(int guid) {
        ArrayList<Long> requests = requestsByGuids.get(guid);
        if (requests != null) {
            for (Long request : requests) {
                cancelRpc(request, true);
            }
            requestsByGuids.remove(guid);
        }
    }

    public void bindRequestToGuid(final Long request, final int guid) {
        Utilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                ArrayList<Long> requests = requestsByGuids.get(guid);
                if (requests != null) {
                    requests.add(request);
                    requestsByClass.put(request, guid);
                }
            }
        });
    }

    public void removeRequestInClass(final Long request) {
        Utilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                Integer guid = requestsByClass.get(request);
                if (guid != null) {
                    ArrayList<Long> requests = requestsByGuids.get(guid);
                    if (requests != null) {
                        requests.remove(request);
                    }
                }
            }
        });
    }
}
