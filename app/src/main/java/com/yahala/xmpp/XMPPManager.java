package com.yahala.xmpp;


import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Environment;
import android.util.SparseArray;
import android.widget.Toast;

import com.yahala.SQLite.Messages;
import com.yahala.android.ConnectionState;
import com.yahala.android.LocaleController;
import com.yahala.android.OSUtilities;
import com.yahala.messenger.DispatchQueue;
import com.yahala.messenger.FileLog;
import com.yahala.messenger.NotificationCenter;
import com.yahala.messenger.R;
import com.yahala.messenger.TLRPC;
import com.yahala.messenger.UserConfig;
import com.yahala.messenger.Utilities;
import com.yahala.objects.MessageObject;
import com.yahala.ui.ApplicationLoader;
import com.yahala.xmpp.Util.ConnectivityManagerUtil;
import com.yahala.xmpp.call.CallParameter;
import com.yahala.xmpp.customProviders.Location;
import com.yahala.xmpp.customProviders.OutOfBandData;
import com.yahala.xmpp.customProviders.PhoneCall;
import com.yahala.xmpp.customProviders.UserLocationExtension;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat.Chat;
import org.jivesoftware.smack.chat.ChatManager;
import org.jivesoftware.smack.chat.ChatMessageListener;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smack.roster.RosterGroup;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.TLSUtils;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.ChatStateManager;
import org.jivesoftware.smackx.iqlast.LastActivityManager;
import org.jivesoftware.smackx.iqlast.packet.LastActivity;
import org.jivesoftware.smackx.iqregister.AccountManager;
import org.jivesoftware.smackx.ping.PingFailedListener;
import org.jivesoftware.smackx.ping.PingManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptManager;
import org.jivesoftware.smackx.receipts.DeliveryReceiptRequest;
import org.jivesoftware.smackx.receipts.ReceiptReceivedListener;
import org.jivesoftware.smackx.search.ReportedData;
import org.jivesoftware.smackx.search.UserSearch;
import org.jivesoftware.smackx.search.UserSearchManager;
import org.jivesoftware.smackx.vcardtemp.packet.VCard;
import org.jivesoftware.smackx.vcardtemp.provider.VCardProvider;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xhtmlim.XHTMLManager;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import org.jxmpp.util.XmppStringUtils;
import org.ocpsoft.prettytime.PrettyTime;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLContext;

import javax.net.ssl.TrustManagerFactory;

import eu.geekplace.javapinning.JavaPinning;

public class XMPPManager /*implements PacketListener*/ {

    public static final int Available = 0;
    public static final int FreeToChat = 1;
    public static final int DoNotDisturb = 2;
    public static final int Away = 3;
    public static final int unavailable = 4;
    public int currentPresence = -1;
    public static Boolean foreground = false;
    public static final int userAuthenticated = 1112;

    public static final int MESSAGE_SEND_STATE_SENDING = 1;
    public static final int MESSAGE_SEND_STATE_SENT = 0;
    public static final int MESSAGE_SEND_STATE_SEND_ERROR = 2;
    public static final int MESSAGE_SEND_STATE_AKN = 3;


    public static final int messageReceivedByAck = 12001;
    public static final int messageReceivedByServer = 12002;
    public static final int messageSendError = 12003;
    public static final int didReceivedNewMessages = 12004;

    public static final int messageComposing = 312;
    public static final int messageComposingCancelled = 313;
    public static final int messageRead = 314;
    public static final int presenceRequestSent = 1201;
    public static final int presenceDidChanged = 1202;
    public static final int uploadProgressDidChanged = 1203;
    public static final int connectionStateDidChanged = 1204;
    public static final int currentUserPresenceDidChanged = 1205;
    public static final int accountCreated = 1001;
    public static final int connectionSuccessful = 1002;

    public static final int updateInterfaces = 1004;
    private static final int MAX_CYCLES = 30;
    // private static final int packetReplyTimeout = 20 * 1000; // millis
    public static volatile DispatchQueue xmppQueue = new DispatchQueue("xmppQueue");
    public static volatile DispatchQueue scheduledTaskQueue = new DispatchQueue("scheduledTaskQueue");
    public static boolean connectionSettingsObsolete;
    //public static String HOST ="192.168.10.101";
    public static String DOMAIN = "@yahala";
    public static String HOST = "Your Server IP";
    public static String HOSTURL = "http://" + HOST + "/";
    public static int PORT = 5222;
    public static int SPORT = 5223;
    public static String RESOURCE = "android";
    //public static String SERVICE_NAME = "192.168.10.101";
    public static String SERVECE_NAME = "Your Server Name";//
    public static SecureRandom random = new SecureRandom();
    private static int sReusedConnectionCount = 0;
    private static int sNewConnectionCount = 0;
    private static volatile XMPPManager Instance = null;
    public ConcurrentHashMap<String, TLRPC.User> users = new ConcurrentHashMap<String, TLRPC.User>(100, 1.0f, 2);
    public int timeDifference = 0;
    public ConnectionState connectionState = ConnectionState.OFFLINE;
    public XMPPTCPConnection connection;
    public SparseArray<MessageObject> sendingMessages = new SparseArray<MessageObject>();
    public PingManager mPingManager;
    public int mCurrentRetryCount = 0;
    RosterListener rosterListener;
    private long lastOutgoingMessageId = 0;
    private long lastPing = new Date().getTime();
    private boolean paused = false;
    private XMPPTCPConnectionConfiguration config;
    private com.yahala.xmpp.MessageListener packetListener;
    private ConnectionCreationListener connectionCreationListener;
    private ConnectionListener connectionGeneralListener;
    private ChatManager chatManager;
    private int nextWakeUpTimeout = 60000;
    private int nextSleepTimeout = 60000;
    private AlarmManager alarmMgr;
    //private final Handler mReconnectHandler;
    //private static Looper mServiceLooper;
    //private final Runnable mReconnectRunnable = new Runnable() {
    //public void run() {
    //FileLog.e("XMPPManager", "attempting reconnection by calling connect");
    // }
    // };
    private PendingIntent alarmIntent;
    private Runnable stageRunnable = new Runnable() {
        @Override
        public void run() {
            xmppRequestStateChange(ConnectionState.ONLINE);
        }
    };

    public XMPPManager(String server, int port) {
        // SmackAndroid.init(ApplicationLoader.applicationContext);
        // SmackConfiguration.setDefaultPacketReplyTimeout(packetReplyTimeout);
        SmackConfiguration.setDefaultPacketReplyTimeout(20 * 300);
        SmackConfiguration.DEBUG = true;
        // createAlarm();
        //mServiceLooper = Looper.getMainLooper();
        this.HOST = server;
        this.PORT = port;
        //mReconnectHandler = new Handler(mServiceLooper);
    }

    public XMPPManager() {
        //SmackAndroid.init(ApplicationLoader.applicationContext);
        //SmackConfiguration.setDefaultPacketReplyTimeout(packetReplyTimeout);
        SmackConfiguration.setDefaultPacketReplyTimeout(20 * 300);
        SmackConfiguration.DEBUG = true;

        createAlarm();
        // mReconnectHandler = new Handler(mServiceLooper);
    }

    public static XMPPManager getInstance() {
        XMPPManager localInstance = Instance;
        if (localInstance == null) {
            synchronized (XMPPManager.class) {
                localInstance = Instance;
                if (localInstance == null) {
                    Instance = localInstance = new XMPPManager();
                }
            }
        }
        return localInstance;
    }

    public void createAlarm() {

    }

    // WeChat lets you recall that message you sent by accident
    private void start(ConnectionState mConnectionState) {
        switch (mConnectionState) {
            case ONLINE:
                initConnection();
                break;
            case RECONNECT_DELAYED:
                connectionState = ConnectionState.RECONNECT_DELAYED;
                Utilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(connectionStateDidChanged);
                    }
                });
                break;
            case RECONNECT_NETWORK:
                connectionState = ConnectionState.RECONNECT_NETWORK;
                Utilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(connectionStateDidChanged);
                    }
                });
                break;
            default:
                throw new IllegalStateException("xmppMgr start() Invalid State: " + connectionState.toString());
        }
    }

    public void updateChatState(String dialog_id, ChatState chatState) {
        try {
            if (isConnected()) {
                ChatMessageListener messageListener = new ChatMessageListener() {
                    @Override
                    public void processMessage(Chat chat, Message message) {

                    }
                };
                ChatManager chatManager = ChatManager.getInstanceFor(connection);
                Chat chat = chatManager.createChat(JidCreate.from(dialog_id).asEntityJidIfPossible(), messageListener);
                chat.removeMessageListener(messageListener);
                ChatStateManager.getInstance(connection).setCurrentState(chatState, chat);
                chat.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public boolean isConnected() {
        return isXmppConnected() && connectionState == ConnectionState.ONLINE;
    }

    boolean isXmppConnected() {
       /* try {
            FileLog.e("XMPPManager", "isXmppConnected " + (connection.isConnected() ? " Connected" : "Not connected"));
        } catch (Exception e) {

        }*/

        return connection != null && connection.isConnected();
    }

    public ConnectionState getConnectionStatus() {
        return connectionState;
    }

    /**
     * This method *requests* a state change - what state things actually
     * wind up in is impossible to know (eg, a request to connect may wind up
     * with a state of CONNECTED, DISCONNECTED or WAITING_TO_CONNECT...
     */
    public void xmppRequestStateChange(final ConnectionState newState) {
        xmppQueue.postRunnable(new Runnable() {
            @Override
            public void run() {

                //ConnectionState currentState = getConnectionStatus();
                FileLog.d("XMPPManager", "xmppRequestStateChange " + connectionState + " => " + newState.toString());
                switch (newState) {
                    case ONLINE:
                        FileLog.e("XMPPManager", "ONLINE case");
                        if (!isConnected() /*&& tryConnectOrNot()*/) {
                            cleanupConnection();
                            FileLog.e("XMPPManager", "cleanup Connection and start");
                            start(ConnectionState.ONLINE);
                        }
                        break;
                    case DISCONNECTED:
                        FileLog.e("Test Internet", "DISCONNECTED");
                        stop();
                        break;
                    case RECONNECT_DELAYED:
                        cleanupConnection();
                        start(ConnectionState.RECONNECT_DELAYED);
                        break;
                    case RECONNECT_NETWORK:
                        cleanupConnection();
                        start(ConnectionState.RECONNECT_NETWORK);
                        break;
                    default:
                        FileLog.e("XMPPManager", "xmppRequestStateChange() invalid state to switch to: " + newState.toString());
                }

            }
        });
        // Now we have requested a new state, our state receiver will see when
        // the state actually changes and update everything accordingly.
    }

   /* public SSLSocketFactory getSSLContextRtc (){

        return sslFactory;
    }*/


    public SSLContext getSSLContext() {
        CertificateFactory cf = null;
        SSLContext context = null;
        try {
            cf = CertificateFactory.getInstance("X.509");

            InputStream caInput = new BufferedInputStream(ApplicationLoader.applicationContext.getAssets().open("y.crt"));
            Certificate ca = cf.generateCertificate(caInput);
            String keyStoreType = KeyStore.getDefaultType();
            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);

            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);

            // Create an SSLContext that uses our TrustManager
            //
            context = SSLContext.getInstance("TLS");
            context.init(null, tmf.getTrustManagers(), null);

        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return context;
    }

    public boolean tryConnectOrNot() {
        return connectionState != ConnectionState.RECONNECT_NETWORK && connectionState != ConnectionState.RECONNECT_DELAYED
                && connectionState != ConnectionState.DISCONNECTING && connectionState != ConnectionState.CONNECTING;
    }

    public void initConnection() {

        XMPPTCPConnection mConnection;

        connectionState = ConnectionState.CONNECTING;
        Utilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                NotificationCenter.getInstance().postNotificationName(connectionStateDidChanged);
            }
        });
        // create a new connection if the connection is obsolete or if the
        // old connection is still active
        if (connectionSettingsObsolete || connection == null || connection.isConnected()) {
            lastOutgoingMessageId = 0;
            FileLog.e("yahala", String.format("Initializing connection to server %1$s port %2$d\n", HOST, SPORT));
            SSLContext sc;
            try {
                //f223cd8569d1b33c523c0f7d7110a4c2e17dc909a46a67e4085873699e9338b4
                //f223cd8569d1b33c523c0f7d7110a4c2e17dc909a46a67e4085873699e9338b4
                //F2:23:CD:85:69:D1:B3:3C:52:3C:0F:7D:71:10:A4:C2:E1:7D:C9:09:A4:6A:67:E4:08:58:73:69:9E:93:38:B4
                //6c1b299c7d29ef09d0c7bf0b0248476695026ab563b6d3bd6cd0753545af38f4 new alpha
                sc = JavaPinning.forPin("SHA256:f223cd8569d1b33c523c0f7d7110a4c2e17dc909a46a67e4085873699e9338b4");

               /* CertificateFactory cf = CertificateFactory.getInstance("X.509");
                InputStream caInput = new BufferedInputStream( ApplicationLoader.applicationContext.getAssets().open("y.crt"));
                Certificate ca = cf.generateCertificate(caInput);
                String keyStoreType = KeyStore.getDefaultType();
                KeyStore keyStore = KeyStore.getInstance(keyStoreType);
                keyStore.load(null, null);
                keyStore.setCertificateEntry("ca", ca);

                String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
                tmf.init(keyStore);

                // Create an SSLContext that uses our TrustManager
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(null, tmf.getTrustManagers(), null);*/

                config = XMPPTCPConnectionConfiguration.builder()
                        .setHost(HOST)
                        .setPort(PORT)
                        .setXmppDomain(JidCreate.domainBareFrom(SERVECE_NAME))
                        /* .setServiceName(SERVECE_NAME)*/
                        .setCustomSSLContext(getSSLContext())
                                // .setSocketFactory(new DummySSLSocketFactory())
                        .setResource(RESOURCE)
                                // .setUsernameAndPassword("962797982825", "admin")
                        .setUsernameAndPassword(UserConfig.currentUser.phone, "admin")

                        .setCompressionEnabled(true)
                        .setSecurityMode(ConnectionConfiguration.SecurityMode.required)
                        .setDebuggerEnabled(true)
                        .setSendPresence(false).build()
                ////.setRosterLoadedAtLogin(true)
                ;
                FileLog.e("JidCreate.domainBareFrom(SERVECE_NAME)", JidCreate.domainBareFrom(SERVECE_NAME).toString());

                // TLSUtils.acceptAllCertificates(config.builder());
                // mConnection = new XMPPTCPConnection(config.build());
                /*  try {
                        mConnection.connect();
                        mConnection.login("admin","admin");
                    } catch (XMPPException e) {
                        e.printStackTrace();
                    } catch (SmackException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    return;
                    */

            } catch (KeyManagementException e) {
                e.printStackTrace();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (XmppStringprepException e) {
                e.printStackTrace();
            }


            //config.setReconnectionAllowed(false);


            //SASLAuthentication.registerSASLMechanism(new SASLPlainMechanism());

            /*   cf = CertificateFactory.getInstance("X.509");
                InputStream in = this.mContext.getResources().openRawResource(R.raw.cert);
                Certificate ca;
                ca = cf.generateCertificate(in);
                System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
                in.close();
                URL url = new URL("https://example.com");
                HttpsURLConnection urlConnection =
                        (HttpsURLConnection)url.openConnection();
                in = urlConnection.getInputStream();
                byte[] responsedata = CommonUtil.readInputStream(in);
                Log.w(TAG, "response is "+CommonUtil.convertBytesToHexString(responsedata));
                in.close();
           */

            // config.setSecurityMode(ConnectionConfiguration.SecurityMode.required);




            /*try {
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, MemorizingTrustManager.getInstanceList(ApplicationLoader.applicationContext), new SecureRandom());
                config.setCustomSSLContext(sc);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e);
            } catch (KeyManagementException e) {
                throw new IllegalStateException(e);
            }*/
           /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                config.setTruststoreType("AndroidCAStore");
                config.setTruststorePassword(null);
                config.setTruststorePath(null);
            } else {
                config.setTruststoreType("BKS");
                String path = System.getProperty("javax.net.ssl.trustStore");
                if (path == null)
                    path = System.getProperty("java.home") + File.separator + "etc"
                            + File.separator + "security" + File.separator
                            + "cacerts.bks";
                config.setTruststorePath(path);
            }*/


            //  pm.addExtensionProvider(SentServerReceipt.ELEMENT_NAME, SentServerReceipt.NAMESPACE, new SentServerReceipt.Provider());
            //  pm.addExtensionProvider(ReceivedServerReceipt.ELEMENT_NAME, ReceivedServerReceipt.NAMESPACE, new ReceivedServerReceipt.Provider());
            //  pm.addExtensionProvider(ServerReceiptRequest.ELEMENT_NAME, ServerReceiptRequest.NAMESPACE, new ServerReceiptRequest.Provider());
            //  pm.addExtensionProvider(AckServerReceipt.ELEMENT_NAME, AckServerReceipt.NAMESPACE, new AckServerReceipt.Provider());
            ProviderManager.addExtensionProvider(OutOfBandData.ELEMENT_NAME, OutOfBandData.NAMESPACE, new OutOfBandData.Provider());
            ProviderManager.addExtensionProvider(OutOfBandData.ELEMENT_NAME, PhoneCall.NAMESPACE, new PhoneCall.Provider());
            ProviderManager.addExtensionProvider(UserLocationExtension.ELEMENT_NAME, UserLocationExtension.NAMESPACE, new UserLocationExtension.UserLocationProvider());

            try {
                mConnection = new XMPPTCPConnection(config);
                // mConnection.setUseStreamManagement(false);
                // mConnection.addConnectionCreationListener(createConnectionListener());
                // mConnection.addConnectionListener(createGeneralConnectionListener());//0795019691
                mConnection.setUseStreamManagement(true);

                //Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.accept_all);

                /*packetListener = new com.yahala.xmpp.MessageListener();
                PacketFilter filter = new PacketTypeFilter(org.jivesoftware.smack.packet.Message.class);
                mConnection.addPacketListener(packetListener, filter);
                XmppNotifications.addMessageEventListener(mConnection);*/
                FileLog.e("XMPPManager", "config");
            } catch (Exception e) {
                // connection failure
                FileLog.e("XMPPManager", "Exception creating new XMPP Connection", e);
                maybeStartReconnect();
                return;
            }
            connectionSettingsObsolete = false;
            if (!connectAndAuth(mConnection)) {
                // connection failure
                return;
            }
            sNewConnectionCount++;
        } else {
            // reuse the old connection settings
            mConnection = connection;
            // I reuse the xmpp connection so only connect() is needed
            if (!connectAndAuth(mConnection)) {
                // connection failure
                return;
            }
            sReusedConnectionCount++;
        }

        // this code is only executed if we have an connection established
        onConnectionEstablished(mConnection);

        FileLog.e("XMPPManager", "Connected: " + mConnection.isConnected());

    }

    private void onConnectionEstablished(XMPPTCPConnection mConnection) {
        try {
            connection = mConnection;
            FileLog.e("XMPPManager", "Configuring listeners and retrieving offline messages");

            ////// connection.addConnectionCreationListener(createConnectionListener());
            connection.addConnectionListener(createGeneralConnectionListener());//0795019691

            StanzaFilter filter = new StanzaTypeFilter(Message.class);
            packetListener = new com.yahala.xmpp.MessageListener();

            connection.addAsyncStanzaListener(packetListener, filter);
            connection.addStanzaAcknowledgedListener(new StanzaListener() {
                @Override
                public void processPacket(Stanza packet) throws SmackException.NotConnectedException {
                    FileLog.e("XMPPManager", "AcknowledgedListener:" + packet.getStanzaId());
                }
            });
            /*connection.addPacketListener(this, new AndFilter(
                    new NotFilter(filter)));*/
            Roster.setDefaultSubscriptionMode(Roster.SubscriptionMode.accept_all);
            XmppNotifications.addMessageEventListener(connection);

            //FileLog.e("XMPPManager","ServerContacts size: "+ ContactsController.getInstance().ServerContacts.size());
            //to enforce ui to update user presence
            if (connectionState != ConnectionState.ONLINE) {
                connectionState = ConnectionState.ONLINE;
                Utilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(connectionStateDidChanged);
                        //  Toast.makeText(ApplicationLoader.applicationContext, "yahala is connected", Toast.LENGTH_LONG).show();
                    }
                });
            }
            sendUnsentMessages();

        } catch (Exception e) {
            e.printStackTrace();
        }

        FileLog.e("XMPPManager", "Presence set to" + getStatusName(UserConfig.presence));
        if (UserConfig.presence == -1) {
            if (!foreground)
                setPresence(Away, false);
            else if (foreground) {
                setPresence(Available, false);
            }

        } else {
            setPresence(UserConfig.presence, false);
        }
        mCurrentRetryCount = 0;
        NotificationCenter.getInstance().postNotificationName(userAuthenticated);
        try {
            ContactsController.getInstance().updateFriendsPresence();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Tries to fully establish the given XMPPConnection
     * Calls maybeStartReconnect() or stop() in an error case
     *
     * @param connection
     * @return true if we are connected and authenticated, false otherwise
     */
    private boolean connectAndAuth(XMPPTCPConnection connection) {
        if (UserConfig.currentUser == null)
            UserConfig.loadConfig();
        try {
            FileLog.d("XMPPManager", "Connecting to " + HOST + ":" + SPORT);
            connection.connect();
            registerRoasterListener(connection);
        } catch (Exception e) {
            FileLog.e("XMPPManager", "XMPP connection failed", e);
            if (e instanceof XMPPException) {
                XMPPException xmppEx = (XMPPException) e;
                // StreamError error = xmppEx.getStreamError();
                // // Make sure the error is not null
                //if (error != null) {
                //    FileLog.e("XMPPManager","XMPP connection failed because of stream error: " + error.toString());
                // }
            }
            connectionState = ConnectionState.DISCONNECTED;

            maybeStartReconnect();
            Utilities.RunOnUIThread(new Runnable() {
                @Override
                public void run() {
                    NotificationCenter.getInstance().postNotificationName(connectionStateDidChanged);
                }
            });
            return false;
        }
        // if we reuse the connection and the auth was done with the connect()
        if (connection.isAuthenticated()) {
            return true;
        }

        //add roster listener - must added befor login.
        // registerRoasterListener(connection);


        FileLog.e("XMPPManager", "Service discovery");

        mPingManager = PingManager.getInstanceFor(connection);

        mPingManager.setDefaultPingInterval(30);
        mPingManager.setPingInterval(30);
        mPingManager.registerPingFailedListener(new PingFailedListener() {
            @Override
            public void pingFailed() {
                long now = new Date().getTime();
                if (now - lastPing > 30000) {
                    FileLog.e("XMPPManager", "getInstanceFor reported failed ping, calling maybeStartReconnect()");
                    mCurrentRetryCount = 0;
                    connectionState = ConnectionState.DISCONNECTED;
                    Utilities.RunOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            NotificationCenter.getInstance().postNotificationName(connectionStateDidChanged);
                        }
                    });
                    maybeStartReconnect();
                    lastPing = now;
                } else {
                    FileLog.e("XMPPManager", "Ping failure reported too early. Skipping this occurrence.");
                }
                FileLog.e("XMPPManager", "PingManager reported failed ping");
            }
        });

        try {
            // org.jivesoftware.smackx.muc.
            XHTMLManager.setServiceEnabled(connection, false);
        } catch (Exception e) {
            FileLog.e("XMPPManager", "Failed to set ServiceEnabled flag for XHTMLManager", e);
            // Managing an issue with ServiceDiscoveryManager
            if (e.getMessage() == null) {
                restartConnection();
            }
        }

        try {


            FileLog.e("XMPPManager", "Login with " + UserConfig.currentUser.phone + "@yahala\\" + RESOURCE);
            //UserConfig.loadConfig();
            connection.login();
            // connection.login(UserConfig.currentUser.phone, "admin", RESOURCE);

            //get offline messages


            DeliveryReceiptManager.getInstanceFor(connection).setAutoReceiptMode(DeliveryReceiptManager.AutoReceiptMode.always); //enableAutoReceipts();
            DeliveryReceiptManager.getInstanceFor(connection).addReceiptReceivedListener(new ReceiptReceivedListener() {


                @Override
                public void onReceiptReceived(final Jid fromJid, Jid toJid, final String receiptId, Stanza receipt) {
                    XmppNotifications.updateMessageSendState(receiptId, fromJid.toString(), "", MESSAGE_SEND_STATE_AKN);
                    Utilities.RunOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            // FileLog.e("XMPPManager messageReceivedByAck ", fromJid + " " + receiptId);
                            NotificationCenter.getInstance().postNotificationName(XMPPManager.messageReceivedByAck, fromJid.toString(), receiptId.toString());
                        }
                    });
                }
            });

            //send presence request after login
            sendPresenceRequest(connection);
            //change presence to online

        } catch (Exception e) {
            FileLog.e("XMPPManager login failed", "", e);
            // sadly, smack throws the same generic XMPPException for network
            // related messages (eg "no response from the server") as for
            // authoritative login errors (ie, bad password).  The only
            // differentiation is the message itself which starts with this
            // hard-coded string.
            //  if (e instanceof org.apache.harmony.javax.security.sasl.SaslException) {
            //     // doesn't look like a bad username/password, so retry
            //      maybeStartReconnect();
            //  } else
            if (e instanceof SmackException.AlreadyLoggedInException) {
                FileLog.e("XMPPManager", "Already logged in");
                return true;
            } else if (e instanceof XMPPException.StreamErrorException) {
                FileLog.e("XMPPManager", "XMPP connection failed because of stream error: " + e.getMessage());
                maybeStartReconnect();
            } else {
                stop();
            }
            return false;
        }
        return true;
    }

    /**
     * Register roaster listener
     */
    private void registerRoasterListener(XMPPTCPConnection mConnection) {
        final Roster roster = Roster.getInstanceFor(mConnection);
        roster.setSubscriptionMode(Roster.SubscriptionMode.accept_all);
        rosterListener = new RosterListener() {
            public void presenceChanged(final Presence presence) {

                //final String user = XmppStringUtils.parseBareJid(presence.getFrom().toString());
                final Presence bestPresence;
                try {
                    bestPresence = roster.getPresence(JidCreate.bareFrom(presence.getFrom()));

                    Utilities.RunOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            NotificationCenter.getInstance().postNotificationName(presenceDidChanged, presence.getFrom().toString(), bestPresence, "");
                        }
                    });
                    FileLog.e("Presence changed", "Presence changed: " + presence.getFrom().toString() + " " + bestPresence);
                } catch (XmppStringprepException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void entriesAdded(Collection<Jid> addresses) {

            }

            @Override
            public void entriesUpdated(Collection<Jid> addresses) {

            }

            @Override
            public void entriesDeleted(Collection<Jid> addresses) {

            }


            // Ignored events public void entriesAdded(Collection<String> addresses) {}


        };
        roster.addRosterListener(rosterListener);
    }

    /**
     * calls cleanupConnection and
     * sets _status to DISCONNECTED
     */
    private void stop() {
        FileLog.e("Test Internet", "stop");
        /*connectionState = ConnectionState.DISCONNECTING;
        Utilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                NotificationCenter.getInstance().postNotificationName(connectionStateDidChanged);
                FileLog.e("Test Internet", "RunOnUIThread");
                //  Toast.makeText(ApplicationLoader.applicationContext, "Disconnecting", Toast.LENGTH_LONG).show();
            }
        });*/
        cleanupConnection();
        connectionState = ConnectionState.DISCONNECTED;
        connection = null;
        Utilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                FileLog.e("Test Internet", "RunOnUIThread");
                NotificationCenter.getInstance().postNotificationName(connectionStateDidChanged);
                //Toast.makeText(ApplicationLoader.applicationContext, "Disconnected", Toast.LENGTH_LONG).show();
            }
        });


    }

    public String getLastSeenMessage(String jid) {
        if (!isConnected() || !connection.isAuthenticated()) {
            return LocaleController.getString("Offline", R.string.Offline);
        }
        try {
            LastActivityManager lastActivityManager = LastActivityManager.getInstanceFor(connection);
            LastActivity activity = lastActivityManager.getLastActivity(JidCreate.bareFrom(jid));

            int lastSeenBySeconds = Utilities.parseInt(activity.lastActivity + "");

            String lastSeenMessage = "";
            lastSeenMessage = LocaleController.getString("Offline", R.string.Offline);
            if (lastSeenBySeconds >= 1) {
                PrettyTime p = new PrettyTime();
                Date date = new Date();
                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.SECOND, -1 * lastSeenBySeconds);

                lastSeenMessage = LocaleController.formatDateOnline(cal.getTime()); //p.format(cal.getTime());
            } else {
                lastSeenMessage = LocaleController.getString("Offline", R.string.Offline);
            }
            //FileLog.e("LAST ACTIVITY","" + ""+ "" + lastSeenBySeconds +"  "+jid);
            return lastSeenMessage;

        } catch (Exception e) {
            e.printStackTrace();
            return LocaleController.getString("Offline", R.string.Offline);
        }
        //return "Offline";
    }

    public synchronized void setPresence(final int code, final Boolean save) {

        xmppQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                currentPresence = code;
                FileLog.e("Test", getStatusName(code));
                if (connection == null)
                    return;
                if (save) {
                    if (code == Available) {
                        UserConfig.presence = -1;
                    } else {
                        UserConfig.presence = code;
                    }
                    UserConfig.saveConfig(false);
                    FileLog.e("save", getStatusName(UserConfig.presence));
                }

                Presence presence;
                try {
                    switch (code) {
                        case Available:
                            //XmppOfflineMessages.handleOfflineMessages(connection);
                            presence = new Presence(Presence.Type.available);
                            presence.setStatus(UserConfig.clientUserStatus);

                            connection.sendStanza(presence);
                            // FileLog.e("Test", "State: Available");
                            break;
                        case FreeToChat:
                            // XmppOfflineMessages.handleOfflineMessages(connection);
                            presence = new Presence(Presence.Type.available);
                            presence.setStatus(UserConfig.clientUserStatus);
                            presence.setMode(Presence.Mode.chat);
                            connection.sendStanza(presence);
                            // FileLog.e("Test", "State: Free to chat");
                            break;
                        case DoNotDisturb:
                            //XmppOfflineMessages.handleOfflineMessages(connection);
                            presence = new Presence(Presence.Type.available);
                            presence.setStatus(UserConfig.clientUserStatus);
                            presence.setMode(Presence.Mode.dnd);
                            connection.sendStanza(presence);
                            // FileLog.e("Test", "State: Do not disturb");
                            break;
                        case Away:
                            // XmppOfflineMessages.handleOfflineMessages(connection);
                            presence = new Presence(Presence.Type.available);
                            presence.setStatus(UserConfig.clientUserStatus);
                            presence.setMode(Presence.Mode.away);
                            connection.sendStanza(presence);
                            // FileLog.e("Test", "State: Away");
                            break;
                        case unavailable:
                            Roster roster = Roster.getInstanceFor(connection);
                            Collection<RosterEntry> entries = roster.getEntries();
                            for (RosterEntry entry : entries) {
                                presence = new Presence(Presence.Type.unavailable);
                                //presence.setPacketID(Packet.ID_NOT_AVAILABLE);
                                presence.setFrom(connection.getUser());
                                presence.setTo(entry.getUser());
                                connection.sendStanza(presence);
                                //FileLog.e("Test", "State:" + presence.toXML());
                            }
                            presence = new Presence(Presence.Type.unavailable);
                            presence.setStatus(UserConfig.clientUserStatus);
                            // presence.setPacketID(Packet.ID_NOT_AVAILABLE);
                            presence.setFrom(connection.getUser());
                            presence.setTo(connection.getUser());
                            connection.sendStanza(presence);
                            // FileLog.e("Test", "State: unavailable for pre jid list");
                            break;
                        case 5:
                            presence = new Presence(Presence.Type.unavailable);
                            presence.setStatus(UserConfig.clientUserStatus);
                            connection.sendStanza(presence);
                            // FileLog.e("Test", " unavailable");
                            break;
                        default:
                            break;
                    }
            /*Utilities.RunOnUIThread(new Runnable() {
                @Override
                public void run() {
                    NotificationCenter.getInstance().postNotificationName(currentUserPresenceDidChanged);
                }
            });*/
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });


    }

    public void destroy() {
        // if (connection != null) {
        try {
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        //}
    }

    private long getNextRandomId() {
        long val = 0;
        while (val == 0) {
            val = random.nextLong();
        }
        return val;
    }

    public void createEntry(String user, String name) throws Exception {
        // FileLog.e("Test", String.format("Creating entry for buddy '%1$s' with name %2$s", user, name));
        Roster roster = Roster.getInstanceFor(connection);
        roster.createEntry(JidCreate.bareFrom(user), name, null);
    }

   /* @Override
    public void processPacket(Packet packet) throws SmackException.NotConnectedException {

       // System.out.print("Got packet: " + packet.toXML());

    }*/

    public XMPPConnection getConnection() {
        return this.connection;
    }

    public void openConnectionAsync() {
        xmppQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    connection.connect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public ConnectionCreationListener createConnectionListener() {
        connectionCreationListener = new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection xmppConnection) {
                FileLog.e("Test", "Connection created: Successful!");
            }
        };

        return connectionCreationListener;
    }

    public ConnectionListener createGeneralConnectionListener() {
        connectionGeneralListener = new ConnectionListener() {

            @Override
            public void connected(XMPPConnection xmppConnection) {
                FileLog.e("XMPPManager", "connected");

               /* NotificationCenter.getInstance().postNotificationName(connectionSuccessful);
                Utilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(connectionStateDidChanged);
                        //  Toast.makeText(ApplicationLoader.applicationContext, "Connected", Toast.LENGTH_LONG).show();
                           }
                });
               */
            }

            @Override
            public void authenticated(XMPPConnection connection, boolean resumed) {
                FileLog.e("XMPPManager", "connection authenticated");
                NotificationCenter.getInstance().postNotificationName(userAuthenticated);
            }


            @Override
            public void connectionClosed() {
                FileLog.e("XMPPManager", "ConnectionListener: connectionClosed() called - connection was shutdown by foreign host or by us");
                currentPresence = 5;
                if (connectionState != ConnectionState.CONNECTING && connectionState != ConnectionState.RECONNECT_NETWORK /*&& com.yahala.xmpp.MessagesController.isScreenOn*/) {
                    FileLog.e("XMPPManager", "is connected calling xmppRequestStateChange");

                    xmppQueue.postRunnable(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                connectionState = ConnectionState.CONNECTING;
                                Utilities.RunOnUIThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        NotificationCenter.getInstance().postNotificationName(connectionStateDidChanged);
                                        //  Toast.makeText(ApplicationLoader.applicationContext, "yahala is connected", Toast.LENGTH_LONG).show();
                                    }
                                });
                                connection.connect();
                                XmppOfflineMessages.handleOfflineMessages(connection);
                                FileLog.e("XMPPManager connectionClosed", " connection.connect();");
                                connectionState = ConnectionState.ONLINE;
                                Utilities.RunOnUIThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        NotificationCenter.getInstance().postNotificationName(connectionStateDidChanged);
                                        //  Toast.makeText(ApplicationLoader.applicationContext, "yahala is connected", Toast.LENGTH_LONG).show();
                                    }
                                });
                                if (UserConfig.presence == -1 && !foreground)
                                    setPresence(Away, false);
                                else if (UserConfig.presence == -1 && foreground) {
                                    setPresence(Available, false);
                                } else {
                                    setPresence(UserConfig.presence, false);
                                }

                                // setPresence(UserConfig.presence, false);

                            }/*catch (SmackException.AlreadyLoggedInException e){
                                e.printStackTrace();
                                connectionState = ConnectionState.ONLINE;
                                Utilities.RunOnUIThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        NotificationCenter.getInstance().postNotificationName(connectionStateDidChanged);
                                        //  Toast.makeText(ApplicationLoader.applicationContext, "yahala is connected", Toast.LENGTH_LONG).show();
                                    }
                                });
                            }   */ catch (Exception e) {
                                FileLog.e("XMPPManager connectionClosed", "Exception e: connection.connect(); ");
                                mPingManager = null;
                                e.printStackTrace();
                                connectionState = ConnectionState.DISCONNECTED;

                                XMPPTCPConnection xmppConnection = new XMPPTCPConnection(config);
                                xmppConnection.addConnectionListener(connectionGeneralListener);

                                connection = null;
                                rosterListener = null;
                                mPingManager = null;

                                connectAndAuth(xmppConnection);
                                onConnectionEstablished(xmppConnection);
                            }
                        }
                    });




                  /*connectionState = ConnectionState.DISCONNECTED;
                    Utilities.RunOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            NotificationCenter.getInstance().postNotificationName(connectionStateDidChanged);
                            //Toast.makeText(ApplicationLoader.applicationContext, "connection lost", Toast.LENGTH_LONG).show();
                        }
                    });
                    XMPPConnection xmppConnection = new XMPPTCPConnection(config);
                    xmppConnection.addConnectionListener(connectionGeneralListener);
                    connection = null;
                    rosterListener = null;
                    mPingManager = null;
                    connectAndAuth(xmppConnection);
                    onConnectionEstablished(xmppConnection);*/
                }
                //xmppRequestStateChange(getConnectionStatus());


                //xmppRequestStateChange(ConnectionState.DISCONNECTED);

                // xmppRequestStateChange(getConnectionStatus());
                // Utilities.stageQueue.postRunnable(new Runnable() {
                //    @Override
                //    public void run() {
                //       cleanupConnection();

                //xmppRequestStateChange(getConnectionStatus());
               /* try{

                    connection.connect();}
                catch (Exception e){
                           e.printStackTrace();
                }
                try{
                    Utilities.RunOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ApplicationLoader.applicationContext, "Disconnecting", Toast.LENGTH_LONG).show();
                        }
                    });
                   connection.disconnect();

                }
                catch (Exception e){
                    e.printStackTrace();
                }
               Utilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(ApplicationLoader.applicationContext, "Connection closed", Toast.LENGTH_LONG).show();
                    }
                });*/
                // if(isConnected()){


                // maybeStartReconnect();


                //     }

                //  }
                // });
                //Utilities.stageQueue.postRunnable(new Runnable() {
                //    @Override
                //    public void run() {
                //        maybeStartReconnect();
                //    }
                //},200);


            }

            @Override
            public void connectionClosedOnError(Exception e) {
                // this happens mainly because of on IOException
                // eg. connection timeouts because of lost connectivity
                FileLog.e("XMPPManager", "xmpp disconnected due to error: " + e);
                // We update the state to disconnected (mainly to cleanup listeners etc)
                // then schedule an automatic reconnect.
                connectionState = ConnectionState.DISCONNECTED;
                currentPresence = 5;
                Utilities.RunOnUIThread(new Runnable() {
                    @Override
                    public void run() {
                        NotificationCenter.getInstance().postNotificationName(connectionStateDidChanged);
                        // Toast.makeText(ApplicationLoader.applicationContext, "Connection lost reconnecting", Toast.LENGTH_LONG).show();
                    }
                });
                xmppQueue.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        maybeStartReconnect();
                    }

                }, 500);

            }

            @Override
            public void reconnectingIn(int seconds) {
                throw new IllegalStateException("Reconnection Manager is running");
            }

            @Override
            public void reconnectionSuccessful() {
                FileLog.e("XMPPManager", "Reconnection successful");
                //throw new IllegalStateException("Reconnection Manager is running");
                /*if (connectionState != ConnectionState.ONLINE) {
                    connectionState = ConnectionState.ONLINE;
                    Utilities.RunOnUIThread(new Runnable() {
                        @Override
                        public void run() {
                            NotificationCenter.getInstance().postNotificationName(connectionStateDidChanged);
                            //    Toast.makeText(ApplicationLoader.applicationContext, "Connection lost reconnecting", Toast.LENGTH_LONG).show();
                        }
                    });
                }*/
            }

            @Override
            public void reconnectionFailed(Exception e) {
                throw new IllegalStateException("Reconnection Manager is running");
            }
        };

        return connectionGeneralListener;
    }

    private void restartConnection() {
        cleanupConnection();
        connection = null;

        start(ConnectionState.ONLINE);

    }

    public void maybeStartReconnect() {
        if (!isConnected() /*&& tryConnectOrNot()*/) {


            // a simple linear back off strategy with 5 min max
            // + 100ms to avoid post delayed issue ??

            if (!(!ConnectivityManagerUtil.hasDataConnection(ApplicationLoader.applicationContext) || connectionState == ConnectionState.ONLINE || connectionState == ConnectionState.RECONNECT_NETWORK || connectionState == ConnectionState.CONNECTING)) {
                int timeout = mCurrentRetryCount < 20 ? 5000 * mCurrentRetryCount + 100 : 1000 * 60 * 5;

                FileLog.e("XMPPManager WAITING_TO_CONNcleanupConnection();ECT", "Attempt #" + mCurrentRetryCount + " in " + timeout / 1000 + "s");
                FileLog.e("XMPPManager", "maybeStartReconnect scheduling retry in " + timeout + "ms. Retry #" + mCurrentRetryCount);

                Utilities.stageQueue.handler.removeCallbacks(stageRunnable);
                Utilities.stageQueue.postRunnable(stageRunnable, timeout);
                //FileLog.e("XMPPManager", "maybeStartReconnect fails to post delayed job, reconnecting in 5s.");
                //connectionState = ConnectionState.RECONNECT_DELAYED;
                mCurrentRetryCount++;
            }
        }
    }

    /**
     * Removes all references to the old connection.
     * <p/>
     * Spawns a new disconnect runnable if the connection
     * is still connected and removes packetListeners and
     * Callbacks for the reconnectHandler.
     * <p/>
     * synchronized because cleanupConnection() ->
     * maybeStartReconnect() -> connectionClosedOnError()
     * is called from a different thread
     */
    private synchronized void cleanupConnection() {


        Utilities.stageQueue.handler.removeCallbacks(stageRunnable);
        if (connection != null) {


            // Removing the PacketListener should not be necessary
            // as it's also done by XMPPConnection.disconnect()
            // but it couldn't harm anyway
            FileLog.e("XMPPManager", "Cleanup connection");
            if (packetListener != null) {
                connection.removeAsyncStanzaListener(packetListener);
            }
            /*if (connectionGeneralListener != null) {
                connection.removeConnectionListener(connectionGeneralListener);
                connectionGeneralListener = null;
            }*/
            if (rosterListener != null) {
                Roster roster = Roster.getInstanceFor(connection);
                roster.removeRosterListener(rosterListener);
            }
            if (connection.isConnected()) {
                try {
                    connection.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                connection = null;
                mPingManager = null;

            }
        }

        FileLog.e("XMPPManager", "cleanupConnection end");

    }

    public void createAccount(final String username, final String password, final TLRPC.TL_userSelf userToRegister) {
        xmppQueue.postRunnable(new Runnable() {
            @Override
            public void run() {


                XMPPTCPConnectionConfiguration mConfig = null
                        // // .setRosterLoadedAtLogin(true)
                        ;

                try {
                    mConfig = XMPPTCPConnectionConfiguration.builder()
                            .setHost(HOST)
                            .setPort(PORT)
                            .setXmppDomain(JidCreate.domainBareFrom(SERVECE_NAME))
                            .setCustomSSLContext(getSSLContext())
                            .setResource(RESOURCE)
                            .setCompressionEnabled(true)
                            .setSecurityMode(ConnectionConfiguration.SecurityMode.required)
                            .setDebuggerEnabled(true)
                            .setSendPresence(false).build();
                } catch (XmppStringprepException e) {
                    e.printStackTrace();
                }
                  /*  try {
                        TLSUtils.acceptAllCertificates(mConfig.builder());
                      } catch (NoSuchAlgorithmException e) {
                        e.printStackTrace();
                      } catch (KeyManagementException e) {
                        e.printStackTrace();
                      }
                */
                //connectionState=  ConnectionState.CONNECTING;
                XMPPTCPConnection xmppConnection = new XMPPTCPConnection(mConfig);

                try {
                    xmppConnection.connect();
                } catch (Exception e) {
                    e.printStackTrace();
                    FileLog.e("Yahala create Account", e.toString());

                }
                //onConnectionEstablished(xmppConnection);

                //openConnection();
                FileLog.d("yahala", "Creating account");
                if (xmppConnection.isConnected()) {
                    FileLog.d("yahala", "isConnected=true");

                    AccountManager accountManager = AccountManager.getInstance(xmppConnection);
                    Map<String, String> attributes = new HashMap<String, String>();
                    attributes.put("phone", userToRegister.phone);
                    attributes.put("first", userToRegister.first_name);
                    attributes.put("last", userToRegister.last_name);
                    attributes.put("name", userToRegister.first_name);
                    try {
                        try {
                            accountManager.createAccount(JidCreate.bareFrom(username + "@yahala").getLocalpartOrNull(), password, attributes);
                        } catch (Exception e) {
                            if (e.getMessage().equals("conflict")) {
                                FileLog.e("yahala", " User already exist");
                            }
                        }
                        Utilities.RunOnUIThread(new Runnable() {
                            @Override
                            public void run() {
                                FileLog.e("test", "Account Created");
                                NotificationCenter.getInstance().postNotificationName(XMPPManager.accountCreated);
                            }
                        });
                        FileLog.e("yahala", " XmppManager.accountCreated");
                        FileLog.e("yahala", " currentUser name : " + userToRegister.first_name);

                        UserConfig.clearConfig();
                        //com.yahala.messenger.MessagesStorage.getInstance().cleanUp();
                        //com.yahala.messenger.MessagesController.getInstance().cleanUp();
                        UserConfig.currentUser = userToRegister;
                        UserConfig.clientActivated = true;
                        UserConfig.clientUserJid = userToRegister.jid;
                        UserConfig.clientUserId = userToRegister.id;
                        UserConfig.saveConfig(true);
                        ArrayList<TLRPC.User> users = new ArrayList<TLRPC.User>();
                        users.add(userToRegister);
                        //com.yahala.messenger.MessagesStorage.getInstance().putUsersAndChats(users, null, true, true);

                        MessagesController.getInstance().users.put(userToRegister.jid, userToRegister);
                        //xmppConnection.login(userToRegister.phone, password);

                    } catch (Exception e1) {
                        if (e1.getMessage().contains("conflict(409)")) {
                            NotificationCenter.getInstance().postNotificationName(XMPPManager.accountCreated);
                        }
                        FileLog.e("test", e1.getMessage());
                    }
                    //FileLog.e("test","user authenticated");
                    //setUserVCard(xmppConnection);

                    maybeStartReconnect();
                }
            }
        });


    }

    public boolean addUser(String userName, String name) {
        //AuthenticateUser();
        FileLog.e("yahala", "addUser to roaster " + userName);
        if (connection == null && connection.isConnected())
            return false;
        try {
            Presence subscribe = new Presence(Presence.Type.subscribe);
            subscribe.setTo(userName);
            connection.sendStanza(subscribe);
            Roster roster = Roster.getInstanceFor(connection);
            roster.createEntry(JidCreate.bareFrom(userName), name, null);
            return true;
        } catch (Exception e) {
            FileLog.e("yahala", "addUser to roaster " + e.toString());
            return false;
        }
    }

    public void sendPresenceRequest(XMPPConnection mConnection) {
        Roster roster = Roster.getInstanceFor(mConnection);
        Collection<RosterEntry> entries = roster.getEntries();
        FileLog.e("test", "getFriends entries.size()=" + entries.size());
        try {
            for (RosterEntry e : entries) {
                Presence subscribe = new Presence(Presence.Type.subscribe);
                subscribe.setTo(XmppStringUtils.parseBareAddress(e.getUser()));
                connection.sendStanza(subscribe);
                // FileLog.e("test sendPresenceRequest", "Presence Request sent to " + StringUtils.parseBareAddress(e.getUser()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        FileLog.e("test sendPresenceRequest", "Presence Request sent");
        Utilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                NotificationCenter.getInstance().postNotificationName(presenceRequestSent);
            }
        });
    }

    public ArrayList<TLRPC.User> getFriends(boolean fromCache) {
        ArrayList<TLRPC.User> result = new ArrayList<TLRPC.User>();
        ArrayList<Map<String, Object>> groupDataList = new ArrayList<Map<String, Object>>();
        ArrayList<ArrayList<Map<String, Object>>> childDataList = new ArrayList<ArrayList<Map<String, Object>>>();
        List<RosterGroup> rosterGroupsList = new ArrayList<RosterGroup>();
        Map<RosterGroup, List<RosterEntry>> groupEntriesMap = new HashMap<RosterGroup, List<RosterEntry>>();
        Roster roster = Roster.getInstanceFor(connection);
        Collection<RosterEntry> entries = roster.getEntries();
        //FileLog.e("test", "getFriends entries.size()=" + entries.size());
        if (fromCache) {
            ContactsController.getInstance().performSyncPhoneBook(false);
        } else {
            try {
                for (RosterEntry e : entries) {
                    Presence subscribe = new Presence(Presence.Type.subscribe);
                    subscribe.setTo(XmppStringUtils.parseBareJid(e.getUser()));
                    connection.sendStanza(subscribe);
                    TLRPC.User u = new TLRPC.User();
                    u.first_name = e.getName();
                    u.jid = XmppStringUtils.parseBareJid(e.getUser());
                    u.phone = e.getUser();//StringUtils.parseName(e.getUser());

                    result.add(u);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (UserConfig.contactsHash != String.valueOf(entries.hashCode())) {
                UserConfig.contactsHash = String.valueOf(entries.hashCode());
                UserConfig.saveConfig(false);

            }
        }
        return result;
    }

    public VCard getUserVCard(String jid) {
        //ProviderManager.getInstance().addIQProvider("vCard", "vcard-temp", new VCardProvider());

        VCard vCard = new VCard();

        try {
            vCard.load(XMPPManager.getInstance().getConnection(), JidCreate.entityBareFrom(jid/*+DOMAIN*/));
            VCardProvider vCardProvider = new VCardProvider();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return vCard;
    }

    public void setUserVCard(final XMPPConnection mConnection) {
        xmppQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                XMPPConnection c;
                if (mConnection != null) {
                    c = mConnection;
                } else {
                    c = connection;
                }
                // UserConfig.loadConfig();
                FileLog.e("XMPPManager setUserVCard", UserConfig.currentUser.first_name + " " + UserConfig.currentUser.last_name);

                try {
                    VCard vcard = new VCard();
                    vcard.load(c);
                    //FileLog.e("XMPPManager", "  vcard.save(c);");
                    vcard.setFirstName(UserConfig.currentUser.first_name);
                    vcard.setLastName(UserConfig.currentUser.last_name);

                    vcard.save(c);

                } catch (Exception e) {

                    FileLog.e("XMPPManager", e);

                }
            }
        });

    }

    public void changeImage(final File file) {
        xmppQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                try {
                    VCard vcard = new VCard();
                    vcard.load(connection);

                    byte[] bytes;

                    bytes = getFileBytes(file);
                    if (file == null) {
                        vcard.removeAvatar();
                    } else {
                        vcard.setAvatar(bytes);
                    }
                    // FileLog.e("yahala", "changeImage "+vcard.getFirstName());
                    vcard.save(connection);
                    UserConfig.clientUserPhoto = file.getAbsolutePath();
                    UserConfig.saveConfig(false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public void syncRoasters() {

        //ContactsController.getInstance().performSyncPhoneBook();
    }

    public int IsUserOnLine(String user_jid) {
        String url = "http://" + HOST + ":9090/plugins/presence/status?" +
                "jid=" + user_jid + "@SERVER_NAME &type=xml";
        int shOnLineState = 0; // Not exist
        try {
            URL oUrl = new URL(url);
            URLConnection oConn = oUrl.openConnection();
            if (oConn != null) {
                BufferedReader oIn = new BufferedReader(new InputStreamReader(
                        oConn.getInputStream()));
                if (null != oIn) {
                    String strFlag = oIn.readLine();
                    oIn.close();
                    System.out.println("strFlag" + strFlag);
                    if (strFlag.indexOf("type=\"unavailable\"") >= 0) {
                        shOnLineState = 2;
                    }
                    if (strFlag.indexOf("type=\"error\"") >= 0) {
                        shOnLineState = 0;
                    } else if (strFlag.indexOf("priority") >= 0
                            || strFlag.indexOf("id=\"") >= 0) {
                        shOnLineState = 1;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return shOnLineState;
    }

    public void initPushConnection() {


    }

    public List<HashMap<String, String>> searchUsers(String userName) {

        if (connection == null || !connection.isConnected())
            return null;
        HashMap<String, String> user = null;
        List<HashMap<String, String>> results = new ArrayList<HashMap<String, String>>();
        try {
            UserSearchManager usm = new UserSearchManager(connection);
            Form searchForm = usm.getSearchForm(JidCreate.domainBareFrom("search." + connection.getServiceName()));
            Form answerForm = searchForm.createAnswerForm();
            UserSearch userSearch = new UserSearch();
            answerForm.setAnswer("Username", true);
            answerForm.setAnswer("search", userName);
            ReportedData data = userSearch.sendSearchForm(connection, answerForm, JidCreate.domainBareFrom("search." + connection.getServiceName()));

            for (ReportedData.Row row : data.getRows()) {
                user = new HashMap<String, String>();
                user.put("Username", row.getValues("Username").toString());
                results.add(user);
            }
            FileLog.e("Test", results.size() + "");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }

    public void sendFileOutOFBand(final String jid, final String filePath, final Messages newMsg) {
        /*xmppQueue.postRunnable(new Runnable() {
            @Override
            public void run() {*/
        String root = Environment.getExternalStorageDirectory().toString();
        String path = "/yahala/media/yahala Images/received";


        FileLoader.getInstance().uploadFile("image", filePath, newMsg, jid);
    }

    public void sendRoomId(String jid, Messages newMsg, CallParameter callParameter) {
        /* try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

        ChatMessageListener messageListener = new ChatMessageListener() {
            @Override
            public void processMessage(Chat chat, Message message) {

            }
        };
        ChatManager chatManager = ChatManager.getInstanceFor(connection);
        Chat chat = null;
        try {
            chat = chatManager.createChat(JidCreate.entityBareFrom(jid), messageListener);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }

        // Send the message. The framework requires a MessageListener
        // here, so I add a temporary listener and remove it immediately
        // in the next step.
        chat.removeMessageListener(messageListener);
        Message msg = new Message();
        // String mimeType = URLConnection.guessContentTypeFromName(file.getName());
        msg.addExtension(new PhoneCall(callParameter.mRoomId, callParameter.mVideoCall, callParameter.mCallHangUp));
        msg.setType(Message.Type.chat);
        msg.setPacketID(newMsg.getId() + "");
        msg.setBody("-1");

        // FileLog.e("Test", "msg.getPacketID():" + msg.getPacketID());
        DeliveryReceiptRequest.addTo(msg);


        ////////MessageEventManager.addNotificationsRequests(msg, false, true, true, false);

        try {
            chat.sendMessage(msg);
            //newMsg.setSend_state(MESSAGE_SEND_STATE_SENT);
            // MessagesStorage.getInstance().updateMessage(newMsg);
           /* Utilities.RunOnUIThread(new Runnable() {
                @Override
                public void run() {
                    NotificationCenter.getInstance().postNotificationName(XMPPManager.updateInterfaces);
                }
            });*/
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        chat.close();
    }

    public void sendOutOfBandMessage(String jid, Messages newMsg, String filePath) {
       /* try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/
        File file = new File(filePath);
        ChatMessageListener messageListener = new ChatMessageListener() {

            @Override
            public void processMessage(Chat chat, Message message) {

            }
        };
        ChatManager chatManager = ChatManager.getInstanceFor(connection);
        Chat chat = null;
        try {
            chat = chatManager.createChat(JidCreate.entityBareFrom(jid), messageListener);
        } catch (XmppStringprepException e) {
            e.printStackTrace();
        }

        // Send the message. The framework requires a MessageListener
        // here, so I add a temporary listener and remove it immediately
        // in the next step.
        chat.removeMessageListener(messageListener);
        Message msg = new Message();
        // String mimeType = URLConnection.guessContentTypeFromName(file.getName());
        msg.addExtension(new OutOfBandData("https://188.247.90.132:9092/uploads/" + UserConfig.currentUser.phone + "/" + XmppStringUtils.parseLocalpart(jid) + "/" + file.getName(), "N/A", file.length()));
        msg.setType(Message.Type.chat);
        msg.setPacketID(newMsg.getId() + "");
        msg.setBody("-1");

        // FileLog.e("Test", "msg.getPacketID():" + msg.getPacketID());
        DeliveryReceiptRequest.addTo(msg);


        ////////MessageEventManager.addNotificationsRequests(msg, false, true, true, false);

        try {
            chat.sendMessage(msg);
            newMsg.setSend_state(MESSAGE_SEND_STATE_SENT);
            MessagesStorage.getInstance().updateMessage(newMsg);
            Utilities.RunOnUIThread(new Runnable() {
                @Override
                public void run() {
                    NotificationCenter.getInstance().postNotificationName(XMPPManager.updateInterfaces);
                }
            });
        } catch (SmackException.NotConnectedException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        chat.close();
    }

    private byte[] getFileBytes(File file) throws IOException {
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            int bytes = (int) file.length();
            byte[] buffer = new byte[bytes];
            int readBytes = bis.read(buffer);
            if (readBytes != buffer.length) {
                throw new IOException("Entire file not read");
            }
            return buffer;
        } finally {
            if (bis != null) {
                bis.close();
            }
        }
    }

    long generateMessageId() {
        long messageId = (long) ((((double) System.currentTimeMillis() + ((double) timeDifference) * 1000) * 4294967296.0) / 1000.0);
        if (messageId <= lastOutgoingMessageId) {
            messageId = lastOutgoingMessageId + 1;
        }
        while (messageId % 4 != 0) {
            messageId++;
        }
        lastOutgoingMessageId = messageId;
        return messageId;
    }

    long getTimeFromMsgId(long messageId) {
        return (long) (messageId / 4294967296.0 * 1000);
    }

    public void sendUnsentMessages() {
        MessagesStorage.getInstance().getUnsentMessages();
    }

    public void sendMessage(TLRPC.User user, String jid, int mid) {
        sendMessage(null, 0, 0, null, null, null, null, user, null, null, jid, mid, null);
    }

    public void sendMessage(String jid, int mid, CallParameter call) {
        sendMessage(null, 0, 0, null, null, null, null, null, null, null, jid, mid, call);
    }

    public void sendMessage(MessageObject message, String jid, int mid) {
        sendMessage(null, 0, 0, null, null, message, null, null, null, null, jid, mid, null);
    }

    public void sendMessage(TLRPC.TL_document document, String jid, int mid) {
        sendMessage(null, 0, 0, null, null, null, null, null, document, null, jid, mid, null);
    }

    public void sendMessage(String message, String jid, int mid) {
        sendMessage(message, 0, 0, null, null, null, null, null, null, null, jid, mid, null);
    }

    public void sendMessage(TLRPC.FileLocation location, String jid, int mid) {
        sendMessage(null, 0, 0, null, null, null, location, null, null, null, jid, mid, null);
    }

    public void sendMessage(double lat, double lon, String jid, int mid) {
        sendMessage(null, lat, lon, null, null, null, null, null, null, null, jid, mid, null);
    }

    public void sendMessage(TLRPC.TL_photo photo, String jid, int mid) {
        sendMessage(null, 0, 0, photo, null, null, null, null, null, null, jid, mid, null);
    }

    public void sendMessage(TLRPC.TL_video video, String jid, int mid) {
        sendMessage(null, 0, 0, null, video, null, null, null, null, null, jid, mid, null);
    }

    public void sendMessage(TLRPC.TL_audio audio, String jid, int mid) {
        sendMessage(null, 0, 0, null, null, null, null, null, null, audio, jid, mid, null);
    }

    private void sendMessage(final String message, final double lat, final double lon, final TLRPC.TL_photo photo,
                             final TLRPC.Video video, final MessageObject msgObjfinal, TLRPC.FileLocation location,
                             final TLRPC.User user, final TLRPC.TL_document document, final TLRPC.TL_audio audio, final String jid, final int mid,
                             CallParameter call) {


        try {
            FileLog.e("Test sendMessage", "sendMessage");
            int type = -1;
            final Messages newMsg = new Messages();

            if (message != null) {//if not null the message type is text
                newMsg.tl_message = new TLRPC.TL_message();
                newMsg.tl_message.message = message;
                //newMsg.media = new TLRPC.TL_messageMediaEmpty();
                type = 0;
                newMsg.tl_message.media = new TLRPC.TL_messageMediaEmpty();
                newMsg.setMessage(message);
            } else if (call != null) {
                //if not null the message type is text
                newMsg.tl_message = new TLRPC.TL_message();
                newMsg.tl_message.message = message;
                //newMsg.media = new TLRPC.TL_messageMediaEmpty();
                type = 40;
                newMsg.tl_message.media = new TLRPC.TL_messageMediaEmpty();
                newMsg.setMessage("-1");
            } else if (photo != null) {
                type = 2;
                newMsg.tl_message = new TLRPC.TL_message();
                FileLog.e("Test sendMessage", "sendMessage");


                newMsg.setMessage("-1");
                newMsg.tl_message.message = "-1";
                TLRPC.FileLocation location1 = photo.sizes.get(photo.sizes.size() - 1).location;
                newMsg.tl_message.attachPath = OSUtilities.getCacheDir() + "/" + location1.volume_id + "_" + location1.local_id + ".jpg";

                TLRPC.TL_messageMediaPhoto tl_messageMediaPhoto = new TLRPC.TL_messageMediaPhoto();
                tl_messageMediaPhoto.photo = photo;
                tl_messageMediaPhoto.progress = 1;
                newMsg.tl_message.media = tl_messageMediaPhoto;
                FileLog.e("yahala tl_messageMediaPhoto", photo.sizes.size() + "");
            } else if (video != null) {
                newMsg.tl_message = new TLRPC.TL_message();
                newMsg.tl_message.media = new TLRPC.TL_messageMediaVideo();
                newMsg.tl_message.media.video = video;
                type = 6;
                newMsg.tl_message.message = "-1";
                newMsg.tl_message.attachPath = video.path;
                newMsg.setMessage("-1");
            } else if (document != null) {
                newMsg.tl_message = new TLRPC.TL_message();
                newMsg.tl_message.media = new TLRPC.TL_messageMediaDocument();
                newMsg.tl_message.media.document = document;
                type = 16;
                newMsg.tl_message.message = "-1";
                newMsg.setMessage("-1");
                newMsg.tl_message.attachPath = document.path;
            } else if (audio != null) {
                type = 18;
                FileLog.e("Test", "audio != null");

                newMsg.tl_message = new TLRPC.TL_message();
                newMsg.tl_message.attachPath = audio.path;

                FileLog.e("yahala audio.path", audio.path);
                TLRPC.TL_messageMediaAudio tl_messageMediaAudio = new TLRPC.TL_messageMediaAudio();

                tl_messageMediaAudio.progress = 1;
                tl_messageMediaAudio.audio = audio;
                FileLog.e("yahala tl_message.media.audio", audio.duration + "");

                newMsg.tl_message.media = tl_messageMediaAudio;
                newMsg.tl_message.message = "-1";
                newMsg.setMessage("-1");

            } else if (lat != 0 && lon != 0) {
                newMsg.tl_message = new TLRPC.TL_message();
                newMsg.tl_message.media = new TLRPC.TL_messageMediaGeo();
                newMsg.tl_message.media.geo = new TLRPC.TL_geoPoint();
                newMsg.tl_message.media.geo.lat = lat;
                newMsg.tl_message.media.geo._long = lon;
                newMsg.tl_message.message = "";
                newMsg.setMessage("");
                type = 4;
            }

                    /*if (newMsg == null) {
                        FileLog.e("yahala newMsg == null", "newMsg == null");
                        return;
                    }*/
            newMsg.tl_message.from_id = jid;

            // newMsg.tl_message.date=0;

            //create the message obj and get its local id
            newMsg.setId((long) mid);
            newMsg.id = mid;
            newMsg.setJid(jid);

            newMsg.setRead_state(0);
            newMsg.tl_message.out = true;
            newMsg.setOut(1);
            newMsg.setType(type);
            newMsg.setDate(new Date());
            newMsg.setSend_state(XMPPManager.MESSAGE_SEND_STATE_SENDING);

            FileLog.e("yahala newMsg == null", "putMessage newMsg");
            //  newMsg.setSend_state(XmppManager.MESSAGE_SEND_STATE_SENDING);
            if (newMsg.getId() != 0L || newMsg.getType() != 40)
                MessagesStorage.getInstance().putMessage(newMsg);
            NotificationCenter.getInstance().postNotificationName(XMPPManager.didReceivedNewMessages, newMsg, newMsg.getJid());
            Utilities.RunOnUIThread(new Runnable() {
                @Override
                public void run() {
                    NotificationCenter.getInstance().postNotificationName(XMPPManager.updateInterfaces);
                }
            });

            UserConfig.saveConfig(false);//called to save the last message id - false to not update the current user

            if (type == 0 || type == 4) {
                FileLog.e("Test", String.format("\nSending message '%1$s' to user %2$s", message, jid));
                sendTextMessage(newMsg);
            } else if (type == 2 || type == 3 || type == 6 || type == 18 || type == 16) {
                        /*Message msg = new Message();
                        msg.setType(Message.Type.chat);
                        msg.setPacketID(newMsg.getId() + "");
                        msg.setBody(message);
                        FileLog.e("Test ", "type==2 msg.getPacketID():" + msg.getPacketID());
                         DeliveryReceiptManager.addDeliveryReceiptRequest(msg);
                        MessageEventManager.addNotificationsRequests(msg, true, true, true, true);*/
                FileLog.e("yahala newMsg == null", "sending file");


                sendFileOutOFBand(jid, newMsg.tl_message.attachPath, newMsg);

            } else if (type == 40) {
                sendRoomId(jid, newMsg, call);
            }

        } catch (Exception e) {
            Utilities.RunOnUIThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ApplicationLoader.applicationContext, "Sorry, something happened and we couldn't send your message", Toast.LENGTH_SHORT);
                }
            });
        }


    }

    public void sendTextMessage(final Messages messages) {
        // MessageObject obj =new MessageObject(messages,null);

        Message msg = new Message();
        msg.setType(Message.Type.chat);
        msg.setStanzaId(messages.getId() + "");
        msg.setBody(messages.getMessage());
        FileLog.e("Test", "msg.getStanzaID():" + msg.getStanzaId());

        if (messages.getId() != 0L)//service and notification messages
        {////////MessageEventManager.addNotificationsRequests(msg, false, true, true, false);

            DeliveryReceiptRequest.addTo(msg);
        }

        ChatMessageListener messageListener = new ChatMessageListener() {

            @Override
            public void processMessage(Chat chat, Message message) {

            }
        };
        ChatManager chatManager = ChatManager.getInstanceFor(connection);
        Chat chat = null;
        try {
            chat = chatManager.createChat(JidCreate.entityBareFrom(messages.getJid()), messageListener);
            chat.removeMessageListener(messageListener);
        } catch (Exception e) {
            e.printStackTrace();
        }


        if (messages.getType() == 4) {
            UserLocationExtension userLocationExtension = new UserLocationExtension(new Location(messages.tl_message.media.geo._long, messages.tl_message.media.geo.lat));
            msg.addExtension(userLocationExtension);
        }


        try {
            chat.sendMessage(msg);
            messages.setSend_state(XMPPManager.MESSAGE_SEND_STATE_SENT);
            NotificationCenter.getInstance().postNotificationName(XMPPManager.messageReceivedByServer, messages.getJid(), messages.getId());
        } catch (Exception e) {
            e.printStackTrace();
            messages.setSend_state(XMPPManager.MESSAGE_SEND_STATE_SENDING);
            Utilities.RunOnUIThread(new Runnable() {
                @Override
                public void run() {
                    NotificationCenter.getInstance().postNotificationName(XMPPManager.messageSendError, messages.getJid(), messages.getId());

                }
            });

            Utilities.RunOnUIThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ApplicationLoader.applicationContext, "Sorry, something happened and we couldn't send your message", Toast.LENGTH_SHORT);
                }
            });
        }

        try {
            if (messages.getId() != 0L)//service and notification messages

                MessagesStorage.getInstance().updateMessage(messages);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            chat.close();
        }
        Utilities.RunOnUIThread(new Runnable() {
            @Override
            public void run() {
                NotificationCenter.getInstance().postNotificationName(XMPPManager.updateInterfaces);
            }
        });
    }

    public void sendOfflineMessage(Messages messages) {
        if (isConnected()) {
            messages = MessagesStorage.getInstance().getMedia(messages);
            // Send the message. The framework requires a MessageListener
            // here, so I add a temporary listener and remove it immediately
            // in the next step.

            messages.setSend_state(XMPPManager.MESSAGE_SEND_STATE_SENDING);
            if (messages.getType() == 0) {
                sendTextMessage(messages);
            } else if (messages.getType() == 2 || messages.getType() == 4 || messages.getType() == 6) {
                messages.setSend_state(XMPPManager.MESSAGE_SEND_STATE_SENT);
                sendFileOutOFBand(messages.getJid(), messages.tl_message.attachPath, messages);
                messages.setSend_state(XMPPManager.MESSAGE_SEND_STATE_AKN);
            }
            if (messages.getId() != 0L) {
                MessagesStorage.getInstance().updateMessage(messages);

            }
        }
    }

    private void sendMessage() {
      /*  TLRPC.Message newMsg = null;
        int type = -1;
        if (message != null) {
            newMsg = new TLRPC.TL_message();
            newMsg.media = new TLRPC.TL_messageMediaEmpty();
            type = 0;
            newMsg.message = message;
        }*/

    }

    public void removeNotification() {
        NotificationManager mNotificationManager =
                (NotificationManager) ApplicationLoader.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(0);
    }

    //  public void postNotification(String title, String notification) {

       /* Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmSound == null) {
                alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }
        }*/
      /*  long[] vibrate = {0, 100, 200, 300};

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(ApplicationLoader.applicationContext)
                .setSmallIcon(R.drawable.ic_ab_logo) // notification icon
                .setContentTitle(title) // title for notification
                .setContentText(notification) // message for notification
                .setAutoCancel(true)// clear notification after click
                        // .setSound(alarmSound)
                .setSound(Uri.parse("android.resource://com.yahala.app/" + R.raw.electronic))
                .setVibrate(vibrate);
        Intent intent = new Intent(ApplicationLoader.applicationContext, LaunchActivity.class);
        PendingIntent pi = PendingIntent.getActivity(ApplicationLoader.applicationContext, 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);
        mBuilder.setContentIntent(pi);
        NotificationManager mNotificationManager =
                (NotificationManager) ApplicationLoader.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(0, mBuilder.build());*/
    // }

    public void applicationMovedToForeground() {
        Utilities.stageQueue.postRunnable(new Runnable() {
            @Override
            public void run() {
                if (paused) {
                    nextSleepTimeout = 60000;
                    nextWakeUpTimeout = 60000;
                    FileLog.e("yahala", "reset timers by application moved to foreground");
                }
            }
        });
    }

    public void weakup() {
        Timer serviceTimer = new Timer();
        serviceTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Utilities.stageQueue.postRunnable(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }
        }, 1000, 5000);


    }


    public String getStatusName(int presence) {

        //FileLog.e("Test updatePresenceMenuIcon","!Null");
        if (presence == XMPPManager.Available) {
            return LocaleController.getString("Online", R.string.Online);
        } else if (presence == XMPPManager.Away) {
            return LocaleController.getString("Away", R.string.Away);
        } else if (presence == 5) {
            return LocaleController.getString("Offline", R.string.Offline);
        } else if (presence == XMPPManager.DoNotDisturb) {
            return LocaleController.getString("DoNotDisturb", R.string.DoNotDisturb);
        } else if (presence == XMPPManager.FreeToChat) {
            return LocaleController.getString("FreeToChat", R.string.FreeToChat);
        } else {
            return "Invalid";
        }
    }

    public static class User {
        public int id;
        public String first_name;
        public String last_name;
        public long access_hash;
        public String phone;
        // public UserProfilePhoto photo;
        // public UserStatus status;
        public boolean inactive;
    }

    public class PresenceTypeFilter implements PacketFilter {
        private final Presence.Type type;

        public PresenceTypeFilter(Presence.Type type) {
            this.type = type;
        }


        @Override
        public boolean accept(Stanza stanza) {
            if (!(stanza instanceof Presence)) {
                return false;
            } else {
                return ((Presence) stanza).getType().equals(this.type);
            }
        }
    }

    public void cancelSendingMessage(MessageObject object) {
        String keyToRemove = object.messageOwner.tl_message.attachPath;
        boolean enc = false;

        FileLoader.getInstance().cancelUploadFile(keyToRemove, enc);
        ArrayList<Integer> messages = new ArrayList<Integer>();
        messages.add(object.messageOwner.id);
        MessagesStorage.getInstance().deleteMessages(messages);
    }
}